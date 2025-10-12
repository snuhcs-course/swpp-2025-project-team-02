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
    def get_by_index(cls, index: int) -> 'TenStems':
        """인덱스로 천간 찾기"""
        all_stems = list(cls)
        return all_stems[index % len(all_stems)]

    @classmethod
    def find(cls, name: str) -> 'TenStems':
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
    def get_by_index(cls, index: int) -> 'TwelveBranches':
        """인덱스로 지지 찾기"""
        all_branches = list(cls)
        return all_branches[index % len(all_branches)]

    @classmethod
    def find(cls, name: str) -> 'TwelveBranches':
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

        # Kotlin: LocalTime.of(hour, minute)..LocalTime.of(hour, minute)
        # Python: time(hour, minute) <= time_value < time(hour, minute)
        time_value = time_obj

        # 자시: 00:30 ~ 01:30
        if Time(0, 30) <= time_value < Time(1, 30):
            return cls.JA_SI
        # 축시: 01:30 ~ 03:30
        if Time(1, 30) <= time_value < Time(3, 30):
            return cls.CHUK_SI
        # 인시: 03:30 ~ 05:30
        if Time(3, 30) <= time_value < Time(5, 30):
            return cls.IN_SI
        # 묘시: 05:30 ~ 07:30
        if Time(5, 30) <= time_value < Time(7, 30):
            return cls.MYO_SI
        # 진시: 07:30 ~ 09:30
        if Time(7, 30) <= time_value < Time(9, 30):
            return cls.JIN_SI
        # 사시: 09:30 ~ 11:30
        if Time(9, 30) <= time_value < Time(11, 30):
            return cls.SA_SI
        # 오시: 11:30 ~ 13:30
        if Time(11, 30) <= time_value < Time(13, 30):
            return cls.O_SI
        # 미시: 13:30 ~ 15:30
        if Time(13, 30) <= time_value < Time(15, 30):
            return cls.MI_SI
        # 신시: 15:30 ~ 17:30
        if Time(15, 30) <= time_value < Time(17, 30):
            return cls.SIN_SI
        # 유시: 17:30 ~ 19:30
        if Time(17, 30) <= time_value < Time(19, 30):
            return cls.YU_SI
        # 술시: 19:30 ~ 21:30
        if Time(19, 30) <= time_value < Time(21, 30):
            return cls.SUL_SI
        # 해시: 21:30 ~ 23:30
        if Time(21, 30) <= time_value < Time(23, 30):
            return cls.HAE_SI
        # 야자시: 23:30 이후 또는 00:30 이전
        # Kotlin: time.isBefore(LocalTime.of(0, 30)) || time.isAfter(LocalTime.of(23, 30))
        if time_value < Time(0, 30) or time_value >= Time(23, 30):
            return cls.YA_JA_SI

        # 기본값
        return cls.JA_SI


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
        """날짜로 절기 찾기 (태양 황경 기준)"""
        current_longitude = cls.get_solar_longitude(date_value)

        # 절기 순서대로 확인 (입절 기준)
        # 입춘(315°)부터 시작해서 순환
        major_terms = [
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

        # 현재 황경이 어느 절기 구간에 속하는지 확인
        for index, (term, start_longitude) in enumerate(major_terms):
            next_index = (index + 1) % len(major_terms)
            next_longitude = major_terms[next_index][1]

            # 연말->연초 경계 처리 (소한 285° ~ 입춘 315°)
            if start_longitude > next_longitude:
                if current_longitude >= start_longitude or current_longitude < next_longitude:
                    return term
            else:
                if start_longitude <= current_longitude < next_longitude:
                    return term

        # 기본값 (도달하지 않아야 함)
        return cls.IPCHUN

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
                GanJi(TenStems.get_by_index(index % 10), TwelveBranches.get_by_index(index % 12))
                for index in range(60)
            ]
        return cls._cached

    @classmethod
    def get_by_index(cls, index: int) -> 'GanJi':
        """인덱스로 간지 찾기 (Kotlin: GanJi.idxAt)"""
        cached_ganji_list = cls._get_cached()
        return cached_ganji_list[index % len(cached_ganji_list)]

    @classmethod
    def find(cls, *args) -> 'GanJi':
        """간지 찾기 (Kotlin의 find 오버로딩)"""
        if len(args) == 1:
            # find(text: String)
            ganji_text = args[0]
            if len(ganji_text) == 2:
                return cls.find(ganji_text[0], ganji_text[1])
            raise ValueError(f"간지 이름은 2글자여야 합니다: {ganji_text}")
        elif len(args) == 2:
            if isinstance(args[0], str) and isinstance(args[1], str):
                # find(a: String, b: String)
                stem = TenStems.find(args[0])
                branch = TwelveBranches.find(args[1])
                return cls.find(stem, branch)
            elif isinstance(args[0], TenStems) and isinstance(args[1], TwelveBranches):
                # find(천간: 천간, 지지: 지지)
                target_stem, target_branch = args
                cached_ganji_list = cls._get_cached()
                for ganji in cached_ganji_list:
                    if ganji.stem == target_stem and ganji.branch == target_branch:
                        return ganji
                raise ValueError(f"간지를 찾을 수 없습니다: {target_stem.korean_name}{target_branch.korean_name}")
        raise ValueError("Invalid arguments for find()")

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
        """생년월일시로부터 사주 계산 (Kotlin: SaJu.from)"""
        yearly_pillar = cls._calculate_year_pillar(birth_date)
        monthly_pillar = cls._calculate_month_pillar(birth_date, yearly_pillar)
        daily_pillar = cls._calculate_day_pillar(birth_date)
        hourly_pillar = cls._calculate_hour_pillar(birth_time, daily_pillar)
        return Saju(yearly_pillar, monthly_pillar, daily_pillar, hourly_pillar)

    @staticmethod
    def _calculate_year_pillar(birth_date: date) -> GanJi:
        """년주 계산 (Kotlin: private fun year)"""
        years_from_1900 = birth_date.year - 1900
        base_ganji_1900 = GanJi.find("경자")
        return GanJi(
            base_ganji_1900.stem.next(years_from_1900),
            base_ganji_1900.branch.next(years_from_1900)
        )

    @staticmethod
    def _calculate_month_pillar(birth_date: date, yearly_pillar: GanJi) -> GanJi:
        """월주 계산 (Kotlin: private fun month) - 절기 기준"""
        # 태양 황경으로 정확한 절기 판정
        solar_term = SolarTerms.find_by_date(birth_date)

        # Kotlin when 문과 동일
        if yearly_pillar.stem in (TenStems.GAHP, TenStems.GI):
            base_month_stem = TenStems.BYUNG
        elif yearly_pillar.stem in (TenStems.EUL, TenStems.GYUNG):
            base_month_stem = TenStems.MU
        elif yearly_pillar.stem in (TenStems.BYUNG, TenStems.SIN):
            base_month_stem = TenStems.GYUNG
        elif yearly_pillar.stem in (TenStems.JUNG, TenStems.IM):
            base_month_stem = TenStems.IM
        elif yearly_pillar.stem in (TenStems.MU, TenStems.GYE):
            base_month_stem = TenStems.GAHP
        else:
            base_month_stem = TenStems.GAHP

        return GanJi(
            base_month_stem.next(solar_term.month - 1),
            solar_term.branch
        )

    @staticmethod
    def _calculate_day_pillar(birth_date: date) -> GanJi:
        """일주 계산 (Kotlin: private fun daily)"""
        reference_date_1925 = date(1925, 2, 9)
        days_from_reference = (birth_date - reference_date_1925).days
        return GanJi.get_by_index(days_from_reference)

    @staticmethod
    def _calculate_hour_pillar(birth_time: time, daily_pillar: GanJi) -> GanJi:
        """시주 계산 (Kotlin: private fun hourly)"""
        birth_time_unit = TimeUnits.from_time(birth_time)

        # Kotlin when 문과 동일 (line 67-73)
        # 갑일/기일 -> 갑자시 시작
        if daily_pillar.stem in (TenStems.GAHP, TenStems.GI):
            base_hour_stem = TenStems.GAHP
        # 을일/경일 -> 병자시 시작
        elif daily_pillar.stem in (TenStems.EUL, TenStems.GYUNG):
            base_hour_stem = TenStems.BYUNG
        # 병일/신일 -> 무자시 시작
        elif daily_pillar.stem in (TenStems.BYUNG, TenStems.SIN):
            base_hour_stem = TenStems.MU
        # 정일/임일 -> 경자시 시작
        elif daily_pillar.stem in (TenStems.JUNG, TenStems.IM):
            base_hour_stem = TenStems.GYUNG
        # 무일/계일 -> 임자시 시작
        elif daily_pillar.stem in (TenStems.MU, TenStems.GYE):
            base_hour_stem = TenStems.IM
        else:
            base_hour_stem = TenStems.GAHP

        # Kotlin: baseHeaven.next(십이시.ordinal % 12)
        time_unit_index = list(TimeUnits).index(birth_time_unit)

        return GanJi(
            base_hour_stem.next(time_unit_index % 12),
            TwelveBranches.of(birth_time_unit)
        )

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