"""
Korean Saju (四柱) concepts and calculations.
Direct port from Kotlin SajuConcepts.kt
"""
from enum import Enum
from datetime import date, time, datetime
from typing import Optional
from astronomy import Time, SunPosition
from lunarcalendar import Converter, Solar, Lunar
from loguru import logger


class YinYang(Enum):
    """음양 (Yin-Yang)"""
    YANG = "양"
    YIN = "음"


class FiveElements(Enum):
    """오행 (Five Elements)"""
    WOOD = ("목", "나무", "푸른")
    FIRE = ("화", "불", "붉은")
    EARTH = ("토", "흙", "노란")
    METAL = ("금", "철", "흰")
    WATER = ("수", "물", "검은")

    def __init__(self, chinese: str, easy_korean: str, color: str):
        self.chinese = chinese
        self.easy_korean = easy_korean
        self.color = color

    def empowers(self, other: 'FiveElements') -> bool:
        """상생 관계 (empowers)"""
        relations = {
            FiveElements.WOOD: FiveElements.FIRE,
            FiveElements.FIRE: FiveElements.EARTH,
            FiveElements.EARTH: FiveElements.METAL,
            FiveElements.METAL: FiveElements.WATER,
            FiveElements.WATER: FiveElements.WOOD,
        }
        return relations[self] == other

    def weakens(self, other: 'FiveElements') -> bool:
        """상극 관계 (weakens)"""
        relations = {
            FiveElements.WOOD: FiveElements.EARTH,
            FiveElements.FIRE: FiveElements.METAL,
            FiveElements.EARTH: FiveElements.WATER,
            FiveElements.METAL: FiveElements.WOOD,
            FiveElements.WATER: FiveElements.FIRE,
        }
        return relations[self] == other


class TenStems(Enum):
    """천간 (Ten Heavenly Stems)"""
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

    def next(self, offset: int) -> 'TenStems':
        """다음 천간 (offset만큼 이동)"""
        stems = list(TenStems)
        current_index = stems.index(self)
        return stems[(current_index + offset) % len(stems)]

    @classmethod
    def find_by_index(cls, index: int) -> 'TenStems':
        """인덱스로 천간 찾기"""
        all_stems = list(cls)
        return all_stems[index % len(all_stems)]

    @classmethod
    def find_by_name(cls, name: str) -> 'TenStems':
        """한글 이름으로 천간 찾기"""
        for stem in cls:
            if stem.korean_name == name:
                return stem
        raise ValueError(f"천간을 찾을 수 없습니다: {name}")


class TwelveBranches(Enum):
    """지지 (Twelve Earthly Branches)"""
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

    def next(self, offset: int) -> 'TwelveBranches':
        """다음 지지 (offset만큼 이동)"""
        branches = list(TwelveBranches)
        current_index = branches.index(self)
        return branches[(current_index + offset) % len(branches)]

    @classmethod
    def find_by_index(cls, index: int) -> 'TwelveBranches':
        """인덱스로 지지 찾기"""
        all_branches = list(cls)
        return all_branches[index % len(all_branches)]

    @classmethod
    def find_by_name(cls, name: str) -> 'TwelveBranches':
        """한글 이름으로 지지 찾기"""
        for branch in cls:
            if branch.korean_name == name:
                return branch
        raise ValueError(f"지지를 찾을 수 없습니다: {name}")

    @classmethod
    def of(cls, time_unit: 'TimeUnits') -> 'TwelveBranches':
        """십이시로부터 지지 찾기"""
        mapping = {
            TimeUnits.JA_SI: cls.JA,
            TimeUnits.CHUK_SI: cls.CHUK,
            TimeUnits.IN_SI: cls.IN,
            TimeUnits.MYO_SI: cls.MYO,
            TimeUnits.JIN_SI: cls.JIN,
            TimeUnits.SA_SI: cls.SA,
            TimeUnits.O_SI: cls.O,
            TimeUnits.MI_SI: cls.MI,
            TimeUnits.SIN_SI: cls.SIN,
            TimeUnits.YU_SI: cls.YU,
            TimeUnits.SUL_SI: cls.SUL,
            TimeUnits.HAE_SI: cls.HAE,
            TimeUnits.YA_JA_SI: cls.HAE,  # Kotlin: 야자시 -> 해
        }
        return mapping.get(time_unit, cls.JA)


