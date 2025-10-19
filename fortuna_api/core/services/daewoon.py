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
        _, next_solar_term_datetime = SolarTerms.next_major_term_datetime(user.birth_date_solar)
        date_offset = next_solar_term_datetime - user.birth_date_solar
        return date_offset.days // 3
                
    @staticmethod
    def calculate_daewoon(user: User):
        direction = DaewoonCalculator.calculate_daewoon_direction(user)
        starting_age = DaewoonCalculator.calculate_daewoon_starting_age(user)
        
        monthly_ganji = user.saju().monthly
        user_current_age = (datetime.now() - user.birth_date_solar).days // 365 + 1 # 한국나이
        
        if user_current_age < starting_age:
            return None
        
        # add/sub (user age offset // 10) monthly ganji 
        offset = (user_current_age - starting_age) // 10
        directed_offset = offset if direction == DaewoonDirection.FORWARD else -offset
        
        ganji_index = GanJi.get_index(monthly_ganji)
        return GanJi.find_by_index(ganji_index + directed_offset)