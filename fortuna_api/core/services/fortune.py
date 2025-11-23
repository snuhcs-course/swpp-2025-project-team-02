"""
Fortune-telling service using Korean Saju and OpenAI API.
Generates personalized daily fortunes based on Saju compatibility and user data.
"""

import os
import base64
from datetime import datetime, timedelta
from typing import Dict, Any, Generic, List, Literal, Optional, TypeVar
from core.services.daewoon import DaewoonCalculator
from pydantic import BaseModel, Field
import openai
from django.conf import settings
from django.core.files.base import ContentFile
from user.models import User
from ..utils.saju_concepts import (
    Saju,
    GanJi,
    FiveElements,
    TwelveBranches
)
from .image import ImageService
from core.models import FortuneResult
from loguru import logger
import numpy as np

# TODO - move to global utils

T = TypeVar("T", bound=BaseModel)

class ErrorInfo(BaseModel):
    code: str
    message: str

class Response(BaseModel, Generic[T]):
    status: Literal["success", "error"]
    data: Optional[T] = None
    error: Optional[ErrorInfo] = None

class FortuneAIResponse(BaseModel):
    """사용자의 사주와 일진을 종합적으로 분석한 하루 운세 응답 구조."""
    today_fortune_summary: str = Field(
        description="오늘 운세를 한 줄로 요약한 캐치프레이즈. 필요한 오행 원소를 반드시 포함하여 긍정적이고 친근한 톤으로 작성. (예: '오늘은 재물운이 좋은 날! 오늘 토의 기운을 모아 기회를 놓치지 마세요!', '차분한 마음으로 목의 기운을 채워보세요'). 30-40자 내외."
    )
    today_element_balance_description: str = Field(
        description="오늘 하루 운기의 오행 분포와 사용자의 사주 오행 분포를 기반으로 오늘 오행 분포에 대한 설명. 설명에는, 사용자에게 필요한 오행과 그 이유가 포함되어야힙니다. 알아듣기 쉽게 친절하게 4-5 문장으로 작성."
    )
    today_daily_guidance: str = Field(
        description="부족한 오행 요소를 보강할 수 있는 일상 속 행동들을 today_element_balance_description을 기반으로 설명. 알아듣기 쉽게 4-5문장으로 작성."
    )

class TomorrowGapja(BaseModel):
    code: int = Field(description="Gapja code")
    name: str = Field(description="Gapja name")
    stem: str = Field(description="Gapja stem")
    branch: str = Field(description="Gapja branch")
    element: str = Field(description="Gapja element")
    element_color: str = Field(description="Gapja element color")
    animal: str = Field(description="Gapja animal")


class ElementDistribution(BaseModel):
    """Distribution of a specific element in the fortune."""
    count: int = Field(description="Number of occurrences")
    percentage: float = Field(description="Percentage of total elements")


class FortuneScore(BaseModel):
    """Fortune score with entropy-based five elements balance."""
    entropy_score: float = Field(description="Balance score from 0-100 based on entropy")
    elements: Dict[str, Optional[Dict[str, Any]]] = Field(
        description="8 pillars: 대운, 세운, 월운, 일운, 년주, 월주, 일주, 시주 (full GanJi dicts)"
    )
    element_distribution: Dict[str, ElementDistribution] = Field(
        description="Distribution of 5 elements: 목, 화, 토, 금, 수"
    )
    interpretation: str = Field(description="Human-readable interpretation of balance score")
    needed_element: str = Field(description="Needed element (목/화/토/금/수) to harmonize user's energy with today's energy")

class FortuneResponse(BaseModel):
    """Response model for today's fortune endpoint."""
    model_config = {'arbitrary_types_allowed': True}

    date: str = Field(description="Date for which fortune was generated")
    user_id: int = Field(description="User ID")
    fortune: FortuneAIResponse = Field(description="Fortune AI response")
    fortune_score: FortuneScore = Field(description="Fortune score")
    saju_date: Any = Field(description="Saju calculated from date (Saju object)")
    saju_user: Any = Field(description="User's birth Saju (Saju object)")
    daewoon: Any = Field(description="Current Daewoon (GanJi object, may be None)")

class FortuneResponseDeprecated(BaseModel):
    fortune_id: int = Field(description="Fortune ID")
    user_id: int = Field(description="User ID")
    generated_at: str = Field(description="Generation time")
    for_date: str = Field(description="Date for which fortune was generated")
    tomorrow_gapja: TomorrowGapja = Field(description="Tomorrow's gapja")
    fortune: FortuneAIResponse = Field(description="Fortune AI response")
    fortune_score: FortuneScore = Field(description="Fortune score")