class TimeUnits(Enum):
    """십이시 (Traditional Korean Time Units)"""
    JA_SI = "자시"
    CHUK_SI = "축시"
    IN_SI = "인시"
    MYO_SI = "묘시"
    JIN_SI = "진시"
    SA_SI = "사시"
    O_SI = "오시"
    MI_SI = "미시"
    SIN_SI = "신시"
    YU_SI = "유시"
    SUL_SI = "술시"
    HAE_SI = "해시"
    YA_JA_SI = "야자시"

    @classmethod
    def from_time(cls, time_obj: time) -> 'TimeUnits':
        """시간으로부터 십이시 찾기 (Kotlin 로직과 동일)"""
        from datetime import time as Time

        # 시간 범위 매핑 테이블
        time_ranges = [
            (Time(0, 30), Time(1, 30), cls.JA_SI),
            (Time(1, 30), Time(3, 30), cls.CHUK_SI),
            (Time(3, 30), Time(5, 30), cls.IN_SI),
            (Time(5, 30), Time(7, 30), cls.MYO_SI),
            (Time(7, 30), Time(9, 30), cls.JIN_SI),
            (Time(9, 30), Time(11, 30), cls.SA_SI),
            (Time(11, 30), Time(13, 30), cls.O_SI),
            (Time(13, 30), Time(15, 30), cls.MI_SI),
            (Time(15, 30), Time(17, 30), cls.SIN_SI),
            (Time(17, 30), Time(19, 30), cls.YU_SI),
            (Time(19, 30), Time(21, 30), cls.SUL_SI),
            (Time(21, 30), Time(23, 30), cls.HAE_SI),
        ]

        # 일반 시간대 확인
        for start_time, end_time, time_unit in time_ranges:
            if cls._is_time_in_range(time_obj, start_time, end_time):
                return time_unit

        # 야자시: 23:30 이후 또는 00:30 이전
        if cls._is_ya_ja_si(time_obj):
            return cls.YA_JA_SI

        # 기본값
        return cls.JA_SI

    @staticmethod
    def _is_time_in_range(time_value: time, start: time, end: time) -> bool:
        """주어진 시간이 범위 내에 있는지 확인"""
        return start <= time_value < end

    @staticmethod
    def _is_ya_ja_si(time_value: time) -> bool:
        """야자시인지 확인 (23:30 이후 또는 00:30 이전)"""
        from datetime import time as Time
        return time_value < Time(0, 30) or time_value >= Time(23, 30)


