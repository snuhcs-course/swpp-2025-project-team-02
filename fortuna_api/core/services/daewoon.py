"""
Fortune-telling service using Korean Saju and OpenAI API.
Generates personalized daily fortunes based on Saju compatibility and user data.
"""

from enum import Enum
import os
import json
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional
from core.utils.saju_concepts import SolarTerms, YinYang, GanJi
from pydantic import BaseModel, Field
import openai
from django.conf import settings
from user.models import User
from .image import ImageService
from core.models import FortuneResult
from loguru import logger

class DaewoonDirection(Enum):
    FORWARD = "forward"
    BACKWARD = "backward"

class DaewoonCalculator:
        
    @staticmethod
    def calculate_daewoon_direction(user: User):
        """
        Calculate daewoon direction
        """
        saju = user.saju()
        yearly_yinyang = saju.yearly.stem.yin_yang
        
        if user.gender == 'M':
            if yearly_yinyang == YinYang.YANG:
                return DaewoonDirection.FORWARD
            else:
                return DaewoonDirection.BACKWARD
        else:
            if yearly_yinyang == YinYang.YANG:
                return DaewoonDirection.BACKWARD
            else:
                return DaewoonDirection.FORWARD
            
    @staticmethod
    def calculate_daewoon_starting_age(
        user: User,
    ):
        # Convert date to datetime (assuming birth at noon if time unknown)
        birth_datetime = datetime.combine(user.birth_date_solar, datetime.min.time().replace(hour=12))

        # Determine direction
        direction = DaewoonCalculator.calculate_daewoon_direction(user)

        if direction == DaewoonDirection.FORWARD:
            # Forward: days from birth to next solar term
            _, next_solar_term_datetime = SolarTerms.next_major_term_datetime(birth_datetime)
            date_offset = next_solar_term_datetime - birth_datetime
        else:
            # Backward: days from previous solar term to birth
            _, prev_solar_term_datetime = SolarTerms.previous_major_term_datetime(birth_datetime)
            date_offset = birth_datetime - prev_solar_term_datetime

        return date_offset.days // 3
                
    @staticmethod
    def calculate_daewoon(user: User) -> GanJi:
        direction = DaewoonCalculator.calculate_daewoon_direction(user)
        starting_age = DaewoonCalculator.calculate_daewoon_starting_age(user)

        monthly_ganji = user.saju().monthly
        user_current_age = (datetime.now().date() - user.birth_date_solar).days // 365 + 1 # 한국나이

        if user_current_age < starting_age:
            return None

        # First daewoon starts from the NEXT ganji after monthly ganji
        # offset 0 means 1st daewoon (monthly + 1 for forward, monthly - 1 for backward)
        # offset 1 means 2nd daewoon (monthly + 2 for forward, monthly - 2 for backward)
        offset = (user_current_age - starting_age) // 10

        # Apply direction: +1 for forward, -1 for backward on top of offset
        if direction == DaewoonDirection.FORWARD:
            directed_offset = offset + 1
        else:
            directed_offset = -(offset + 1)

        ganji_index = GanJi.get_index(monthly_ganji)
        return GanJi.find_by_index(ganji_index + directed_offset)