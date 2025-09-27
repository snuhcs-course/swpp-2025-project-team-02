"""
Korean Saju (四柱) concepts and calculations.
Based on the traditional 60 Gapja system and Five Elements theory.
"""
from enum import Enum
from datetime import datetime, date, time
from typing import Tuple, Optional
from lunarcalendar import Converter, Solar, Lunar


class YinYang(Enum):
    """음양 (Yin-Yang)"""
    YIN = "음"
    YANG = "양"


class FiveElements(Enum):
    """오행 (Five Elements)"""
    WOOD = "목"
    FIRE = "화"
    EARTH = "토"
    METAL = "금"
    WATER = "수"


class TenStems(Enum):
    """십간 (Ten Heavenly Stems)"""
    GAHP = ("갑", YinYang.YANG, FiveElements.WOOD)
    EUL = ("을", YinYang.YIN, FiveElements.WOOD)
    BYUNG = ("병", YinYang.YANG, FiveElements.FIRE)
    JUNG = ("정", YinYang.YIN, FiveElements.FIRE)
    MU = ("무", YinYang.YANG, FiveElements.EARTH)
    GI = ("기", YinYang.YIN, FiveElements.EARTH)
    GYUNG = ("경", YinYang.YANG, FiveElements.METAL)
    SIN = ("신", YinYang.YIN, FiveElements.METAL)
    IM = ("임", YinYang.YANG, FiveElements.WATER)
    GYE = ("계", YinYang.YIN, FiveElements.WATER)

    def __init__(self, korean_name: str, yin_yang: YinYang, element: FiveElements):
        self.korean_name = korean_name
        self.yin_yang = yin_yang
        self.element = element

    @classmethod
    def get_by_index(cls, index: int) -> 'TenStems':
        """Get stem by index (0-9)"""
        stems = list(cls)
        return stems[index % 10]

    @classmethod
    def find_by_name(cls, name: str) -> 'TenStems':
        """Find stem by Korean name"""
        for stem in cls:
            if stem.korean_name == name:
                return stem
        raise ValueError(f"Unknown stem name: {name}")


class TwelveBranches(Enum):
    """십이지 (Twelve Earthly Branches)"""
    JA = ("자", "쥐", YinYang.YANG, FiveElements.WATER)
    CHUK = ("축", "소", YinYang.YIN, FiveElements.EARTH)
    IN = ("인", "범", YinYang.YANG, FiveElements.WOOD)
    MYO = ("묘", "토끼", YinYang.YIN, FiveElements.WOOD)
    JIN = ("진", "용", YinYang.YANG, FiveElements.EARTH)
    SA = ("사", "뱀", YinYang.YIN, FiveElements.FIRE)
    O = ("오", "말", YinYang.YANG, FiveElements.FIRE)
    MI = ("미", "양", YinYang.YIN, FiveElements.EARTH)
    SIN = ("신", "원숭이", YinYang.YANG, FiveElements.METAL)
    YU = ("유", "닭", YinYang.YIN, FiveElements.METAL)
    SUL = ("술", "개", YinYang.YANG, FiveElements.EARTH)
    HAE = ("해", "돼지", YinYang.YIN, FiveElements.WATER)

    def __init__(self, korean_name: str, animal: str, yin_yang: YinYang, element: FiveElements):
        self.korean_name = korean_name
        self.animal = animal
        self.yin_yang = yin_yang
        self.element = element

    @classmethod
    def get_by_index(cls, index: int) -> 'TwelveBranches':
        """Get branch by index (0-11)"""
        branches = list(cls)
        return branches[index % 12]

    @classmethod
    def find_by_name(cls, name: str) -> 'TwelveBranches':
        """Find branch by Korean name"""
        for branch in cls:
            if branch.korean_name == name:
                return branch
        raise ValueError(f"Unknown branch name: {name}")


class TimeUnits(Enum):
    """십이시 (Traditional Korean Time Units)"""
    JA_SI = ("자시", (23, 1), TwelveBranches.JA)
    CHUK_SI = ("축시", (1, 3), TwelveBranches.CHUK)
    IN_SI = ("인시", (3, 5), TwelveBranches.IN)
    MYO_SI = ("묘시", (5, 7), TwelveBranches.MYO)
    JIN_SI = ("진시", (7, 9), TwelveBranches.JIN)
    SA_SI = ("사시", (9, 11), TwelveBranches.SA)
    O_SI = ("오시", (11, 13), TwelveBranches.O)
    MI_SI = ("미시", (13, 15), TwelveBranches.MI)
    SIN_SI = ("신시", (15, 17), TwelveBranches.SIN)
    YU_SI = ("유시", (17, 19), TwelveBranches.YU)
    SUL_SI = ("술시", (19, 21), TwelveBranches.SUL)
    HAE_SI = ("해시", (21, 23), TwelveBranches.HAE)
    UNKNOWN = ("모름", None, None)

    def __init__(self, korean_name: str, hours: Optional[Tuple[int, int]], branch: Optional[TwelveBranches]):
        self.korean_name = korean_name
        self.hours = hours
        self.branch = branch

    @classmethod
    def from_time(cls, time_obj: time) -> 'TimeUnits':
        """Get time unit from time object"""
        hour = time_obj.hour

        for unit in cls:
            if unit.hours is None:  # UNKNOWN case
                continue
            start, end = unit.hours
            if start > end:  # Handle wrap around midnight (자시)
                if hour >= start or hour < end:
                    return unit
            elif start <= hour < end:
                return unit

        return cls.UNKNOWN


