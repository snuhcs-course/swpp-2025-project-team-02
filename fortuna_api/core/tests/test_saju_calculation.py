"""
Tests for Saju (사주) calculation logic.
"""
from datetime import date, time
from django.test import TestCase
from core.utils.saju_concepts import Saju, SajuCalculator


class SajuCalculationTestCase(TestCase):
    """Test cases for Saju calculation"""

    def test_saju_2000_03_17_10_25(self):
        """
        Test case 1: 양력 2000년 3월 17일 10시 25분
        Expected:
        - 연주 (Yearly): 경진
        - 월주 (Monthly): 기묘
        - 일주 (Daily): 갑술
        - 시주 (Hourly): 기사
        """
        birth_date = date(2000, 3, 17)
        birth_time = time(10, 25)

        saju = Saju.from_date(birth_date, birth_time)

        self.assertEqual(saju.yearly.two_letters, "경진",
                        f"연주가 '경진'이어야 하는데 '{saju.yearly.two_letters}'입니다.")
        self.assertEqual(saju.monthly.two_letters, "기묘",
                        f"월주가 '기묘'이어야 하는데 '{saju.monthly.two_letters}'입니다.")
        self.assertEqual(saju.daily.two_letters, "갑술",
                        f"일주가 '갑술'이어야 하는데 '{saju.daily.two_letters}'입니다.")
        self.assertEqual(saju.hourly.two_letters, "기사",
                        f"시주가 '기사'이어야 하는데 '{saju.hourly.two_letters}'입니다.")

    def test_saju_2001_09_21_19_55(self):
        """
        Test case 2: 양력 2001년 9월 21일 19시 55분
        Expected:
        - 연주 (Yearly): 신사
        - 월주 (Monthly): 정유
        - 일주 (Daily): 정해
        - 시주 (Hourly): 경술
        """
        birth_date = date(2001, 9, 21)
        birth_time = time(19, 55)

        saju = Saju.from_date(birth_date, birth_time)

        self.assertEqual(saju.yearly.two_letters, "신사",
                        f"연주가 '신사'이어야 하는데 '{saju.yearly.two_letters}'입니다.")
        self.assertEqual(saju.monthly.two_letters, "정유",
                        f"월주가 '정유'이어야 하는데 '{saju.monthly.two_letters}'입니다.")
        self.assertEqual(saju.daily.two_letters, "정해",
                        f"일주가 '정해'이어야 하는데 '{saju.daily.two_letters}'입니다.")
        self.assertEqual(saju.hourly.two_letters, "경술",
                        f"시주가 '경술'이어야 하는데 '{saju.hourly.two_letters}'입니다.")

    def test_saju_calculator_compatibility(self):
        """
        Test SajuCalculator (backward compatibility) with test case 1
        """
        birth_date = date(2000, 3, 17)
        birth_time = time(10, 25)

        result = SajuCalculator.calculate_saju(birth_date, birth_time)

        self.assertEqual(result['yearly_ganji'], "경진")
        self.assertEqual(result['monthly_ganji'], "기묘")
        self.assertEqual(result['daily_ganji'], "갑술")
        self.assertEqual(result['hourly_ganji'], "기사")