class SolarTerms(Enum):
    """절기 (24 Solar Terms) - based on solar longitude"""
    # 절기명, 태양황경, 월지 순서
    # 입절(立節)을 기준으로 월지가 바뀜
    IPCHUN = ("입춘", 315, 1, TwelveBranches.IN)      # 315° = 입춘 (인월 시작)
    USU = ("우수", 330, 1, TwelveBranches.IN)
    GYEONGCHIP = ("경칩", 345, 2, TwelveBranches.MYO)  # 345° = 경칩 (묘월 시작)
    CHUNBUN = ("춘분", 0, 2, TwelveBranches.MYO)
    CHEONGMYEONG = ("청명", 15, 3, TwelveBranches.JIN)  # 15° = 청명 (진월 시작)
    GOGU = ("곡우", 30, 3, TwelveBranches.JIN)
    IPHA = ("입하", 45, 4, TwelveBranches.SA)          # 45° = 입하 (사월 시작)
    SOMAN = ("소만", 60, 4, TwelveBranches.SA)
    MANGJONG = ("망종", 75, 5, TwelveBranches.O)       # 75° = 망종 (오월 시작)
    HAJI = ("하지", 90, 5, TwelveBranches.O)
    SOSEO = ("소서", 105, 6, TwelveBranches.MI)        # 105° = 소서 (미월 시작)
    DAESEO = ("대서", 120, 6, TwelveBranches.MI)
    IPCHU = ("입추", 135, 7, TwelveBranches.SIN)       # 135° = 입추 (신월 시작)
    CHEOSEO = ("처서", 150, 7, TwelveBranches.SIN)
    BAENGNO = ("백로", 165, 8, TwelveBranches.YU)      # 165° = 백로 (유월 시작)
    CHUBUN = ("추분", 180, 8, TwelveBranches.YU)
    HANNO = ("한로", 195, 9, TwelveBranches.SUL)       # 195° = 한로 (술월 시작)
    SANGGANG = ("상강", 210, 9, TwelveBranches.SUL)
    IPDONG = ("입동", 225, 10, TwelveBranches.HAE)     # 225° = 입동 (해월 시작)
    SOSEOL = ("소설", 240, 10, TwelveBranches.HAE)
    DAESEOL = ("대설", 255, 11, TwelveBranches.JA)     # 255° = 대설 (자월 시작)
    DONGJI = ("동지", 270, 11, TwelveBranches.JA)
    SOHAN = ("소한", 285, 12, TwelveBranches.CHUK)     # 285° = 소한 (축월 시작)
    DAEHAN = ("대한", 300, 12, TwelveBranches.CHUK)

    def __init__(self, korean_name: str, longitude: int, month: int, branch: TwelveBranches):
        self.korean_name = korean_name
        self.longitude = longitude  # 태양 황경 (degrees)
        self.month = month
        self.branch = branch

    @classmethod
    def get_solar_longitude(cls, date_value: date) -> float:
        """특정 날짜의 태양 황경 계산 (astronomy-engine 사용)"""
        # datetime으로 변환 (정오 기준)
        astro_time = Time.Make(date_value.year, date_value.month, date_value.day, 12, 0, 0)

        # 태양 위치 계산
        sun_position = SunPosition(astro_time)

        # 황경 반환 (ecliptic longitude)
        ecliptic_longitude = sun_position.elon
        return ecliptic_longitude

    @classmethod
    def find_by_date(cls, date_value: date) -> 'SolarTerms':
        """
        날짜로 절기 찾기 (태양 황경 기준)

        천문학적으로 정확한 절기 계산: 태양의 황경(ecliptic longitude)을 계산하여
        24절기 중 어느 구간에 속하는지 판단
        """
        # 현재 날짜의 태양 황경 계산
        current_longitude = cls.get_solar_longitude(date_value)

        # 주요 절기 목록 (입절 기준 12개)
        major_terms = cls._get_major_solar_terms()

        # 현재 황경이 어느 절기 구간에 속하는지 확인
        for index in range(len(major_terms)):
            term = cls._find_solar_term_in_range(current_longitude, major_terms, index)
            if term:
                return term

        # 기본값 (정상적으로는 도달하지 않음)
        return cls.IPCHUN

    @classmethod
    def _get_major_solar_terms(cls) -> list:
        """주요 절기 목록 반환 (입절 기준)"""
        return [
            (cls.IPCHUN, 315),      # 인월
            (cls.GYEONGCHIP, 345),  # 묘월
            (cls.CHEONGMYEONG, 15), # 진월
            (cls.IPHA, 45),         # 사월
            (cls.MANGJONG, 75),     # 오월
            (cls.SOSEO, 105),       # 미월
            (cls.IPCHU, 135),       # 신월
            (cls.BAENGNO, 165),     # 유월
            (cls.HANNO, 195),       # 술월
            (cls.IPDONG, 225),      # 해월
            (cls.DAESEOL, 255),     # 자월
            (cls.SOHAN, 285),       # 축월
        ]

    @staticmethod
    def _find_solar_term_in_range(current_longitude: float, major_terms: list, index: int):
        """
        주어진 인덱스의 절기 범위에 황경이 속하는지 확인

        연말-연초 경계를 고려한 범위 체크
        예: 소한(285°) ~ 입춘(315°) 구간은 285° ~ 360° 또는 0° ~ 315° 범위
        """
        term, start_longitude = major_terms[index]
        next_index = (index + 1) % len(major_terms)
        next_longitude = major_terms[next_index][1]

        # 연말->연초 경계 처리 (start > next인 경우)
        if start_longitude > next_longitude:
            if current_longitude >= start_longitude or current_longitude < next_longitude:
                return term
        else:
            # 일반적인 범위 체크
            if start_longitude <= current_longitude < next_longitude:
                return term
        return None

    @classmethod
    def find_by_month(cls, month: int) -> 'SolarTerms':
        """월로 절기 찾기 (하위 호환성 - deprecated)"""
        for term in cls:
            if term.month == month:
                return term
        raise ValueError(f"절기를 찾을 수 없습니다: {month}")


