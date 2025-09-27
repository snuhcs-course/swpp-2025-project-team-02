import logging
from datetime import time

from django.contrib.auth.models import AbstractUser
from django.db import models
from django.utils import timezone

from .managers import UserManager
from core.utils.saju_concepts import TimeUnits, SajuCalculator

logger = logging.getLogger(__name__)

class User(AbstractUser):
    """Fortuna 사용자 모델"""

    GENDER_CHOICES = [
        ('M', '남자'),
        ('F', '여자'),
    ]

    CALENDAR_TYPE_CHOICES = [
        ('solar', '양력'),
        ('lunar', '음력'),
    ]

    TIME_UNIT_CHOICES = [
        (unit.korean_name, unit.korean_name) for unit in TimeUnits
    ]

    # 기본 정보
    email = models.EmailField(unique=True)
    google_id = models.CharField(max_length=100, unique=True, null=True, blank=True)
    profile_image = models.URLField(max_length=500, null=True, blank=True)

    # 사주 관련 정보
    birth_date_solar = models.DateField(null=True, blank=True, help_text="양력 생년월일")
    birth_date_lunar = models.DateField(null=True, blank=True, help_text="음력 생년월일")
    solar_or_lunar = models.CharField(
        max_length=10,
        choices=CALENDAR_TYPE_CHOICES,
        null=True,
        blank=True,
        help_text="사용자가 입력한 생년월일 타입"
    )
    birth_time_units = models.CharField(
        max_length=10,
        choices=TIME_UNIT_CHOICES,
        null=True,
        blank=True,
        help_text="시진"
    )
    gender = models.CharField(max_length=1, choices=GENDER_CHOICES, null=True, blank=True, help_text="성별")

    # 계산된 사주 데이터
    yearly_ganji = models.CharField(max_length=2, null=True, blank=True, help_text="년주")
    monthly_ganji = models.CharField(max_length=2, null=True, blank=True, help_text="월주")
    daily_ganji = models.CharField(max_length=2, null=True, blank=True, help_text="일주")
    hourly_ganji = models.CharField(max_length=2, null=True, blank=True, help_text="시주")

    # 앱 설정
    nickname = models.CharField(max_length=20, unique=True, null=True, blank=True)
    is_profile_complete = models.BooleanField(default=False)
    
    # 타임스탬프
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_login = models.DateTimeField(null=True, blank=True)
    
    objects = UserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']
    
    class Meta:
        db_table = 'fortuna_users'
    
    def __str__(self):
        return f"{self.email} ({self.nickname or 'No nickname'})"
    
    def update_last_login(self):
        """마지막 로그인 시간 업데이트"""
        self.last_login = timezone.now()
        self.save(update_fields=['last_login'])
    
    def check_profile_completeness(self):
        """프로필 완성도 체크"""
        # 필수 필드 리스트 정의
        required_fields = [
            self.nickname,           # 닉네임
            self.birth_date_solar,   # 양력 생년월일
            self.birth_date_lunar,   # 음력 생년월일
            self.solar_or_lunar,     # 달력 타입
            self.birth_time_units,   # 시진
            self.gender,             # 성별
        ]

        # 모든 필수 필드가 채워졌는지 확인
        is_complete = all(field is not None and field != '' for field in required_fields)

        # 닉네임 길이 추가 검증
        if self.nickname and (len(self.nickname.strip()) < 2 or len(self.nickname.strip()) > 20):
            is_complete = False

        self.is_profile_complete = is_complete
        self.save(update_fields=['is_profile_complete'])
        return self.is_profile_complete

    def set_birth_info(self, birth_date, calendar_type, time_units):
        """생년월일 정보 설정 및 사주 계산"""
        # 1. 입력 데이터 검증
        if not birth_date:
            raise ValueError("생년월일은 필수입니다.")

        if calendar_type not in ['solar', 'lunar']:
            raise ValueError("달력 타입은 'solar' 또는 'lunar'여야 합니다.")

        # 2. 양력/음력 변환 및 저장
        try:
            if calendar_type == 'solar':
                self.birth_date_solar = birth_date
                self.birth_date_lunar = SajuCalculator.solar_to_lunar(birth_date)
            else:  # lunar
                self.birth_date_lunar = birth_date
                self.birth_date_solar = SajuCalculator.lunar_to_solar(birth_date)

            self.solar_or_lunar = calendar_type
            self.birth_time_units = time_units

        except Exception as e:
            logger.error(f"달력 변환 오류: {e}")
            # 변환 실패 시 원본 데이터 사용
            if calendar_type == 'solar':
                self.birth_date_solar = birth_date
                self.birth_date_lunar = birth_date  # Fallback
            else:
                self.birth_date_lunar = birth_date
                self.birth_date_solar = birth_date  # Fallback

            self.solar_or_lunar = calendar_type
            self.birth_time_units = time_units

        # 3. 사주 계산
        self._calculate_saju_data()

    def _calculate_saju_data(self):
        """사주 데이터 계산 (내부 메서드)"""
        if not self.birth_date_lunar:
            return

        try:
            # 기본 시간 설정 (오정 12시 = 12:00)
            default_birth_time = time(12, 0)

            # 사주 계산 수행
            saju_data = SajuCalculator.calculate_saju(
                self.birth_date_lunar,
                default_birth_time
            )

            # 계산 결과 저장
            self.yearly_ganji = saju_data['yearly_ganji']
            self.monthly_ganji = saju_data['monthly_ganji']
            self.daily_ganji = saju_data['daily_ganji']
            self.hourly_ganji = saju_data['hourly_ganji']

        except Exception as e:
            logger.error(f"사주 계산 오류: {e}")
            # 계산 실패 시 기본값 설정
            self.yearly_ganji = None
            self.monthly_ganji = None
            self.daily_ganji = None
            self.hourly_ganji = None