class GanJi:
    """간지 (Sexagenary Cycle) - combination of stem and branch"""

    def __init__(self, stem: TenStems, branch: TwelveBranches):
        self.stem = stem
        self.branch = branch
        self.korean_name = stem.korean_name + branch.korean_name

    @classmethod
    def get_by_index(cls, index: int) -> 'GanJi':
        """Get GanJi by index (0-59)"""
        stem_index = index % 10
        branch_index = index % 12
        stem = TenStems.get_by_index(stem_index)
        branch = TwelveBranches.get_by_index(branch_index)
        return cls(stem, branch)

    @classmethod
    def find_by_name(cls, name: str) -> 'GanJi':
        """Find GanJi by Korean name (2 characters)"""
        if len(name) != 2:
            raise ValueError(f"GanJi name must be 2 characters: {name}")

        stem_name = name[0]
        branch_name = name[1]
        stem = TenStems.find_by_name(stem_name)
        branch = TwelveBranches.find_by_name(branch_name)
        return cls(stem, branch)

    def __str__(self):
        return self.korean_name

    def __eq__(self, other):
        if isinstance(other, GanJi):
            return self.stem == other.stem and self.branch == other.branch
        return False


class SajuCalculator:
    """사주 계산기"""

    BASE_DATE_1900 = date(1900, 1, 1)  # 경자년 기준
    BASE_GANJI_1900 = GanJi.find_by_name("경자")

    # 일진 계산을 위한 기준일 (1925년 2월 9일 = 갑자일)
    BASE_DATE_1925 = date(1925, 2, 9)

    @staticmethod
    def solar_to_lunar(solar_date: date) -> date:
        """Convert solar date to lunar date"""
        try:
            solar = Solar(solar_date.year, solar_date.month, solar_date.day)
            lunar = Converter.Solar2Lunar(solar)
            return date(lunar.year, lunar.month, lunar.day)
        except Exception:
            # If conversion fails, return original date
            return solar_date

    @staticmethod
    def lunar_to_solar(lunar_date: date) -> date:
        """Convert lunar date to solar date"""
        try:
            lunar = Lunar(lunar_date.year, lunar_date.month, lunar_date.day)
            solar = Converter.Lunar2Solar(lunar)
            return date(solar.year, solar.month, solar.day)
        except Exception:
            # If conversion fails, return original date
            return lunar_date

    @classmethod
    def calculate_yearly_ganji(cls, lunar_date: date) -> GanJi:
        """음력 생년월일로부터 년주(年柱) 간지 계산"""
        # 1900년(경자년)을 기준으로 연도 차이 계산
        year_offset = lunar_date.year - 1900

        # 1900년 = 경자년 기준 인덱스 계산
        base_stem_index = list(TenStems).index(TenStems.GYUNG)   # 경(庚)
        base_branch_index = list(TwelveBranches).index(TwelveBranches.JA)  # 자(子)

        # 연도 차이만큼 오프셋 적용
        yearly_stem = TenStems.get_by_index(base_stem_index + year_offset)
        yearly_branch = TwelveBranches.get_by_index(base_branch_index + year_offset)

        return GanJi(yearly_stem, yearly_branch)

    @classmethod
    def calculate_monthly_ganji(cls, lunar_date: date, yearly_ganji: GanJi) -> GanJi:
        """음력 날짜와 년주 간지를 기반으로 월주(月柱) 간지 계산"""
        month = lunar_date.month

        # 년간에 따른 월간 기준 맵핑 (전통 사주학 규칙)
        # 각 년간에 따라 월간의 시작 기준이 달라짐
        yearly_stem_to_monthly_base = {
            # 갑년/기년 -> 병월 시작
            TenStems.GAHP: TenStems.BYUNG,   # 갑 -> 병
            TenStems.GI: TenStems.BYUNG,     # 기 -> 병

            # 을년/경년 -> 무월 시작
            TenStems.EUL: TenStems.MU,       # 을 -> 무
            TenStems.GYUNG: TenStems.MU,     # 경 -> 무

            # 병년/신년 -> 경월 시작
            TenStems.BYUNG: TenStems.GYUNG,  # 병 -> 경
            TenStems.SIN: TenStems.GYUNG,    # 신 -> 경

            # 정년/임년 -> 임월 시작
            TenStems.JUNG: TenStems.IM,      # 정 -> 임
            TenStems.IM: TenStems.IM,        # 임 -> 임

            # 무년/계년 -> 갑월 시작
            TenStems.MU: TenStems.GAHP,      # 무 -> 갑
            TenStems.GYE: TenStems.GAHP      # 계 -> 갑
        }

        # 년간에 따른 월간 기준 결정
        monthly_base_stem = yearly_stem_to_monthly_base.get(
            yearly_ganji.stem,
            TenStems.GAHP
        )

        # 월간 계산 (인월부터 시작, month-1 오프셋)
        monthly_stem_index = list(TenStems).index(monthly_base_stem) + (month - 1)
        monthly_stem = TenStems.get_by_index(monthly_stem_index)

        # 월지 계산 (인월=인지부터 순차적 배치)
        monthly_branch_index = (month - 1) % 12
        monthly_branch = TwelveBranches.get_by_index(monthly_branch_index)

        return GanJi(monthly_stem, monthly_branch)

    @classmethod
    def calculate_daily_ganji(cls, lunar_date: date) -> GanJi:
        """음력 날짜로부터 일주(日柱) 간지 계산"""
        # 기준일(갑자일)로부터의 날짜 차이 계산
        days_difference = (lunar_date - cls.BASE_DATE_1925).days

        # 60갑자 순환으로 일진 계산
        daily_ganji_index = days_difference % 60

        return GanJi.get_by_index(daily_ganji_index)

    @classmethod
    def calculate_hourly_ganji(cls, birth_time: time, daily_ganji: GanJi) -> GanJi:
        """출생 시간과 일주 간지로부터 시주(時柱) 간지 계산"""
        # 시간을 전통 시진으로 변환
        time_unit = TimeUnits.from_time(birth_time)

        # 시진을 알 수 없는 경우 기본값 반환
        if time_unit == TimeUnits.UNKNOWN or time_unit.branch is None:
            return GanJi(TenStems.GAHP, TwelveBranches.JA)

        # 일간에 따른 시간 기준 맵핑 (전통 사주학 규칙)
        daily_stem_to_hourly_base = {
            # 갑일/기일 -> 갑자시 시작
            TenStems.GAHP: TenStems.GAHP,     # 갑 -> 갑
            TenStems.GI: TenStems.GAHP,       # 기 -> 갑

            # 을일/경일 -> 병자시 시작
            TenStems.EUL: TenStems.BYUNG,     # 을 -> 병
            TenStems.GYUNG: TenStems.BYUNG,   # 경 -> 병

            # 병일/신일 -> 무자시 시작
            TenStems.BYUNG: TenStems.MU,      # 병 -> 무
            TenStems.SIN: TenStems.MU,        # 신 -> 무

            # 정일/임일 -> 경자시 시작
            TenStems.JUNG: TenStems.GYUNG,    # 정 -> 경
            TenStems.IM: TenStems.GYUNG,      # 임 -> 경

            # 무일/계일 -> 임자시 시작
            TenStems.MU: TenStems.IM,         # 무 -> 임
            TenStems.GYE: TenStems.IM         # 계 -> 임
        }

        # 일간에 따른 시간 기준 결정
        hourly_base_stem = daily_stem_to_hourly_base.get(
            daily_ganji.stem,
            TenStems.GAHP
        )

        # 시간 계산 (자시부터 순차적으로)
        branch_position = list(TwelveBranches).index(time_unit.branch)
        hourly_stem_index = list(TenStems).index(hourly_base_stem) + branch_position
        hourly_stem = TenStems.get_by_index(hourly_stem_index)

        return GanJi(hourly_stem, time_unit.branch)

    @classmethod
    def calculate_saju(cls, birth_date_lunar: date, birth_time: time) -> dict:
        """완전한 사주(四柱) 계산 - 년월일시 네 기둥"""
        # 1. 년주(年柱) 계산 - 태어난 년도의 기운
        yearly_pillar = cls.calculate_yearly_ganji(birth_date_lunar)

        # 2. 월주(月柱) 계산 - 태어난 달의 기운
        monthly_pillar = cls.calculate_monthly_ganji(birth_date_lunar, yearly_pillar)

        # 3. 일주(日柱) 계산 - 태어난 날의 기운 (가장 중요)
        daily_pillar = cls.calculate_daily_ganji(birth_date_lunar)

        # 4. 시주(時柱) 계산 - 태어난 시각의 기운
        hourly_pillar = cls.calculate_hourly_ganji(birth_time, daily_pillar)

        # 사주 결과 반환 (한글 이름으로)
        return {
            'yearly_ganji': yearly_pillar.korean_name,   # 년주
            'monthly_ganji': monthly_pillar.korean_name, # 월주
            'daily_ganji': daily_pillar.korean_name,     # 일주 (일간 - 가장 중요)
            'hourly_ganji': hourly_pillar.korean_name    # 시주
        }