class GanJi:
    """간지 (Sexagenary Cycle)"""

    # 60갑자 캐시 (Kotlin의 cached와 동일)
    _cached = None

    def __init__(self, stem: TenStems, branch: TwelveBranches):
        self.stem = stem
        self.branch = branch
        self.two_letters = stem.korean_name + branch.korean_name

    @classmethod
    def _get_cached(cls):
        """60갑자 캐시 생성 (Kotlin의 cached 로직)"""
        if cls._cached is None:
            cls._cached = [
                GanJi(TenStems.find_by_index(index % 10), TwelveBranches.find_by_index(index % 12))
                for index in range(60)
            ]
        return cls._cached

    @classmethod
    def find_by_index(cls, index: int) -> 'GanJi':
        """인덱스로 간지 찾기 (Kotlin: GanJi.idxAt)"""
        cached_ganji_list = cls._get_cached()
        return cached_ganji_list[index % len(cached_ganji_list)]

    @classmethod
    def find_by_name(cls, *args) -> 'GanJi':
        """간지 찾기 (Kotlin의 find 오버로딩)"""
        if len(args) == 1:
            # find_by_name(text: String)
            ganji_text = args[0]
            if len(ganji_text) == 2:
                return cls.find_by_name(ganji_text[0], ganji_text[1])
            raise ValueError(f"간지 이름은 2글자여야 합니다: {ganji_text}")
        elif len(args) == 2:
            if isinstance(args[0], str) and isinstance(args[1], str):
                # find_by_name(a: String, b: String)
                stem = TenStems.find_by_name(args[0])
                branch = TwelveBranches.find_by_name(args[1])
                return cls.find_by_name(stem, branch)
            elif isinstance(args[0], TenStems) and isinstance(args[1], TwelveBranches):
                # find_by_name(천간: 천간, 지지: 지지)
                target_stem, target_branch = args
                cached_ganji_list = cls._get_cached()
                for ganji in cached_ganji_list:
                    if ganji.stem == target_stem and ganji.branch == target_branch:
                        return ganji
                raise ValueError(f"간지를 찾을 수 없습니다: {target_stem.korean_name}{target_branch.korean_name}")
        raise ValueError("Invalid arguments for find_by_name()")

    def __str__(self):
        return self.two_letters

    def __eq__(self, other):
        if isinstance(other, GanJi):
            return self.stem == other.stem and self.branch == other.branch
        return False


class Saju:
    """사주 (Four Pillars)"""

    def __init__(self, yearly: GanJi, monthly: GanJi, daily: GanJi, hourly: GanJi):
        self.yearly = yearly
        self.monthly = monthly
        self.daily = daily
        self.hourly = hourly

    @classmethod
    def from_date(cls, birth_date: date, birth_time: time) -> 'Saju':
        """
        생년월일시로부터 사주팔자 계산

        년주, 월주, 일주, 시주를 순차적으로 계산하여 사주 객체 생성
        월주는 년주에 의존, 시주는 일주에 의존하므로 계산 순서 중요
        """
        yearly_pillar = cls._calculate_year_pillar(birth_date)
        monthly_pillar = cls._calculate_month_pillar(birth_date, yearly_pillar)
        daily_pillar = cls._calculate_day_pillar(birth_date)
        hourly_pillar = cls._calculate_hour_pillar(birth_time, daily_pillar)
        return Saju(yearly_pillar, monthly_pillar, daily_pillar, hourly_pillar)

    @staticmethod
    def _calculate_year_pillar(birth_date: date) -> GanJi:
        """
        년주 계산

        1900년(경자년)을 기준으로 경과 연수만큼 간지를 이동시켜 계산
        """
        years_from_1900 = birth_date.year - 1900
        base_ganji_1900 = GanJi.find_by_name("경자")  # 1900년 = 경자년
        return GanJi(
            base_ganji_1900.stem.next(years_from_1900),
            base_ganji_1900.branch.next(years_from_1900)
        )

    @staticmethod
    def _calculate_month_pillar(birth_date: date, yearly_pillar: GanJi) -> GanJi:
        """
        월주 계산 (절기 기준)

        양력 날짜로부터 태양 황경을 계산하여 절기(24절기) 판단
        절기에 따라 월지(月支)가 결정되고, 년간에 따라 월간(月干) 결정
        """
        # 태양 황경으로 현재 절기 판단
        solar_term = SolarTerms.find_by_date(birth_date)

        # 년간으로부터 월간의 시작점 계산
        base_month_stem = Saju._get_base_month_stem(yearly_pillar.stem)

        return GanJi(
            base_month_stem.next(solar_term.month - 1),
            solar_term.branch
        )

    @staticmethod
    def _get_base_stem_from_mapping(source_stem: TenStems, stem_mapping: dict) -> TenStems:
        """
        천간 매핑 테이블에서 기준 천간을 찾는 공통 로직

        Args:
            source_stem: 조회할 천간
            stem_mapping: (천간 튜플) -> 기준 천간 매핑 딕셔너리

        Returns:
            매핑된 기준 천간 (없으면 갑(GAHP) 반환)
        """
        for stems_tuple, base_stem in stem_mapping.items():
            if source_stem in stems_tuple:
                return base_stem
        return TenStems.GAHP

    @staticmethod
    def _get_base_month_stem(year_stem: TenStems) -> TenStems:
        """
        년간으로부터 월간의 기준 천간 계산

        사주에서 년간에 따라 월간의 시작 천간이 결정됨
        예: 갑년/기년 -> 정월이 병인(丙寅)월로 시작
        """
        stem_mapping = {
            (TenStems.GAHP, TenStems.GI): TenStems.BYUNG,
            (TenStems.EUL, TenStems.GYUNG): TenStems.MU,
            (TenStems.BYUNG, TenStems.SIN): TenStems.GYUNG,
            (TenStems.JUNG, TenStems.IM): TenStems.IM,
            (TenStems.MU, TenStems.GYE): TenStems.GAHP,
        }
        return Saju._get_base_stem_from_mapping(year_stem, stem_mapping)

    @staticmethod
    def _calculate_day_pillar(birth_date: date) -> GanJi:
        """
        일주 계산

        1925년 2월 9일(갑자일)을 기준으로 경과 일수를 60갑자로 변환
        """
        reference_date_1925 = date(1925, 2, 9)  # 갑자일
        days_from_reference = (birth_date - reference_date_1925).days
        return GanJi.find_by_index(days_from_reference)

    @staticmethod
    def _calculate_hour_pillar(birth_time: time, daily_pillar: GanJi) -> GanJi:
        """
        시주 계산

        일간에 따라 시간의 시작 천간이 결정되고, 시진(십이시)으로 지지 결정
        """
        birth_time_unit = TimeUnits.from_time(birth_time)
        base_hour_stem = Saju._get_base_hour_stem(daily_pillar.stem)
        time_unit_index = list(TimeUnits).index(birth_time_unit)

        return GanJi(
            base_hour_stem.next(time_unit_index % 12),
            TwelveBranches.of(birth_time_unit)
        )

    @staticmethod
    def _get_base_hour_stem(day_stem: TenStems) -> TenStems:
        """
        일간으로부터 시간의 기준 천간 계산

        사주에서 일간에 따라 자시(子時)의 천간이 결정됨
        예: 갑일/기일 -> 자시가 갑자(甲子)시로 시작
        """
        stem_mapping = {
            (TenStems.GAHP, TenStems.GI): TenStems.GAHP,
            (TenStems.EUL, TenStems.GYUNG): TenStems.BYUNG,
            (TenStems.BYUNG, TenStems.SIN): TenStems.MU,
            (TenStems.JUNG, TenStems.IM): TenStems.GYUNG,
            (TenStems.MU, TenStems.GYE): TenStems.IM,
        }
        return Saju._get_base_stem_from_mapping(day_stem, stem_mapping)

    def to_dict(self) -> dict:
        """사주를 딕셔너리로 변환"""
        return {
            'yearly_ganji': self.yearly.two_letters,
            'monthly_ganji': self.monthly.two_letters,
            'daily_ganji': self.daily.two_letters,
            'hourly_ganji': self.hourly.two_letters
        }


