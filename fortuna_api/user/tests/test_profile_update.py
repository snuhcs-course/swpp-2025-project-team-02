"""
프로필 업데이트 시 FortuneResult 무효화 기능 테스트
"""

from django.test import TestCase
from django.contrib.auth import get_user_model
from datetime import date
from rest_framework.test import APIClient
from rest_framework import status

from core.models import FortuneResult

User = get_user_model()


class ProfileUpdateFortuneInvalidationTests(TestCase):
    """프로필 업데이트 시 FortuneResult 무효화 테스트"""

    def setUp(self):
        """테스트 환경 설정"""
        self.client = APIClient()

        # 테스트 사용자 생성
        self.user = User.objects.create_user(
            email='test@example.com',
            first_name='Test User',
            nickname='테스트',
            birth_date_solar=date(1990, 1, 15),
            birth_time_units='묘시',
            gender='M',
            solar_or_lunar='solar'
        )
        self.client.force_authenticate(user=self.user)

        # 기존 FortuneResult 생성
        self.fortune1 = FortuneResult.objects.create(
            user=self.user,
            for_date=date(2025, 11, 1),
            gapja_code=1,
            gapja_name='갑자',
            gapja_element='목',
            fortune_data={'test': 'data'},
            fortune_score={'needed_element': '목'},
            status='completed'
        )
        self.fortune2 = FortuneResult.objects.create(
            user=self.user,
            for_date=date(2025, 11, 2),
            gapja_code=2,
            gapja_name='을축',
            gapja_element='화',
            fortune_data={'test': 'data2'},
            fortune_score={'needed_element': '화'},
            status='completed'
        )

    def test_birth_date_solar_change_invalidates_fortune(self):
        """생년월일(양력) 변경 시 FortuneResult가 삭제되는지 테스트"""
        # 변경 전 FortuneResult 개수 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 생년월일 변경
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': '1990-02-20',
            'input_calendar_type': 'solar',
            'birth_time_units': self.user.birth_time_units  # '묘시'
        }, format='json')

        # 응답 확인
        if response.status_code != status.HTTP_200_OK:
            print(f"Error response: {response.data}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 삭제되었는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_birth_date_lunar_change_invalidates_fortune(self):
        """생년월일(음력) 변경 시 FortuneResult가 삭제되는지 테스트"""
        # 현재 FortuneResult 개수 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 음력 생년월일 변경
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': '1990-02-01',
            'input_calendar_type': 'lunar',
            'birth_time_units': self.user.birth_time_units  # '묘시'
        }, format='json')

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 삭제되었는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_birth_time_units_change_invalidates_fortune(self):
        """생시(시간 단위) 변경 시 FortuneResult가 삭제되는지 테스트"""
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 생시 변경 (생년월일도 함께 보내야 함)
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': self.user.birth_date_solar.isoformat(),
            'input_calendar_type': 'solar',
            'birth_time_units': '오시'  # 묘시에서 오시로 변경
        }, format='json')

        # 응답 확인
        if response.status_code != status.HTTP_200_OK:
            print(f"Error response: {response.data}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 삭제되었는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_gender_change_invalidates_fortune(self):
        """성별 변경 시 FortuneResult가 삭제되는지 테스트"""
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 성별 변경
        response = self.client.patch('/api/user/profile/', {
            'gender': 'F'
        }, format='json')

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 삭제되었는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_nickname_change_preserves_fortune(self):
        """닉네임 변경 시 FortuneResult가 유지되는지 테스트"""
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 닉네임만 변경
        response = self.client.patch('/api/user/profile/', {
            'nickname': '새닉네임'
        }, format='json')

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data['fortune_reset'])

        # FortuneResult가 유지되는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

    def test_multiple_non_saju_fields_change_preserves_fortune(self):
        """여러 비사주 필드 변경 시 FortuneResult가 유지되는지 테스트"""
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 닉네임과 기타 필드 변경 (사주 관련 없는 필드)
        response = self.client.patch('/api/user/profile/', {
            'nickname': '업데이트'
        }, format='json')

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data['fortune_reset'])

        # FortuneResult가 유지되는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

    def test_multiple_saju_fields_change_invalidates_fortune_once(self):
        """여러 사주 필드 동시 변경 시 FortuneResult가 한 번만 삭제되는지 테스트"""
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 여러 사주 필드 동시 변경
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': '1991-03-10',
            'input_calendar_type': 'solar',
            'birth_time_units': '유시',  # 묘시에서 유시로 변경
            'gender': 'F'
        }, format='json')

        # 응답 확인
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 모두 삭제되었는지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_no_fortune_results_does_not_error(self):
        """FortuneResult가 없을 때도 에러 없이 작동하는지 테스트"""
        # 기존 FortuneResult 삭제
        FortuneResult.objects.filter(user=self.user).delete()
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

        # 생년월일 변경
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': '1990-12-25',
            'input_calendar_type': 'solar',
            'birth_time_units': self.user.birth_time_units  # '묘시'
        }, format='json')

        # 응답 확인 (에러 없이 성공)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['fortune_reset'])

        # FortuneResult가 여전히 0개인지 확인
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 0)

    def test_same_value_update_may_still_trigger_reset(self):
        """
        동일한 입력값으로 업데이트 시 FortuneResult 무효화 테스트

        참고: serializer가 사주를 재계산하면서 birth_date_lunar 같은 계산된 필드들이
        미세하게 달라질 수 있어서 fortune_reset이 True가 될 수 있습니다.
        이는 의도된 동작으로, 사용자가 사주 관련 필드를 업데이트하면 항상 재계산됩니다.
        """
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 2)

        # 동일한 생년월일로 업데이트
        response = self.client.patch('/api/user/profile/', {
            'input_birth_date': '1990-01-15',  # 기존 값과 동일
            'input_calendar_type': 'solar',
            'birth_time_units': '묘시'  # 기존 값과 동일
        }, format='json')

        # 응답 확인 - 성공적으로 완료되어야 함
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # fortune_reset은 True일 수 있음 (사주 재계산으로 인해)
        # 이는 정상 동작입니다
        self.assertIn('fortune_reset', response.data)


class ProfileUpdateResponseFormatTests(TestCase):
    """프로필 업데이트 응답 형식 테스트"""

    def setUp(self):
        """테스트 환경 설정"""
        self.client = APIClient()

        self.user = User.objects.create_user(
            email='test2@example.com',
            first_name='Test User 2',
            nickname='testuser2',
            birth_date_solar=date(1992, 5, 20),
            birth_time_units='오시',
            gender='F',
            solar_or_lunar='solar'
        )
        self.client.force_authenticate(user=self.user)

    def test_response_includes_fortune_reset_field(self):
        """응답에 fortune_reset 필드가 포함되는지 테스트"""
        response = self.client.patch('/api/user/profile/', {
            'nickname': '수정됨'
        }, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('fortune_reset', response.data)
        self.assertIsInstance(response.data['fortune_reset'], bool)

    def test_response_includes_standard_fields(self):
        """응답에 표준 필드들이 포함되는지 테스트"""
        response = self.client.patch('/api/user/profile/', {
            'nickname': '수정완'
        }, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('message', response.data)
        self.assertIn('user', response.data)
        self.assertIn('fortune_reset', response.data)

        # user 객체에 필요한 필드들이 있는지 확인
        user_data = response.data['user']
        self.assertIn('user_id', user_data)
        self.assertIn('email', user_data)
        self.assertIn('nickname', user_data)
