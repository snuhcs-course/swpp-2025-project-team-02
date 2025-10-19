"""
Fortune-telling service using Korean Saju and OpenAI API.
Generates personalized daily fortunes based on Saju compatibility and user data.
"""

import os
import json
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field
import openai
from django.conf import settings
from ..utils.concept import (
    GAPJA_SYSTEM,
    get_gapja_by_code,
    calculate_compatibility_score,
    get_element_compatibility,
    FIVE_ELEMENTS
)
from .image import ImageService
from core.models import FortuneResult
from loguru import logger


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


class FortuneResponse(BaseModel):
    """Complete fortune-telling response structure."""
    tomorrow_date: str = Field(description="Tomorrow's date in YYYY-MM-DD format")
    saju_compatibility: str = Field(description="Compatibility between user's day pillar and tomorrow's")
    overall_fortune: int = Field(description="Overall fortune score from 1-100")
    fortune_summary: str = Field(description="2-3 sentence summary of tomorrow's fortune")
    element_balance: str = Field(description="Description of Five Elements balance for tomorrow")
    chakra_readings: List[ChakraReading] = Field(description="Interpretations of today's collected chakras")
    daily_guidance: DailyGuidance = Field(description="Practical guidance for tomorrow")
    special_message: str = Field(description="Personalized encouraging message")


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

    def calculate_gapja_code(self, date: datetime) -> int:
        """
        Calculate 60 Gapja code for a given date.

        Args:
            date: The date to calculate Gapja for

        Returns:
            Gapja code (1-60)
        """
        # Simplified calculation - in production, use proper lunar calendar conversion
        # This is a placeholder calculation based on days since a reference date
        reference_date = datetime(1984, 2, 2)  # 갑자년 갑자월 갑자일
        days_diff = (date - reference_date).days
        return (days_diff % 60) + 1

    def get_user_saju_info(self, user_id: int) -> Dict[str, Any]:
        """
        Get user's Saju information from database.

        Args:
            user_id: User ID

        Returns:
            User's Saju information including birth date and pillars
        """
        # In production, this would query from User model
        # For now, return mock data
        return {
            "user_id": user_id,
            "birth_date": "1995-03-15",
            "birth_time": "14:30",
            "lunar_birth_date": "1995-02-14",
            "four_pillars": {
                "year": {"code": 12, "name": "을해", "element": "목"},  # Example
                "month": {"code": 28, "name": "기묘", "element": "토"},
                "day": {"code": 45, "name": "무신", "element": "토"},    # Day pillar is most important
                "hour": {"code": 7, "name": "경오", "element": "금"}
            }
        }

    def analyze_saju_compatibility(
        self,
        user_day_pillar: Dict[str, Any],
        tomorrow_gapja: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Analyze compatibility between user's day pillar and tomorrow's energy.

        Args:
            user_day_pillar: User's day pillar information
            tomorrow_gapja: Tomorrow's Gapja information

        Returns:
            Compatibility analysis
        """
        # Get compatibility score
        compatibility_score = calculate_compatibility_score(
            user_day_pillar["code"],
            tomorrow_gapja["code"]
        )

        # Get element relationship
        element_relation = get_element_compatibility(
            user_day_pillar["element"],
            tomorrow_gapja["stem_element"]
        )

        # Determine compatibility message
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
            "user_element": user_day_pillar["element"],
            "tomorrow_element": tomorrow_gapja["stem_element"]
        }

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
        user_saju: Dict[str, Any],
        tomorrow_date: datetime,
        tomorrow_gapja: Dict[str, Any],
        compatibility: Dict[str, Any],
        photo_contexts: List[Dict[str, Any]]
    ) -> FortuneResponse:
        """
        Generate fortune using OpenAI API with structured output.

        Args:
            user_saju: User's Saju information
            tomorrow_date: Tomorrow's date
            tomorrow_gapja: Tomorrow's Gapja
            compatibility: Compatibility analysis
            photo_contexts: Photo and location contexts

        Returns:
            Structured fortune response
        """
        # Prepare context for AI
        context = f"""
        당신은 한국 전통 사주 전문가입니다. 사용자의 사주 정보와 오늘 수집한 차크라(사진) 정보를 바탕으로 내일의 운세를 풀어주세요.

        사용자 사주 정보:
        - 일간 (Day Pillar): {user_saju['four_pillars']['day']['name']} ({user_saju['four_pillars']['day']['element']}행)
        - 생년월일: {user_saju['birth_date']}

        내일 날짜: {tomorrow_date.strftime('%Y년 %m월 %d일')}
        내일의 일진: {tomorrow_gapja['korean_name']} ({tomorrow_gapja['stem_element']}행)

        사주 궁합:
        - 점수: {compatibility['score']}/100
        - 관계: {compatibility['element_relation']}
        - 사용자 오행: {compatibility['user_element']}
        - 내일 오행: {compatibility['tomorrow_element']}

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
            return FortuneResponse(
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

    def _parse_fortune_response(self, content: str, tomorrow_date: datetime, compatibility: Dict[str, Any]) -> FortuneResponse:
        """Parse AI response into FortuneResponse structure."""
        # For now, create a structured response with the content
        # TODO: Implement proper JSON parsing when AI returns structured data
        return FortuneResponse(
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

            # Get user's Saju information
            user_saju = self.get_user_saju_info(user_id)

            # Calculate tomorrow's Gapja
            tomorrow_code = self.calculate_gapja_code(tomorrow_date)
            tomorrow_gapja = get_gapja_by_code(tomorrow_code)

            # Analyze compatibility
            compatibility = self.analyze_saju_compatibility(
                user_saju['four_pillars']['day'],
                tomorrow_gapja
            )

            # Get photo contexts if requested
            photo_contexts = []
            if include_photos:
                photo_contexts = self.prepare_photo_context(user_id, date)

            # Generate fortune with AI
            fortune = self.generate_fortune_with_ai(
                user_saju,
                tomorrow_date,
                tomorrow_gapja,
                compatibility,
                photo_contexts
            )

            # Save to database
            fortune_result, created = FortuneResult.objects.update_or_create(
                user_id=user_id,
                for_date=tomorrow_date.date(),
                defaults={
                    'gapja_code': tomorrow_code,
                    'gapja_name': tomorrow_gapja['korean_name'],
                    'gapja_element': tomorrow_gapja['stem_element'],
                    'fortune_data': fortune.model_dump() if fortune else {}
                }
            )

            # Prepare final response
            response = {
                "status": "success",
                "data": {
                    "fortune_id": fortune_result.id,
                    "user_id": user_id,
                    "generated_at": fortune_result.created_at.isoformat(),
                    "for_date": tomorrow_date.strftime('%Y-%m-%d'),
                    "tomorrow_gapja": {
                        "code": tomorrow_code,
                        "name": tomorrow_gapja['korean_name'],
                        "chinese": tomorrow_gapja['chinese_characters'],
                        "element": tomorrow_gapja['stem_element'],
                        "animal": tomorrow_gapja['animal']
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