# 하위 호환성을 위한 SajuCalculator (기존 API 유지)
class SajuCalculator:
    """사주 계산기 (Backward compatibility)"""

    @staticmethod
    def calculate_saju(birth_date: date, birth_time: time) -> dict:
        """사주 계산 (기존 API 호환)"""
        saju = Saju.from_date(birth_date, birth_time)
        return saju.to_dict()
    
    @staticmethod
    def solar_to_lunar(solar_date: date) -> date:
        """Convert solar date to lunar date"""
        try:
            solar_calendar_date = Solar(solar_date.year, solar_date.month, solar_date.day)
            lunar_calendar_date = Converter.Solar2Lunar(solar_calendar_date)
            return date(lunar_calendar_date.year, lunar_calendar_date.month, lunar_calendar_date.day)
        except Exception as error:
            # If conversion fails, return original date
            logger.error(f"Solar to lunar conversion failed: {solar_date}, error: {error}")
            return solar_date

    @staticmethod
    def lunar_to_solar(lunar_date: date) -> date:
        """Convert lunar date to solar date"""
        try:
            lunar_calendar_date = Lunar(lunar_date.year, lunar_date.month, lunar_date.day)
            solar_calendar_date = Converter.Lunar2Solar(lunar_calendar_date)
            return date(solar_calendar_date.year, solar_calendar_date.month, solar_calendar_date.day)
        except Exception as error:
            # If conversion fails, return original date
            logger.error(f"Lunar to solar conversion failed: {lunar_date}, error: {error}")
            return lunar_date


if __name__ == "__main__":
    # 테스트 1: 2000년 3월 17일 14시
    result1 = SajuCalculator.calculate_saju(date(2000, 3, 17), time(14, 0))
    print("Test 1:", result1)