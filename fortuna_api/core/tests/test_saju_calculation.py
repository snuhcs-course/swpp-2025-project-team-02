"""
Tests for Saju (사주) calculation logic.
"""
from datetime import date, time
from django.test import TestCase
from core.utils.saju_concepts import Saju, SajuCalculator
from user.models import User


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

    def test_saju_2000_03_17_14_00(self):
        """
        Test case 3: 양력 2000년 3월 17일 14시 00분 (미시)
        Expected:
        - 연주 (Yearly): 경진
        - 월주 (Monthly): 기묘
        - 일주 (Daily): 갑술
        - 시주 (Hourly): 신미
        """
        birth_date = date(2000, 3, 17)
        birth_time = time(14, 0)

        saju = Saju.from_date(birth_date, birth_time)

        self.assertEqual(saju.yearly.two_letters, "경진",
                        f"연주가 '경진'이어야 하는데 '{saju.yearly.two_letters}'입니다.")
        self.assertEqual(saju.monthly.two_letters, "기묘",
                        f"월주가 '기묘'이어야 하는데 '{saju.monthly.two_letters}'입니다.")
        self.assertEqual(saju.daily.two_letters, "갑술",
                        f"일주가 '갑술'이어야 하는데 '{saju.daily.two_letters}'입니다.")
        self.assertEqual(saju.hourly.two_letters, "신미",
                        f"시주가 '신미'이어야 하는데 '{saju.hourly.two_letters}'입니다.")

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


class SajuCalendarConversionTestCase(TestCase):
    """양력/음력 변환 및 사주 계산 테스트"""

    def test_solar_birth_date_saju_calculation(self):
        """
        양력 생년월일 입력 시 사주 계산 검증

        양력 2000년 3월 17일 → 음력 2000년 2월 12일 변환 후
        양력 기준으로 사주 계산
        """
        user = User.objects.create(
            email="solar_test@example.com",
            username="solar_test_user"
        )

        # 양력 생년월일로 입력
        solar_date = date(2000, 3, 17)
        user.set_birth_date_and_calculate_saju(solar_date, 'solar', '미시')
        user.save()

        # 양력/음력 변환 확인
        self.assertEqual(user.birth_date_solar, date(2000, 3, 17))
        self.assertIsNotNone(user.birth_date_lunar)

        # 양력 기준으로 사주가 계산되었는지 확인
        self.assertIsNotNone(user.yearly_ganji)
        self.assertIsNotNone(user.monthly_ganji)
        self.assertIsNotNone(user.daily_ganji)
        self.assertIsNotNone(user.hourly_ganji)

        # 실제 양력 날짜로 직접 계산한 값과 비교
        expected_saju = SajuCalculator.calculate_saju(
            user.birth_date_solar,
            time(12, 0)  # 기본 시간
        )

        self.assertEqual(user.yearly_ganji, expected_saju['yearly_ganji'])
        self.assertEqual(user.monthly_ganji, expected_saju['monthly_ganji'])
        self.assertEqual(user.daily_ganji, expected_saju['daily_ganji'])
        self.assertEqual(user.hourly_ganji, expected_saju['hourly_ganji'])

    def test_lunar_birth_date_saju_calculation(self):
        """
        음력 생년월일 입력 시 사주 계산 검증

        음력 2000년 2월 12일 직접 입력 → 양력 2000년 3월 17일 변환 후
        양력 기준으로 사주 계산
        """
        user = User.objects.create(
            email="lunar_test@example.com",
            username="lunar_test_user"
        )

        # 음력 생년월일로 입력
        lunar_date = date(2000, 2, 12)
        user.set_birth_date_and_calculate_saju(lunar_date, 'lunar', '미시')
        user.save()

        # 양력/음력 변환 확인
        self.assertEqual(user.birth_date_lunar, date(2000, 2, 12))
        self.assertIsNotNone(user.birth_date_solar)

        # 양력 기준으로 사주가 계산되었는지 확인
        self.assertIsNotNone(user.yearly_ganji)
        self.assertIsNotNone(user.monthly_ganji)
        self.assertIsNotNone(user.daily_ganji)
        self.assertIsNotNone(user.hourly_ganji)

        # 양력 날짜로 직접 계산한 값과 비교
        expected_saju = SajuCalculator.calculate_saju(
            user.birth_date_solar,
            time(12, 0)
        )

        self.assertEqual(user.yearly_ganji, expected_saju['yearly_ganji'])
        self.assertEqual(user.monthly_ganji, expected_saju['monthly_ganji'])
        self.assertEqual(user.daily_ganji, expected_saju['daily_ganji'])
        self.assertEqual(user.hourly_ganji, expected_saju['hourly_ganji'])

    def test_solar_vs_lunar_saju_difference(self):
        """
        양력과 음력으로 같은 날짜를 입력했을 때 사주가 다르게 계산되는지 검증

        같은 날짜(2000년 3월 17일)를 양력/음력으로 각각 입력하면
        양력 변환 후 최종 양력 날짜가 달라지고, 사주 계산도 달라야 함

        - 양력 2000.03.17 입력 → 양력 2000.03.17로 사주 계산
        - 음력 2000.03.17 입력 → 양력 2000.04.21로 변환 후 사주 계산
        """
        # 양력 2000년 3월 17일로 입력
        user_solar = User.objects.create(
            email="solar_user@example.com",
            username="solar_user"
        )
        user_solar.set_birth_date_and_calculate_saju(date(2000, 3, 17), 'solar', '미시')
        user_solar.save()

        # 음력 2000년 3월 17일로 입력
        user_lunar = User.objects.create(
            email="lunar_user@example.com",
            username="lunar_user"
        )
        user_lunar.set_birth_date_and_calculate_saju(date(2000, 3, 17), 'lunar', '미시')
        user_lunar.save()

        # 두 사용자의 최종 양력 날짜는 달라야 함
        self.assertNotEqual(user_solar.birth_date_solar, user_lunar.birth_date_solar)

        # 양력 날짜가 다르므로 사주도 달라야 함 (적어도 하나는 달라야 함)
        saju_fields_different = (
            user_solar.yearly_ganji != user_lunar.yearly_ganji or
            user_solar.monthly_ganji != user_lunar.monthly_ganji or
            user_solar.daily_ganji != user_lunar.daily_ganji or
            user_solar.hourly_ganji != user_lunar.hourly_ganji
        )
        self.assertTrue(saju_fields_different,
                       "양력/음력 입력에 따라 최종 사주가 달라야 합니다")

    def test_solar_to_lunar_conversion_accuracy(self):
        """
        양력 → 음력 변환의 정확성 검증

        양력 2000년 3월 17일 = 음력 2000년 2월 12일
        """
        solar_date = date(2000, 3, 17)
        lunar_date = SajuCalculator.solar_to_lunar(solar_date)

        # 정확한 음력 날짜로 변환되었는지 확인
        self.assertEqual(lunar_date.year, 2000)
        self.assertEqual(lunar_date.month, 2)
        self.assertEqual(lunar_date.day, 12)
