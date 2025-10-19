"""
Fortune-telling service using Korean Saju and OpenAI API.
Generates personalized daily fortunes based on Saju compatibility and user data.
"""

import os
import json
from datetime import datetime, timedelta
from typing import Dict, Any, Generic, List, Literal, Optional, TypeVar
from core.services.daewoon import DaewoonCalculator
from pydantic import BaseModel, Field
import openai
from django.conf import settings
from user.models import User
from ..utils.saju_concepts import (
    Saju,
    GanJi,
    FiveElements,
    TenStems,
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

class ChakraReading(BaseModel):
    """Individual Chakra reading based on photo and location."""
    chakra_type: str = Field(description="Type of chakra energy detected")
    strength: int = Field(description="Strength of chakra from 1-100")
    message: str = Field(description="Interpretation of this chakra")
    location_significance: str = Field(description="Meaning of the location where photo was taken")


class DailyGuidance(BaseModel):
    """Actionable guidance for the next day."""
    best_time: str = Field(description="Best time period for important activities (e.g., '오전 9-11시')")
    lucky_direction: str = Field(description="Lucky direction based on Five Elements")
    lucky_color: str = Field(description="Lucky color for tomorrow")
    activities_to_embrace: List[str] = Field(description="3-5 recommended activities")
    activities_to_avoid: List[str] = Field(description="2-3 activities to be cautious about")
    key_advice: str = Field(description="One key piece of advice for tomorrow")


class FortuneAIResponse(BaseModel):
    """Complete fortune-telling response structure."""
    tomorrow_date: str = Field(description="Tomorrow's date in YYYY-MM-DD format")
    saju_compatibility: str = Field(description="Compatibility between user's day pillar and tomorrow's")
    overall_fortune: int = Field(description="Overall fortune score from 1-100")
    fortune_summary: str = Field(description="2-3 sentence summary of tomorrow's fortune")
    element_balance: str = Field(description="Description of Five Elements balance for tomorrow")
    chakra_readings: List[ChakraReading] = Field(description="Interpretations of today's collected chakras")
    daily_guidance: DailyGuidance = Field(description="Practical guidance for tomorrow")
    special_message: str = Field(description="Personalized encouraging message")

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
        photo_contexts: List[Dict[str, Any]]
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
        당신은 한국 전통 사주 전문가입니다. 사용자의 사주 정보와 오늘 수집한 차크라(사진) 정보를 바탕으로 내일의 운세를 풀어주세요.

        사용자 사주 정보:
        - 년주: {user_saju.yearly.two_letters} ({user_saju.yearly.stem.element.chinese}행)
        - 월주: {user_saju.monthly.two_letters} ({user_saju.monthly.stem.element.chinese}행)
        - 일주: {user_saju.daily.two_letters} ({user_day_element.chinese}행)
        - 시주: {user_saju.hourly.two_letters} ({user_saju.hourly.stem.element.chinese}행)

        내일 날짜: {tomorrow_date.strftime('%Y년 %m월 %d일')}
        내일의 일진: {tomorrow_day_ganji.two_letters} ({tomorrow_day_element.chinese}행)

        사주 궁합:
        - 점수: {compatibility['score']}/100
        - 관계: {compatibility['element_relation']}
        - 상세: {compatibility['relation_detail']}
        - 사용자 오행: {compatibility['user_element']}행 ({compatibility['user_element_color']})
        - 내일 오행: {compatibility['tomorrow_element']}행 ({compatibility['tomorrow_element_color']})

        오늘 수집된 차크라 정보:
        """

        for i, photo in enumerate(photo_contexts, 1):
            if photo['metadata']['location']:
                context += f"""
        차크라 {i}:
        - 시간: {photo['metadata']['timestamp']}
        - 위치: 위도 {photo['metadata']['location']['latitude']}, 경도 {photo['metadata']['location']['longitude']}
        """
            else:
                context += f"""
        차크라 {i}:
        - 시간: {photo['metadata']['timestamp']}
        - 위치: 정보 없음
        """

        context += """

        위 정보를 바탕으로 GenZ 사용자가 쉽게 이해할 수 있는 내일의 운세를 작성해주세요.
        전통적인 사주 해석을 현대적이고 실용적으로 풀어서 설명하고,
        구체적이고 실천 가능한 조언을 제공해주세요.
        """

        # Generate fortune using OpenAI
        try:
            if not self.client:
                raise ValueError("OpenAI client not initialized")
            
            response = self.client.chat.completions.create(
                model="gpt-5-nano",
                messages=[
                    {"role": "system", "content": context},
                    {"role": "user", "content": "내일의 운세를 자세히 풀어주세요."}
                ]
            )

            # Parse the response content into a structured format
            content = response.choices[0].message.content

            # For now, return a basic structured response
            # TODO: Implement proper JSON parsing or structured output
            return self._parse_fortune_response(content, tomorrow_date, compatibility)

        except Exception as e:
            logger.error(f"Failed to generate fortune with AI: {e}")
            # Return default fortune on error
            return FortuneAIResponse(
                tomorrow_date=tomorrow_date.strftime('%Y-%m-%d'),
                saju_compatibility=f"{compatibility['level']} ({compatibility['score']}/100)",
                overall_fortune=compatibility['score'],
                fortune_summary=compatibility['message'],
                element_balance=f"{compatibility['user_element']}행과 {compatibility['tomorrow_element']}행의 조화",
                chakra_readings=[
                    ChakraReading(
                        chakra_type="기본",
                        strength=50,
                        message="오늘 하루의 에너지가 모였습니다.",
                        location_significance="일상의 소중함"
                    )
                ],
                daily_guidance=DailyGuidance(
                    best_time="오전 9-11시",
                    lucky_direction="동쪽",
                    lucky_color="청색",
                    activities_to_embrace=["새로운 시작", "대화와 소통", "창의적 활동"],
                    activities_to_avoid=["큰 결정", "논쟁"],
                    key_advice="오늘의 작은 노력이 내일의 큰 성과로 이어집니다."
                ),
                special_message="당신의 내일이 밝고 희망찬 날이 되기를 기원합니다."
            )

    def _parse_fortune_response(self, content: str, tomorrow_date: datetime, compatibility: Dict[str, Any]) -> FortuneAIResponse:
        """Parse AI response into FortuneAIResponse structure."""
        # For now, create a structured response with the content
        # TODO: Implement proper JSON parsing when AI returns structured data
        return FortuneAIResponse(
            tomorrow_date=tomorrow_date.strftime('%Y-%m-%d'),
            saju_compatibility=f"{compatibility['level']} ({compatibility['score']}/100)",
            overall_fortune=compatibility['score'],
            fortune_summary=content[:200] + "..." if len(content) > 200 else content,
            element_balance=f"{compatibility['user_element']}행과 {compatibility['tomorrow_element']}행의 조화",
            chakra_readings=[
                ChakraReading(
                    chakra_type="AI 해석",
                    strength=75,
                    message="AI가 생성한 종합적인 해석입니다.",
                    location_significance="전체적인 에너지 흐름"
                )
            ],
            daily_guidance=DailyGuidance(
                best_time="오전 9-11시",
                lucky_direction="동쪽",
                lucky_color="청색",
                activities_to_embrace=["새로운 시작", "대화와 소통"],
                activities_to_avoid=["큰 결정", "논쟁"],
                key_advice=content[-100:] if len(content) > 100 else "오늘의 작은 노력이 내일의 큰 성과로 이어집니다."
            ),
            special_message="AI가 생성한 맞춤형 운세입니다."
        )

    ### public methods ###
    
    # todo - fade out.
    def generate_tomorrow_fortune(
        self,
        user_id: int,
        date: datetime,
        include_photos: bool = True
    ) -> Dict[str, Any]:
        """
        Generate complete tomorrow's fortune for a user.

        Args:
            user_id: User ID
            date: Date for which to generate fortune (photos from this date)
            include_photos: Whether to include photo analysis

        Returns:
            Complete fortune response
        """
        try:
            # Get tomorrow's date
            tomorrow_date = date + timedelta(days=1)

            # Get user's Saju information (returns Saju object)
            user_saju = self.get_user_saju_info(user_id)

            # Calculate tomorrow's day pillar (일주)
            tomorrow_day_ganji = self.calculate_day_ganji(tomorrow_date)

            # Analyze compatibility between user's day pillar and tomorrow's day pillar
            compatibility = self.analyze_saju_compatibility(
                user_saju.daily,  # User's day pillar
                tomorrow_day_ganji  # Tomorrow's day pillar
            )

            # Get photo contexts if requested
            photo_contexts = []
            if include_photos:
                photo_contexts = self.prepare_photo_context(user_id, date)

            # Generate fortune with AI
            fortune = self.generate_fortune_with_ai(
                user_saju,
                tomorrow_date,
                tomorrow_day_ganji,
                compatibility,
                photo_contexts
            )

            # Get index of tomorrow's ganji in 60-ganji cycle for storage
            cached_ganji_list = GanJi._get_cached()
            tomorrow_ganji_index = cached_ganji_list.index(tomorrow_day_ganji)

            # Save to database
            fortune_result, created = FortuneResult.objects.update_or_create(
                user_id=user_id,
                for_date=tomorrow_date.date(),
                defaults={
                    'gapja_code': tomorrow_ganji_index,
                    'gapja_name': tomorrow_day_ganji.two_letters,
                    'gapja_element': tomorrow_day_ganji.stem.element.chinese,
                    'fortune_data': fortune.model_dump() if fortune else {}
                }
            )

            # Prepare final response (dict format for backward compatibility)
            response = {
                "status": "success",
                "data": {
                    "fortune_id": fortune_result.id,
                    "user_id": user_id,
                    "generated_at": fortune_result.created_at.isoformat(),
                    "for_date": tomorrow_date.strftime('%Y-%m-%d'),
                    "tomorrow_gapja": {
                        "code": tomorrow_ganji_index,
                        "name": tomorrow_day_ganji.two_letters,
                        "stem": tomorrow_day_ganji.stem.korean_name,
                        "branch": tomorrow_day_ganji.branch.korean_name,
                        "element": tomorrow_day_ganji.stem.element.chinese,
                        "element_color": tomorrow_day_ganji.stem.element.color,
                        "animal": tomorrow_day_ganji.branch.animal
                    },
                    "fortune": fortune.model_dump() if fortune else None
                }
            }

            return response

        except Exception as e:
            logger.error(f"Failed to generate fortune: {e}")
            return {
                "status": "error",
                "message": str(e)
            }

    # new method for fetching fortune.
    def generate_fortune(
        self,
        user: User,
        date: datetime
    ) -> Response[FortuneResponse]:
        try:
            # Get tomorrow's date
            tomorrow_date = date + timedelta(days=1)

            # Get user's Saju information (returns Saju object)
            user_saju = self.get_user_saju_info(user.id)

            # Calculate tomorrow's day pillar (일주)
            tomorrow_day_ganji = self.calculate_day_ganji(tomorrow_date)

            # Analyze compatibility between user's day pillar and tomorrow's day pillar
            compatibility = self.analyze_saju_compatibility(
                user_saju.daily,  # User's day pillar
                tomorrow_day_ganji  # Tomorrow's day pillar
            )

            # Don't include photos for today's fortune
            photo_contexts = []

            # Generate fortune with AI
            fortune = self.generate_fortune_with_ai(
                user_saju,
                tomorrow_date,
                tomorrow_day_ganji,
                compatibility,
                photo_contexts
            )

            # Get index of tomorrow's ganji in 60-ganji cycle for storage
            cached_ganji_list = GanJi._get_cached()
            tomorrow_ganji_index = cached_ganji_list.index(tomorrow_day_ganji)

            # Calculate fortune score
            fortune_score = self.calculate_fortune_balance(user, date)

            # Save to database
            fortune_result, created = FortuneResult.objects.update_or_create(
                user_id=user.id,
                for_date=tomorrow_date.date(),
                defaults={
                    'gapja_code': tomorrow_ganji_index,
                    'gapja_name': tomorrow_day_ganji.two_letters,
                    'gapja_element': tomorrow_day_ganji.stem.element.chinese,
                    'fortune_data': fortune.model_dump() if fortune else {},
                    'fortune_score': fortune_score.model_dump()
                }
            )

            # Prepare final response
            response_data = FortuneResponse(
                date=date.strftime('%Y-%m-%d'),
                user_id=user.id,
                fortune=fortune,
                fortune_score=fortune_score,
                saju_date=Saju.from_date(date.date() if isinstance(date, datetime) else date, user._convert_time_units_to_time(user.birth_time_units)),
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
            interpretation=self._interpret_balance_score(entropy_score)
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