class FortuneService:
    """Service for generating Saju-based fortune tellings."""

    # Element to character file mapping
    ELEMENT_TO_CHARACTER_FILE = {
        FiveElements.WOOD: "wood.png",
        FiveElements.FIRE: "fire.png",
        FiveElements.EARTH: "earth.png",
        FiveElements.METAL: "metal.png",
        FiveElements.WATER: "water.png"
    }

    # Cache for uploaded character file IDs (class variable)
    _character_file_cache: Dict[str, str] = {}

    def __init__(self, image_service: ImageService | None = None):
        """Initialize FortuneService with OpenAI client."""
        api_key = settings.OPENAI_API_KEY if hasattr(settings, 'OPENAI_API_KEY') else os.getenv('OPENAI_API_KEY')
        if api_key:
            self.client = openai.OpenAI(api_key=api_key)
        else:
            self.client = None
        # logger.info(f"FortuneService initialized with OpenAI client: {api_key}")
        self.image_service = image_service if image_service else ImageService()

    ### private methods ###

    def _get_character_file_path(self, element: FiveElements) -> str:
        """
        Get the absolute file path for a character image based on element.

        Args:
            element: FiveElements enum value

        Returns:
            Absolute path to the character image file
        """
        filename = self.ELEMENT_TO_CHARACTER_FILE[element]
        # core/static/characters/ directory
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        character_path = os.path.join(base_dir, 'static', 'characters', filename)

        return character_path

    def _upload_character_file(self, element: FiveElements) -> str:
        """
        Upload character image to OpenAI Files API with caching.

        Args:
            element: FiveElements enum value

        Returns:
            OpenAI file ID for the uploaded character image

        Raises:
            ValueError: If client not initialized or file not found
        """
        if not self.client:
            raise ValueError("OpenAI client not initialized")

        # Check cache first
        element_key = element.chinese
        if element_key in self._character_file_cache:
            logger.info(f"Using cached character file ID for {element_key}")
            return self._character_file_cache[element_key]

        # Get file path
        character_path = self._get_character_file_path(element)

        # Check if file exists
        if not os.path.exists(character_path):
            logger.warning(f"Character file not found: {character_path}")
            raise ValueError(f"Character image file not found for element {element.chinese}")

        # Upload to OpenAI Files API
        try:
            with open(character_path, "rb") as file_content:
                result = self.client.files.create(
                    file=file_content,
                    purpose="vision"
                )
                file_id = result.id

                # Cache the file ID
                self._character_file_cache[element_key] = file_id
                logger.info(f"Uploaded character file for {element.chinese}: {file_id}")

                return file_id

        except Exception as e:
            logger.error(f"Failed to upload character file for {element.chinese}: {e}")
            raise

    def calculate_day_ganji(self, date_value: datetime) -> GanJi:
        """
        Calculate day pillar (일주) for a given date using proper Saju calculation.

        Args:
            date_value: The date to calculate day pillar for

        Returns:
            GanJi object representing the day pillar
        """
        # Convert datetime to date if necessary
        if isinstance(date_value, datetime):
            date_value = date_value.date()

        return Saju._calculate_day_pillar(date_value)

    def get_user_saju_info(self, user_id: int) -> Saju:
        """
        Get user's Saju information from database.

        Args:
            user_id: User ID

        Returns:
            Saju object containing user's four pillars

        Raises:
            ValueError: If user not found or saju data incomplete
        """
        from user.models import User

        try:
            user = User.objects.get(id=user_id)

            # Validate that user has complete saju data
            if not all([user.yearly_ganji, user.monthly_ganji, user.daily_ganji, user.hourly_ganji]):
                raise ValueError(f"User {user_id} has incomplete saju data")

            # Build Saju object from user's ganji data using user.saju() method
            return user.saju()

        except User.DoesNotExist:
            raise ValueError(f"User {user_id} not found")

    def analyze_saju_compatibility(
        self,
        user_day_ganji: GanJi,
        tomorrow_day_ganji: GanJi
    ) -> Dict[str, Any]:
        """
        Analyze compatibility between user's day pillar and tomorrow's day pillar.

        Uses Five Elements (오행) theory to determine compatibility:
        - 상생 (empowers): One element empowers another (score bonus)
        - 상극 (weakens): One element weakens another (score penalty)
        - Same element: Neutral compatibility

        Args:
            user_day_ganji: User's day pillar (GanJi)
            tomorrow_day_ganji: Tomorrow's day pillar (GanJi)

        Returns:
            Compatibility analysis with score, level, message, and element details
        """
        # Get elements from stems (천간의 오행이 주요 에너지)
        user_element = user_day_ganji.stem.element
        tomorrow_element = tomorrow_day_ganji.stem.element

        # Calculate base compatibility score (50 = neutral)
        compatibility_score = 50

        # Analyze element relationship
        element_relation = "중립"
        relation_detail = ""

        if user_element.empowers(tomorrow_element):
            # 상생: User's element empowers tomorrow's element
            compatibility_score += 25
            element_relation = "상생 (相生)"
            relation_detail = f"{user_element.chinese}이(가) {tomorrow_element.chinese}을(를) 도와줍니다"
        elif user_element.weakens(tomorrow_element):
            # 상극: User's element weakens tomorrow's element
            compatibility_score -= 15
            element_relation = "상극 (相剋)"
            relation_detail = f"{user_element.chinese}이(가) {tomorrow_element.chinese}을(를) 극합니다"
        elif tomorrow_element.empowers(user_element):
            # 역상생: Tomorrow's element empowers user's element
            compatibility_score += 20
            element_relation = "수혜 (受惠)"
            relation_detail = f"{tomorrow_element.chinese}이(가) {user_element.chinese}을(를) 도와줍니다"
        elif tomorrow_element.weakens(user_element):
            # 역상극: Tomorrow's element weakens user's element
            compatibility_score -= 20
            element_relation = "피극 (被剋)"
            relation_detail = f"{tomorrow_element.chinese}이(가) {user_element.chinese}을(를) 극합니다"
        elif user_element == tomorrow_element:
            # Same element: Neutral but stable
            compatibility_score += 5
            element_relation = "동행 (同行)"
            relation_detail = f"같은 {user_element.chinese}행의 안정된 기운"

        # Branch compatibility bonus (지지 조합)
        if self._check_beneficial_branch_combination(user_day_ganji.branch, tomorrow_day_ganji.branch):
            compatibility_score += 10

        # Ensure score is within 0-100
        compatibility_score = max(0, min(100, compatibility_score))

        # Determine compatibility level and message
        if compatibility_score >= 80:
            compatibility_level = "매우 좋음"
            message = "당신의 사주와 내일의 기운이 완벽한 조화를 이룹니다."
        elif compatibility_score >= 60:
            compatibility_level = "좋음"
            message = "긍정적인 에너지가 당신을 도울 것입니다."
        elif compatibility_score >= 40:
            compatibility_level = "보통"
            message = "평온한 하루가 될 것입니다."
        else:
            compatibility_level = "주의"
            message = "신중한 판단이 필요한 날입니다."

        return {
            "score": compatibility_score,
            "level": compatibility_level,
            "message": message,
            "element_relation": element_relation,
            "relation_detail": relation_detail,
            "user_element": user_element.chinese,
            "user_element_color": user_element.color,
            "tomorrow_element": tomorrow_element.chinese,
            "tomorrow_element_color": tomorrow_element.color,
            "user_ganji": user_day_ganji.two_letters,
            "tomorrow_ganji": tomorrow_day_ganji.two_letters
        }

    def _check_beneficial_branch_combination(
        self,
        user_branch: TwelveBranches,
        tomorrow_branch: TwelveBranches
    ) -> bool:
        """
        Check if branch combination is beneficial (삼합, 육합 등).

        Args:
            user_branch: User's branch
            tomorrow_branch: Tomorrow's branch

        Returns:
            True if combination is beneficial
        """
        # 육합 (Six Harmonies): 지지끼리의 조화
        liu_he_pairs = [
            (TwelveBranches.JA, TwelveBranches.CHUK),   # 자축합
            (TwelveBranches.IN, TwelveBranches.HAE),    # 인해합
            (TwelveBranches.MYO, TwelveBranches.SUL),   # 묘술합
            (TwelveBranches.JIN, TwelveBranches.YU),    # 진유합
            (TwelveBranches.SA, TwelveBranches.SIN),    # 사신합
            (TwelveBranches.O, TwelveBranches.MI),      # 오미합
        ]

        for branch_a, branch_b in liu_he_pairs:
            if (user_branch == branch_a and tomorrow_branch == branch_b) or \
               (user_branch == branch_b and tomorrow_branch == branch_a):
                return True

        return False

    def prepare_photo_context(
        self,
        user_id: int,
        date: datetime
    ) -> List[Dict[str, Any]]:
        """
        Prepare photo and location context for fortune telling.

        Args:
            user_id: User ID
            date: Date to get photos for

        Returns:
            List of photo contexts with metadata
        """
        photos = self.image_service.get_user_images_for_date(user_id, date)

        photo_contexts = []
        for photo in photos:
            # Extract filename from URL or use photo ID
            filename = photo["url"].split('/')[-1] if photo.get("url") else f"image_{photo['id']}.jpg"

            photo_context = {
                "filename": filename,
                "url": photo["url"],
                "metadata": {
                    "timestamp": photo.get("timestamp", date.isoformat()),
                    "location": photo.get("location") or {
                        "latitude": 37.5665,  # Mock Seoul coordinates
                        "longitude": 126.9780
                    }
                }
            }
            photo_contexts.append(photo_context)

        # If no photos, add placeholder context
        if not photo_contexts:
            photo_contexts.append({
                "filename": "no_photo",
                "metadata": {
                    "timestamp": date.isoformat(),
                    "location": None
                }
            })

        return photo_contexts

    def generate_fortune_with_ai(
        self,
        user_saju: Saju,
        tomorrow_date: datetime,
        tomorrow_day_ganji: GanJi,
        compatibility: Dict[str, Any],
        fortune_score: FortuneScore
    ) -> FortuneAIResponse:
        """
        Generate fortune using OpenAI API with structured output.

        Args:
            user_saju: User's Saju object (four pillars)
            tomorrow_date: Tomorrow's date
            tomorrow_day_ganji: Tomorrow's day pillar (GanJi)
            compatibility: Compatibility analysis
            photo_contexts: Photo and location contexts

        Returns:
            Structured fortune response
        """
        # Extract element information for context
        user_day_element = user_saju.daily.stem.element
        tomorrow_day_element = tomorrow_day_ganji.stem.element

        # Prepare context for AI
        context = f"""
        # Role & Persona
        당신은 GenZ를 위한 사주 기반 라이프 가이드 서비스 '포르투나(Fortuna)'의 명리학 분석가입니다.
        사용자의 사주와 오늘의 일진을 명리학적 원리에 따라 분석하고, 오행의 균형을 통해 운을 개선하는 '개운(開運)' 조언을 제공합니다.

        # Instructions
        1. **쉬운 언어 사용:** GenZ 사용자를 위해 겁재, 식신, 편관 등 어려운 십신 용어나 전문 한자어는 사용하지 마세요.
           오행(목화토금수)의 관계를 쉽게 풀어서 설명해주세요.

        2. **3개의 필드 출력:**
           - today_fortune_summary: 오늘 운세를 한 줄로 요약한 캐치프레이즈.
             * **십신 관계를 활용하여 구체적인 운세 영역(재물, 학업, 연애 등)을 언급**해주세요.
             * **중요: '{fortune_score.needed_element}'는 부족한 원소이므로, "~의 기운을 모아" 또는 "~의 기운을 채워" 형태로 표현**해주세요.
             * "~의 기운으로" 같은 표현은 사용하지 마세요 (이미 많은 것처럼 들림).
             * 30-40자 내외.
             * 예시:
               - "오늘은 재물운이 좋은 날! '{fortune_score.needed_element}'의 기운을 모아 기회를 놓치지 마세요!"
               - "학업에 집중하기 좋은 날, '{fortune_score.needed_element}'의 기운을 채워 균형을 맞춰보세요!"
               - "연애운이 상승하는 날! '{fortune_score.needed_element}'의 기운을 모아 행운을 잡아보세요!"
           - today_element_balance_description: 오늘 하루 운기의 오행 분포와 사용자 사주의 오행 분포를 비교 분석. 사용자에게 **부족한** 오행이 왜 <{fortune_score.needed_element}>행인지, 알기 쉽게 설명해주세요.  (4-5문장)
           - today_daily_guidance: 부족한 오행 '{fortune_score.needed_element}'를 보충할 수 있는 실용적인 일상 행동 조언 (4-5문장)

        ---
        # 사주 & 오행 이론 요약
        1. 천간‧지지의 구조: 오행과 음양
            천간(天干) — 10개
                글자	오행	음양
                갑	목	양(+)
                을	목	음(-)
                병	화	양(+)
                정	화	음(-)
                무	토	양(+)
                기	토	음(-)
                경	금	양(+)
                신	금	음(-)
                임	수	양(+)
                계	수	음(-)
            지지(地支) — 12개
                글자	오행	음양
                자	수	양(+)
                축	토	음(-)
                인	목	양(+)
                묘	목	음(-)
                진	토	양(+)
                사	화	음(-)
                오	화	양(+)
                미	토	음(-)
                신	금	양(+)
                유	금	음(-)
                술	토	양(+)
                해	수	음(-)

        2. **오행의 핵심 원리를 반영:**
           - 상생(相生): 목→화→토→금→수→목 (서로 돕는 관계)
           - 상극(相剋): 목극토, 토극수, 수극화, 화극금, 금극목 (서로 견제하는 관계)

        3. **십신(十神) 관계와 운세 영역 (일간 기준):**
           일간(日干)을 기준으로 다른 오행과의 관계를 십신이라고 하며, 각 십신은 특정 인생 영역을 상징합니다.

           | 십신 | 일간과의 관계 | 대표 운세 영역 | 긍정적 해석 키워드 |
           |------|---------------|----------------|-------------------|
           | 정재 | 아극자(음양다름) | 재물, 배우자(남) | 안정적 수입, 알뜰한 저축, 정확한 이익 |
           | 편재 | 아극자(음양같음) | 사업, 투자 | 큰 재물 획득, 투자 성공, 판단력 발휘 |
           | 정관 | 극아자(음양다름) | 명예, 직위, 남편(여) | 취업, 진급, 원칙 준수 |
           | 편관 | 극아자(음양같음) | 성공, 업적 | 어려운 일 완수, 업적 성취, 권위적 성공 |
           | 식신 | 아생자(음양같음) | 평안, 재능, 건강 | 먹고 사는 일 평안, 재능 발휘, 건강 증진 |
           | 상관 | 아생자(음양다름) | 변화, 창작, 표현 | 새로운 도전, 뛰어난 표현력, 창의성 |
           | 정인 | 생아자(음양다름) | 학업, 문서 | 합격, 좋은 문서 획득, 사려 깊은 사고 |
           | 편인 | 생아자(음양같음) | 창의, 상상 | 창의력 발휘, 독특한 아이디어 |
           | 비견 | 같음(음양같음) | 동료, 협력 | 동료의 도움, 협력 성공 |
           | 겁재 | 같음(음양다름) | 횡재, 경쟁 | 투자 성공, 횡재수, 불굴의 의지 |

           **today_fortune_summary 작성 시 활용법:**
           - 일진(日辰)의 천간/지지가 사용자의 일간(日干)과 어떤 십신 관계를 형성하는지 파악
           - 해당 십신의 대표 운세 영역(재물, 학업, 연애 등)을 구체적으로 언급
           - 긍정적 키워드를 활용하여 친근한 톤으로 작성
           - 예시: "오늘은 재물운이 좋은 날! 토의 기운을 모아 큰 기회를 잡아보세요!" (편재 관계일 때)

        4. 운세를 보는 방식
            a. 가장 먼저, 사주팔자(8글자) 내에서 명주(命主) 자신을 상징하는 **일간(日干)의 오행이 현재 운(運)을 포함한 총 16개 기운 속에서 강한지(旺) 약한지(衰)**를 판단해야 합니다.
            b. 단순 로직에서는 이 16개 글자 분포를 통해 일간을 돕는 기운(인성/비겁)이 과도하게 많아졌는지, 혹은 일간을 제어하는 기운(관살/재성/식상)이 너무 과다해졌는지를 비교하여 **오행의 치우침(過猶不及)**을 파악합니다.
            c. 만약 일간이 신약(身弱)한데 유입된 운의 오행 분포가 일간을 생조(生助)하거나 방조(幇助)하는 기운(인성/비겁)으로 채워져 균형을 맞추면 긍정적인 운세로 보고, 반대로 이미 강한 일간에 동일한 오행이 과다하게 겹치면 독불장군이나 이기적 성향이 강화되어 흉운으로 해석합니다.
            d. 이러한 분포 분석은 사주에 원래 부족하거나(無) 너무 많은(過多) 오행이 운에서 들어와서 길흉을 예측하는 기본 논리가 되며, 이는 타고난 명(命)에 운(運)이 더해져 심리적 문제 해결이나 미래 상황을 예측하는 데 활용될 수 있습니다.

        ---
        # Input Data

        [사용자 사주 정보]
        - 년주: {user_saju.yearly.two_letters} ({user_saju.yearly.stem.element.chinese}행)
        - 월주: {user_saju.monthly.two_letters} ({user_saju.monthly.stem.element.chinese}행)
        - 일주: {user_saju.daily.two_letters} (당신의 대표 오행: {user_day_element.chinese}행)
        - 시주: {user_saju.hourly.two_letters} ({user_saju.hourly.stem.element.chinese}행)

        [분석 날짜 정보]
        - 분석 날짜: {tomorrow_date.strftime('%Y년 %m월 %d일')}
        - 해당 날짜의 일진:
            - 대운: {fortune_score.elements['대운']['two_letters'] if fortune_score.elements.get('대운') else 'N/A'}
            - 세운: {fortune_score.elements['세운']['two_letters']}
            - 월운: {fortune_score.elements['월운']['two_letters']}
            - 일운: {fortune_score.elements['일운']['two_letters']} (해당 날짜의 대표 오행: {tomorrow_day_element.chinese}행)

        [오행 균형 점수]
        - 오행 균형 점수: {fortune_score.entropy_score} / 100
        - 사용자에게 필요한 오행: {fortune_score.needed_element}

        ---
        위 정보를 바탕으로 오늘의 오행 균형 설명과 개운 조언을 2-3문장씩 간결하게 작성해주세요.
        반드시 한글로 작성해야합니다.
        """
        # Generate fortune using OpenAI
        try:
            if not self.client:
                raise ValueError("OpenAI client not initialized")
            
            response = self.client.chat.completions.parse(
                model="gpt-5",
                messages=[
                    {"role": "system", "content": context},
                    {"role": "user", "content": "운세를 자세히 풀어주세요."}
                ],
                response_format=FortuneAIResponse
            )

            # Get parsed response (already a FortuneAIResponse object)
            parsed_fortune = response.choices[0].message.parsed
            return parsed_fortune

        except Exception as e:
            logger.error(f"Failed to generate fortune with AI: {e}")
            # Return default fortune on error
            needed_element = fortune_score.needed_element if fortune_score else '목'
            return FortuneAIResponse(
                today_fortune_summary=f"오늘은 조화로운 날! {needed_element}의 기운을 모아 균형을 찾아보세요.",
                today_element_balance_description=f"당신의 {compatibility['user_element']}행과 오늘의 {compatibility['tomorrow_element']}행이 만나 {compatibility['element_relation']} 관계를 형성합니다. 부족한 {needed_element}의 기운을 채워 오행의 균형을 맞추면 더욱 좋은 하루가 될 것입니다.",
                today_daily_guidance=f"오늘은 평온한 마음으로 일상의 균형을 유지하는 것이 좋습니다. 부족한 {needed_element}의 기운을 보충하기 위해 자신의 내면에 집중하며 안정적인 선택을 해보세요."
            )

    def _parse_fortune_response(self, content: str, _tomorrow_date: datetime, _compatibility: Dict[str, Any]) -> FortuneAIResponse:
        """Parse AI response into FortuneAIResponse structure."""
        # For now, create a structured response with the content
        # TODO: Implement proper JSON parsing when AI returns structured data
        return FortuneAIResponse(
            today_fortune_summary="오늘도 좋은 하루 보내세요!",
            today_element_balance_description=content[:200] + "..." if len(content) > 200 else content,
            today_daily_guidance=content[-200:] if len(content) > 200 else "오늘은 평온한 마음으로 균형을 유지하세요."
        )

    def generate_fortune_image_with_ai(
        self,
        fortune_response: FortuneAIResponse,
        user_saju: Saju,
        tomorrow_date: datetime,
        tomorrow_day_ganji: GanJi,
        fortune_score: FortuneScore
    ) -> Optional[bytes]:
        """
        Generate fortune image using OpenAI API with character image.

        Args:
            fortune_response: Generated fortune text
            user_saju: User's Saju object
            tomorrow_date: Tomorrow's date
            tomorrow_day_ganji: Tomorrow's day pillar
            fortune_score: Calculated fortune score

        Returns:
            Base64 decoded image bytes, or None on error
        """
        try:
            if not self.client:
                raise ValueError("OpenAI client not initialized")

            # Extract element information
            user_day_element = user_saju.daily.stem.element
            needed_element = fortune_score.needed_element

            # Element name mapping for prompt (Korean -> description)
            element_descriptions = {
                "목": "나무 (Wood)",
                "화": "불 (Fire)",
                "토": "흙 (Earth)",
                "금": "금속 (Metal)",
                "수": "물 (Water)"
            }
            needed_element_desc = element_descriptions.get(needed_element, needed_element)

            # Get character image path based on user's day element
            character_path = None
            try:
                character_path = self._get_character_file_path(user_day_element)
                if not os.path.exists(character_path):
                    logger.warning(f"Character file not found: {character_path}. Proceeding without character image.")
                    character_path = None
            except Exception as e:
                logger.warning(f"Failed to get character file path: {e}. Proceeding without character image.")
                character_path = None

            # Prepare image generation prompt
            image_prompt = f"""
            역할: 당신은 입력받은 텍스트(오늘의 운세)의 핵심 내용과 분위기를 파악하여, 대사 없이도 상황이 이해되는 재치 있는 네컷 만화로 각색하는 전문 웹툰 작가입니다.
            
            핵심 지시 사항: 제공된 <오늘의 운세 요약> 텍스트 전체를 읽고, 그 안에 담긴 **오늘 하루의 흐름(시작, 과정, 문제 발생, 해결)**을 당신의 창의력을 발휘하여 자유롭게 네컷 만화로 구성하세요.
            
            1. 이미지 구성 지시 (가장 중요):
            - 포맷: 정확히 4개의 개별 패널(컷)로 구성된 만화 스트립을 생성하세요.
            - 레이아웃: 2x2 그리드 형식으로, 왼쪽 상단이 1번 컷, 오른쪽 상단이 2번 컷, 왼쪽 하단이 3번 컷, 오른쪽 하단이 4번 컷이 되도록 배치하세요. 각 패널 사이에는 명확한 흰색 테두리가 있어야 합니다.
            - 텍스트 위치: 각 패널의 바로 위 중앙에 **영어 캡션(제목)**을 배치하세요. 이 캡션은 간결하고 해당 컷의 내용을 명확히 요약해야 합니다. 캡션 외의 다른 대사나 말풍선은 절대 금지합니다.
            - 스타일: 매우 유머러스하고 과장된 코믹 카툰 스타일로, 굵은 외곽선을 사용하세요.
            
            2. 주인공 및 기본 설정:
            - 주인공: 반드시 제공된 캐릭터 이미지의 스타일과 특징을 유지하여 그리세요.
            - 스타일: 대사가 전혀 없는, 재미있고 과장된 코믹 카툰 스타일.
            - 시간의 흐름: 네 컷은 반드시 [오전] -> [점심] -> [오후] -> [밤] 순서로 시간의 경과가 느껴져야 합니다. 배경의 해/달 위치나 분위기로 표현하세요.
            
            3. 만화의 내용
            - 만화의 전체 내용은 **<오늘의 운세 요약>**의 운세 흐름을 반영해야 합니다.
            - 특히 3컷(오후) 쯤에는 운세에서 말하는 **'문제 상황'이나 '부족한 점'**을 표현해야 합니다.
            - 마지막 4컷(밤)에는 {needed_element_desc} (부족한 오행 원소)를 채움으로써 문제가 해결되고 평온/만족을 되찾는 결말로 마무리하세요.
            
            <오늘의 운세 요약>
            {fortune_response.today_element_balance_description}
            
            각 컷 상단 캡션 예시:
            1컷: Energetic start based on Earth.
            2컷: Busy Flow, Water & Fire Energy
            3컷: Metal Lacking, Deadline Trouble
            4컷: Metal Added, Perfect Balance
            
            오행 원소 설명:
            - 화: 불 (Fire)
            - 수: 물 (Water)
            - 목: 나무 (Wood)
            - 금: 금속 (Metal)
            - 토: 흙 (Earth)
            """

            # With character image reference
            with open(character_path, "rb") as image_file:
                response = self.client.images.edit(
                    model="gpt-image-1",
                    image=image_file,
                    prompt=image_prompt.strip(),
                    size="1024x1536",
                )


            # Extract image data from response
            image_data = response.data[0].b64_json

            if image_data:
                # Decode base64 image
                image_bytes = base64.b64decode(image_data)
                logger.info(f"Successfully generated fortune image for {tomorrow_date}")
                return image_bytes
            else:
                logger.warning("No image data returned from OpenAI API")
                return None

        except Exception as e:
            logger.error(f"Failed to generate fortune image: {e}")
            return None

    def _get_element_color(self, element: str) -> str:
        """Get color representation for an element."""
        element_colors = {
            "목": "초록색, 청색",
            "화": "빨간색, 주황색",
            "토": "노란색, 갈색",
            "금": "흰색, 회색",
            "수": "검은색, 파란색"
        }
        return element_colors.get(element, "무지개색")

    ### public methods ###

    # Main method for fetching fortune.
    def generate_fortune(
        self,
        user: User,
        date: datetime
    ) -> Response[FortuneResponse]:
        """
        Generate fortune with race condition protection using database-level locking.

        Status flow: pending → processing → completed
        """
        from django.db import transaction

        try:
            # Get tomorrow's date
            tomorrow_date = date + timedelta(days=1)

            # Check if fortune already exists
            with transaction.atomic():
                try:
                    # Use select_for_update() to lock the row
                    fortune_result = FortuneResult.objects.select_for_update().get(
                        user_id=user.id,
                        for_date=tomorrow_date.date()
                    )

                    # If completed, return existing fortune
                    if fortune_result.status == 'completed':
                        # Build response from cached data
                        birth_time = user._convert_time_units_to_time(user.birth_time_units)
                        response_data = FortuneResponse(
                            date=tomorrow_date.strftime('%Y-%m-%d'),
                            user_id=user.id,
                            fortune=FortuneAIResponse(**fortune_result.fortune_data),
                            fortune_score=FortuneScore(**fortune_result.fortune_score),
                            saju_date=Saju.from_date(tomorrow_date.date() if isinstance(tomorrow_date, datetime) else tomorrow_date, birth_time),
                            saju_user=user.saju(),
                            daewoon=DaewoonCalculator.calculate_daewoon(user)
                        )
                        return Response(status="success", data=response_data)

                    # If pending or processing, return placeholder
                    if fortune_result.status in ['pending', 'processing']:
                        logger.info(f"Fortune generation in progress for user {user.id}, date {tomorrow_date.date()}")
                        birth_time = user._convert_time_units_to_time(user.birth_time_units)
                        response_data = FortuneResponse(
                            date=tomorrow_date.strftime('%Y-%m-%d'),
                            user_id=user.id,
                            fortune=FortuneAIResponse(**fortune_result.fortune_data),
                            fortune_score=FortuneScore(**fortune_result.fortune_score),
                            saju_date=Saju.from_date(tomorrow_date.date() if isinstance(tomorrow_date, datetime) else tomorrow_date, birth_time),
                            saju_user=user.saju(),
                            daewoon=DaewoonCalculator.calculate_daewoon(user)
                        )
                        return Response(status="success", data=response_data)

                except FortuneResult.DoesNotExist:
                    # Create placeholder record with 'processing' status (atomic)
                    user_saju = self.get_user_saju_info(user.id)
                    tomorrow_day_ganji = self.calculate_day_ganji(tomorrow_date)
                    fortune_score = self.calculate_fortune_balance(user, tomorrow_date)

                    # Get index of tomorrow's ganji in 60-ganji cycle
                    cached_ganji_list = GanJi._get_cached()
                    tomorrow_ganji_index = cached_ganji_list.index(tomorrow_day_ganji)

                    # Create placeholder fortune message
                    placeholder_fortune = FortuneAIResponse(
                        today_fortune_summary="운세를 생성하고 있습니다... 잠시만 기다려주세요!",
                        today_element_balance_description="AI가 당신의 사주와 오늘의 기운을 분석하고 있습니다.",
                        today_daily_guidance="곧 맞춤형 조언을 제공해드리겠습니다."
                    )

                    # Create with 'processing' status immediately to prevent duplicate work
                    fortune_result = FortuneResult.objects.create(
                        user_id=user.id,
                        for_date=tomorrow_date.date(),
                        status='processing',
                        gapja_code=tomorrow_ganji_index,
                        gapja_name=tomorrow_day_ganji.two_letters,
                        gapja_element=tomorrow_day_ganji.stem.element.chinese,
                        fortune_score=fortune_score.model_dump(),
                        fortune_data=placeholder_fortune.model_dump()
                    )

            # Schedule background task to generate fortune with AI
            from core.tasks import schedule_fortune_generation
            schedule_fortune_generation(user.id, tomorrow_date.strftime('%Y-%m-%d'))

            # Return placeholder response immediately
            birth_time = user._convert_time_units_to_time(user.birth_time_units)
            response_data = FortuneResponse(
                date=tomorrow_date.strftime('%Y-%m-%d'),
                user_id=user.id,
                fortune=FortuneAIResponse(**fortune_result.fortune_data),
                fortune_score=FortuneScore(**fortune_result.fortune_score),
                saju_date=Saju.from_date(tomorrow_date.date() if isinstance(tomorrow_date, datetime) else tomorrow_date, birth_time),
                saju_user=user.saju(),
                daewoon=DaewoonCalculator.calculate_daewoon(user)
            )

            return Response(status="success", data=response_data)

        except Exception as e:
            logger.error(f"Failed to generate fortune: {e}")
            return Response(status="error", error=ErrorInfo(code="fortune_generation_failed", message=str(e)))

        
    def calculate_fortune_balance(self, user: User, date: datetime) -> FortuneScore:
        """
        Calculate five elements balance score using entropy.

        Analyzes 16 items (8 stems + 8 branches) from:
        - User's saju (4 pillars)
        - Date's saju (3 pillars: yearly, monthly, daily)
        - User's daewoon (1 pillar)

        Total: 4*2 + 3*2 + 1*2 = 16 elements mapped to 5 categories (목화토금수)

        Args:
            user: User object with saju data
            date: Date to calculate fortune for

        Returns:
            FortuneScore object with entropy score and element distribution
        """
        # Convert birth_time_units string to time object
        birth_time = user._convert_time_units_to_time(user.birth_time_units)

        # Get saju from date (년주, 월주, 일주)
        ganji_from_date = Saju.from_date(date.date() if isinstance(date, datetime) else date, birth_time)

        # Get user's saju (년주, 월주, 일주, 시주)
        ganji_from_user = user.saju()

        # Get daewoon (may be None if before starting age)
        ganji_from_daewoon = DaewoonCalculator.calculate_daewoon(user)

        # Initialize element counts for 5 elements (목화토금수)
        from collections import defaultdict
        elements_count = defaultdict(int)

        # Collect all ganji to analyze (8 pillars = 16 elements)
        ganji_list = []

        # Add date pillars (3 pillars = 6 elements)
        ganji_list.extend([
            ganji_from_date.yearly,
            ganji_from_date.monthly,
            ganji_from_date.daily,
        ])

        # Add user pillars (4 pillars = 8 elements)
        ganji_list.extend([
            ganji_from_user.yearly,
            ganji_from_user.monthly,
            ganji_from_user.daily,
            ganji_from_user.hourly,
        ])

        # Add daewoon pillar (1 pillar = 2 elements) if exists
        if ganji_from_daewoon:
            ganji_list.append(ganji_from_daewoon)

        # Count elements from stems and branches
        for ganji in ganji_list:
            # Stem element (천간)
            elements_count[ganji.stem.element] += 1
            # Branch element (지지)
            elements_count[ganji.branch.element] += 1

        # Get counts for all 5 elements (목화토금수)
        # Ensure all 5 elements are present even if count is 0
        all_five_elements = [
            FiveElements.WOOD,
            FiveElements.FIRE,
            FiveElements.EARTH,
            FiveElements.METAL,
            FiveElements.WATER
        ]

        counts = [elements_count[element] for element in all_five_elements]

        # Calculate entropy score (0-100)
        entropy_score = self._five_element_entropy_score(counts)

        # Prepare detailed distribution
        element_distribution = {
            element.chinese: ElementDistribution(
                count=elements_count[element],
                percentage=round(100 * elements_count[element] / sum(counts), 1) if sum(counts) > 0 else 0
            )
            for element in all_five_elements
        }

        # Calculate needed element (minimum count element with 상생 priority)
        min_count = min(element_distribution.values(), key=lambda x: x.count).count
        min_elements = [elem for elem, dist in element_distribution.items() if dist.count == min_count]

        if len(min_elements) == 1:
            needed_element = min_elements[0]
        else:
            # Multiple elements with same min count - prioritize by 상생 relation with user's day stem
            user_day_element = ganji_from_user.daily.stem.element

            # Map element names to FiveElements objects
            element_map = {
                "목": FiveElements.WOOD,
                "화": FiveElements.FIRE,
                "토": FiveElements.EARTH,
                "금": FiveElements.METAL,
                "수": FiveElements.WATER
            }

            # Find element that empowers (생) user's day element
            # 상생: 수생목, 목생화, 화생토, 토생금, 금생수
            needed_element = None
            for elem_name in min_elements:
                elem_obj = element_map[elem_name]
                if elem_obj.empowers(user_day_element):
                    needed_element = elem_name
                    break

            # If no element empowers user (shouldn't happen but failsafe), use first one
            if not needed_element:
                needed_element = min_elements[0]

        # Helper function to convert GanJi to full dict
        def ganji_to_dict(ganji: Optional[GanJi]) -> Optional[Dict[str, Any]]:
            if ganji is None:
                return None
            return {
                "two_letters": ganji.two_letters,
                "stem": {
                    "korean_name": ganji.stem.korean_name,
                    "element": ganji.stem.element.chinese,
                    "element_color": ganji.stem.element.color,
                    "yin_yang": ganji.stem.yin_yang.value
                },
                "branch": {
                    "korean_name": ganji.branch.korean_name,
                    "element": ganji.branch.element.chinese,
                    "element_color": ganji.branch.element.color,
                    "animal": ganji.branch.animal,
                    "yin_yang": ganji.branch.yin_yang.value
                }
            }

        return FortuneScore(
            entropy_score=entropy_score,
            elements={
                "대운": ganji_to_dict(ganji_from_daewoon),
                "세운": ganji_to_dict(ganji_from_date.yearly),
                "월운": ganji_to_dict(ganji_from_date.monthly),
                "일운": ganji_to_dict(ganji_from_date.daily),
                "년주": ganji_to_dict(ganji_from_user.yearly),
                "월주": ganji_to_dict(ganji_from_user.monthly),
                "일주": ganji_to_dict(ganji_from_user.daily),
                "시주": ganji_to_dict(ganji_from_user.hourly),
            },
            element_distribution=element_distribution,
            interpretation=self._interpret_balance_score(entropy_score),
            needed_element=needed_element
        )

    def _five_element_entropy_score(self, counts: List[int]) -> float:
        """
        Calculate entropy-based balance score.

        Higher entropy (closer to 100) means more balanced distribution.
        Lower entropy means concentration in specific elements.

        Args:
            counts: List of 5 integers representing counts for each element

        Returns:
            Score from 0-100, where 100 is perfectly balanced
        """
        total = sum(counts)
        if total == 0:
            return 0.0

        # Calculate probabilities
        p = np.array(counts) / total
        p = p[p > 0]  # Filter out zeros to avoid log(0)

        # Calculate entropy: -Σ(p * log(p))
        entropy = -np.sum(p * np.log(p))

        # Maximum entropy for 5 categories is log(5)
        max_entropy = np.log(5)

        # Normalize to 0-100 scale
        score = 100 * entropy / max_entropy if max_entropy > 0 else 0

        return round(score, 2)

    def _interpret_balance_score(self, score: float) -> str:
        """Interpret entropy score as human-readable message."""
        if score >= 90:
            return "매우 균형잡힌 오행 배치입니다. 모든 방면에서 조화로운 에너지가 흐릅니다."
        elif score >= 75:
            return "균형잡힌 오행 배치입니다. 전반적으로 안정적인 기운이 있습니다."
        elif score >= 60:
            return "적당히 균형잡힌 오행 배치입니다. 특정 영역에 강점이 있습니다."
        elif score >= 40:
            return "특정 오행에 편중된 배치입니다. 장단점이 뚜렷합니다."
        else:
            return "매우 편중된 오행 배치입니다. 특정 분야에 강한 개성이 있습니다."