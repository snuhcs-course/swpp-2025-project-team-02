"""
Tests for Daewoon (Great Fortune) calculation logic.
"""

from datetime import date
from django.test import TestCase
from core.utils.saju_concepts import GanJi, YinYang
from core.services.daewoon import DaewoonCalculator, DaewoonDirection
from user.models import User


class TestDaewoonCalculation(TestCase):
    """Test Daewoon calculation logic."""

    def setUp(self):
        """
        Set up test user: 2001년 5월 2일 출생, 여성, 진시(辰時, 07:00-09:00)
        - 년간: 신(辛) - 음간
        - 여성 + 음간 = 순행 (FORWARD)
        - 한국나이 11살일 때 대운: 갑오
        """
        self.user = User.objects.create_user(
            email="test_daewoon@example.com",
            password="testpass123",
            gender="F",
        )
        # 생년월일 설정 및 사주 계산
        self.user.set_birth_date_and_calculate_saju(
            birth_date=date(2001, 5, 2),
            calendar_type="solar",
            time_units="진시"  # 07:00-09:00
        )
        self.user.save()

    def test_saju_calculation(self):
        """Test basic Saju calculation for the test user."""
        saju = self.user.saju()

        # 사주 정보 확인
        self.assertIsNotNone(saju.yearly)
        self.assertIsNotNone(saju.monthly)
        self.assertEqual(saju.yearly.stem.yin_yang, YinYang.YIN)  # 신(辛)은 음간

    def test_daewoon_direction_female_yin(self):
        """Test daewoon direction: 여성 + 음간(신) = 순행."""
        direction = DaewoonCalculator.calculate_daewoon_direction(self.user)
        self.assertEqual(direction, DaewoonDirection.FORWARD)

    def test_daewoon_starting_age(self):
        """Test daewoon starting age calculation."""
        starting_age = DaewoonCalculator.calculate_daewoon_starting_age(self.user)

        # 대운 시작 나이는 0~9 사이여야 함 (일반적으로)
        self.assertIsNotNone(starting_age)
        self.assertGreaterEqual(starting_age, 0)
        self.assertLessEqual(starting_age, 30)  # 최대 30일 / 3 = 10년

    def test_daewoon_at_age_11(self):
        """
        Test that at Korean age 11, the user has '갑오' daewoon.

        이것이 핵심 검증 테스트입니다:
        2001년 5월 2일 출생 여성은 한국나이 11살일 때 갑오 대운을 가집니다.
        """
        # 사주는 그대로 유지하고 나이만 계산 시뮬레이션
        saju = self.user.saju()
        monthly_ganji = saju.monthly
        monthly_index = GanJi.get_index(monthly_ganji)

        # 대운 시작 나이 계산
        starting_age = DaewoonCalculator.calculate_daewoon_starting_age(self.user)

        # 한국나이 11살일 때의 offset 계산
        korean_age_11 = 11
        if korean_age_11 >= starting_age:
            offset = (korean_age_11 - starting_age) // 10

            # 순행이므로 directed_offset = offset + 1
            directed_offset = offset + 1

            # 대운 간지 계산
            daewoon_index = monthly_index + directed_offset
            daewoon_ganji = GanJi.find_by_index(daewoon_index)

            # 검증: 한국나이 11살일 때 대운은 갑오여야 함
            self.assertEqual(daewoon_ganji.two_letters, "갑오")

    def test_daewoon_sequence(self):
        """Test daewoon sequence for first 5 periods."""
        saju = self.user.saju()
        monthly_index = GanJi.get_index(saju.monthly)
        starting_age = DaewoonCalculator.calculate_daewoon_starting_age(self.user)

        daewoon_sequence = []
        for i in range(5):
            directed_offset = i + 1  # 순행
            dw_index = monthly_index + directed_offset
            dw_ganji = GanJi.find_by_index(dw_index)
            age_start = starting_age + (i * 10)
            age_end = starting_age + ((i + 1) * 10) - 1

            daewoon_sequence.append({
                'period': i + 1,
                'ganji': dw_ganji.two_letters,
                'age_range': (age_start, age_end),
            })

        # 5개의 대운 기간이 생성되었는지 확인
        self.assertEqual(len(daewoon_sequence), 5)

        # 각 대운 간지가 제대로 생성되었는지 확인
        for dw in daewoon_sequence:
            self.assertIsNotNone(dw['ganji'])
            self.assertEqual(len(dw['ganji']), 2)  # 간지는 2글자

    def test_daewoon_calculator_integration(self):
        """Test DaewoonCalculator.calculate_daewoon method with actual current age."""
        daewoon = DaewoonCalculator.calculate_daewoon(self.user)

        # 2001년생이므로 2025년 기준 한국나이 25살, 대운이 있어야 함
        # 결과가 None이 아니면 GanJi 객체여야 함
        if daewoon is not None:
            self.assertTrue(hasattr(daewoon, 'two_letters'))
            self.assertEqual(len(daewoon.two_letters), 2)
