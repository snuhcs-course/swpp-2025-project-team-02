"""
Fortune-telling service using Korean Saju and OpenAI API.
Generates personalized daily fortunes based on Saju compatibility and user data.
"""

import os
import json
import logging
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field
from ai_sdk import generate_object, openai
from django.conf import settings
from ..utils.concept import (
    GAPJA_SYSTEM,
    get_gapja_by_code,
    calculate_compatibility_score,
    get_element_compatibility,
    FIVE_ELEMENTS
)
from .image import ImageService

logger = logging.getLogger(__name__)


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

    def __init__(self):
        """Initialize FortuneService with AI SDK model."""
        api_key = settings.OPENAI_API_KEY if hasattr(settings, 'OPENAI_API_KEY') else os.getenv('OPENAI_API_KEY')
        self.model = openai("gpt-4o-mini", api_key=api_key)
        self.image_service = ImageService()

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
            # In production, retrieve stored metadata from database
            photo_context = {
                "filename": photo["filename"],
                "url": photo["url"],
                "metadata": {
                    "timestamp": date.isoformat(),
                    "location": {
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

        # Generate fortune using AI SDK
        try:
            result = generate_object(
                model=self.model,
                schema=FortuneResponse,
                messages=[
                    {"role": "system", "content": context},
                    {"role": "user", "content": "내일의 운세를 자세히 풀어주세요."}
                ],
                config={"temperature": 0.8}
            )

            return result.object

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

            # Prepare final response
            response = {
                "status": "success",
                "data": {
                    "user_id": user_id,
                    "generated_at": datetime.now().isoformat(),
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

    def get_hourly_fortune(
        self,
        user_id: int,
        target_datetime: datetime
    ) -> Dict[str, Any]:
        """
        Generate hourly fortune based on traditional time units.

        Args:
            user_id: User ID
            target_datetime: Specific datetime for fortune

        Returns:
            Hourly fortune information
        """
        # Traditional Korean time units (시진)
        time_units = [
            {"name": "자시", "hours": (23, 1), "element": "수"},
            {"name": "축시", "hours": (1, 3), "element": "토"},
            {"name": "인시", "hours": (3, 5), "element": "목"},
            {"name": "묘시", "hours": (5, 7), "element": "목"},
            {"name": "진시", "hours": (7, 9), "element": "토"},
            {"name": "사시", "hours": (9, 11), "element": "화"},
            {"name": "오시", "hours": (11, 13), "element": "화"},
            {"name": "미시", "hours": (13, 15), "element": "토"},
            {"name": "신시", "hours": (15, 17), "element": "금"},
            {"name": "유시", "hours": (17, 19), "element": "금"},
            {"name": "술시", "hours": (19, 21), "element": "토"},
            {"name": "해시", "hours": (21, 23), "element": "수"}
        ]

        hour = target_datetime.hour
        current_time_unit = None

        for unit in time_units:
            start, end = unit["hours"]
            if start > end:  # Handle wrap around midnight
                if hour >= start or hour < end:
                    current_time_unit = unit
                    break
            elif start <= hour < end:
                current_time_unit = unit
                break

        if current_time_unit:
            user_saju = self.get_user_saju_info(user_id)
            user_element = user_saju['four_pillars']['day']['element']

            compatibility = get_element_compatibility(
                user_element,
                current_time_unit['element']
            )

            return {
                "status": "success",
                "data": {
                    "current_time": target_datetime.isoformat(),
                    "time_unit": current_time_unit['name'],
                    "time_element": current_time_unit['element'],
                    "compatibility": compatibility,
                    "advice": self._get_hourly_advice(compatibility)
                }
            }

        return {
            "status": "error",
            "message": "Unable to determine time unit"
        }

    def _get_hourly_advice(self, compatibility: str) -> str:
        """Get advice based on element compatibility."""
        advice_map = {
            "generates": "지금이 바로 행동할 최적의 시간입니다!",
            "is_generated_by": "도움을 받기 좋은 시간입니다.",
            "same": "안정적인 시간입니다.",
            "neutral": "평온한 시간입니다.",
            "destroys": "도전적인 시간이지만 극복할 수 있습니다.",
            "is_destroyed_by": "신중한 판단이 필요한 시간입니다."
        }
        return advice_map.get(compatibility, "일반적인 시간입니다.")