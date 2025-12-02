import logging
from datetime import time

from django.contrib.auth.models import AbstractUser
from django.db import models
from django.utils import timezone

from .managers import UserManager
from core.utils.saju_concepts import GanJi, Saju, TimeUnits, SajuCalculator

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
        (unit.value, unit.value) for unit in TimeUnits
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
    nickname = models.CharField(max_length=20, null=True, blank=True)
    is_profile_complete = models.BooleanField(default=False)
    
    # 타임스탬프
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_login = models.DateTimeField(null=True, blank=True)
    deleted_at = models.DateTimeField(null=True, blank=True, help_text="탈퇴 시간 (soft delete)")

    objects = UserManager()
    all_objects = models.Manager()  # 탈퇴한 사용자 포함 전체 조회용

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = []
    
    class Meta:
        db_table = 'fortuna_users'
    
    def __str__(self):
        return f"{self.email} ({self.nickname or 'No nickname'})"
    
    def saju(self):
        return Saju(
            yearly=GanJi.find_by_name(self.yearly_ganji),
            monthly=GanJi.find_by_name(self.monthly_ganji),
            daily=GanJi.find_by_name(self.daily_ganji),
            hourly=GanJi.find_by_name(self.hourly_ganji),
        )
    
    def update_last_login(self):
        """마지막 로그인 시간 업데이트 - 부모 메서드에서 save() 호출 필요"""
        self.last_login = timezone.now()

    def soft_delete(self):
        """
        사용자 탈퇴 처리 (soft delete)

        deleted_at 필드에 현재 시간을 기록하여 탈퇴 처리
        이 메서드 호출 후 반드시 save() 필요
        """
        self.deleted_at = timezone.now()

    def is_deleted(self):
        """사용자가 탈퇴했는지 확인"""
        return self.deleted_at is not None
    
    def update_profile_completeness_status(self):
        """
        프로필 완성도 검증 및 업데이트

        필수 필드(닉네임, 생년월일, 성별 등) 검증 후 is_profile_complete 플래그 업데이트
        이 메서드 호출 후 반드시 save() 필요
        """
        is_complete = self._validate_profile_completeness()
        self.is_profile_complete = is_complete
        return self.is_profile_complete

    def _validate_profile_completeness(self) -> bool:
        """프로필 완성도 검증"""
        if not self._validate_required_fields_filled():
            return False

        if not self._validate_nickname():
            return False

        return True

    def _validate_required_fields_filled(self) -> bool:
        """필수 필드가 모두 채워졌는지 검증"""
        required_fields = [
            self.nickname,
            self.birth_date_solar,
            self.birth_date_lunar,
            self.solar_or_lunar,
            self.birth_time_units,
            self.gender,
        ]

        return all(field is not None and field != '' for field in required_fields)

    def _validate_nickname(self) -> bool:
        """닉네임 유효성 검증"""
        if not self.nickname:
            return False

        nickname_length = len(self.nickname.strip())
        return 2 <= nickname_length <= 20

    def set_birth_date_and_calculate_saju(self, birth_date, calendar_type, time_units):
        """
        생년월일 정보 설정 및 사주팔자 계산

        사용자가 입력한 생년월일을 양력/음력으로 변환하여 저장하고
        사주팔자(년주/월주/일주/시주)를 계산
        """
        # 입력 데이터 검증
        self._validate_birth_date_input(birth_date, calendar_type)

        # 양력/음력 변환 및 저장
        self._convert_and_store_birth_dates(birth_date, calendar_type, time_units)

        # 사주팔자 계산 및 저장
        self._calculate_and_store_saju_pillars()

    def _validate_birth_date_input(self, birth_date, calendar_type):
        """생년월일 입력 데이터 검증"""
        if not birth_date:
            raise ValueError("생년월일은 필수입니다.")

        if calendar_type not in ['solar', 'lunar']:
            raise ValueError("달력 타입은 'solar' 또는 'lunar'여야 합니다.")

    def _convert_and_store_birth_dates(self, birth_date, calendar_type, time_units):
        """
        양력/음력 변환 및 저장

        사용자가 입력한 달력 타입(양력/음력)에 따라 반대 타입으로 변환
        변환 실패 시 양쪽 모두 동일한 날짜로 저장 (fallback)
        """
        try:
            # 입력받은 달력 타입에 따라 반대 타입으로 변환
            self._perform_calendar_conversion(birth_date, calendar_type)
            self.solar_or_lunar = calendar_type
            self.birth_time_units = time_units

        except Exception as error:
            logger.error(f"달력 변환 오류: {error}")
            # 변환 실패 시 양쪽 날짜를 동일하게 설정 (fallback)
            self._handle_conversion_failure(birth_date, calendar_type, time_units)

    def _perform_calendar_conversion(self, birth_date, calendar_type):
        """달력 변환 수행"""
        if calendar_type == 'solar':
            self.birth_date_solar = birth_date
            self.birth_date_lunar = SajuCalculator.solar_to_lunar(birth_date)
        else:  # lunar
            self.birth_date_lunar = birth_date
            self.birth_date_solar = SajuCalculator.lunar_to_solar(birth_date)

    def _handle_conversion_failure(self, birth_date, calendar_type, time_units):
        """달력 변환 실패 시 처리"""
        if calendar_type == 'solar':
            self.birth_date_solar = birth_date
            self.birth_date_lunar = birth_date  # Fallback
        else:
            self.birth_date_lunar = birth_date
            self.birth_date_solar = birth_date  # Fallback

        self.solar_or_lunar = calendar_type
        self.birth_time_units = time_units

    def _convert_time_units_to_time(self, time_units_str: str) -> time:
        """
        십이시(時辰) 문자열을 대표 시간으로 변환

        각 시진의 중간 시간을 대표값으로 사용
        예: 미시(13:30~15:30) -> 14:30

        Args:
            time_units_str: 시진 문자열 (예: "미시", "오시")

        Returns:
            해당 시진의 대표 시간 (time 객체)
        """
        # 시진별 대표 시간 매핑 (각 시진의 중간 시간)
        time_units_mapping = {
            '자시': time(1, 0),   # 00:30~01:30 중간
            '축시': time(2, 30),  # 01:30~03:30 중간
            '인시': time(4, 30),  # 03:30~05:30 중간
            '묘시': time(6, 30),  # 05:30~07:30 중간
            '진시': time(8, 30),  # 07:30~09:30 중간
            '사시': time(10, 30), # 09:30~11:30 중간
            '오시': time(12, 30), # 11:30~13:30 중간
            '미시': time(14, 30), # 13:30~15:30 중간
            '신시': time(16, 30), # 15:30~17:30 중간
            '유시': time(18, 30), # 17:30~19:30 중간
            '술시': time(20, 30), # 19:30~21:30 중간
            '해시': time(22, 30), # 21:30~23:30 중간
            '야자시': time(0, 0), # 23:30~00:30 중간 (자정)
        }

        # 매핑에서 찾거나 기본값(오정 12:30) 반환
        return time_units_mapping.get(time_units_str, time(12, 30))

    def _calculate_and_store_saju_pillars(self):
        """
        사주팔자(년주/월주/일주/시주) 계산 및 저장

        양력 생년월일을 기준으로 사주팔자를 계산
        사용자가 입력한 birth_time_units를 반영
        계산 실패 시 모든 간지 필드를 None으로 설정
        """
        if not self.birth_date_solar:
            return

        try:
            # 사용자가 입력한 시진을 시간으로 변환
            if self.birth_time_units:
                birth_time = self._convert_time_units_to_time(self.birth_time_units)
            else:
                # 기본값: 오정 12:30 - 정확한 시간을 모를 때 사용
                birth_time = time(12, 30)

            # 사주팔자 계산 (년주, 월주, 일주, 시주)
            saju_pillars_data = SajuCalculator.calculate_saju(
                self.birth_date_solar,
                birth_time
            )

            # 계산된 간지 저장
            self.yearly_ganji = saju_pillars_data['yearly_ganji']
            self.monthly_ganji = saju_pillars_data['monthly_ganji']
            self.daily_ganji = saju_pillars_data['daily_ganji']
            self.hourly_ganji = saju_pillars_data['hourly_ganji']

        except Exception as error:
            logger.error(f"사주 계산 오류: {error}")
            # 계산 실패 시 모든 간지를 None으로 설정
            self.yearly_ganji = None
            self.monthly_ganji = None
            self.daily_ganji = None
            self.hourly_ganji = None
