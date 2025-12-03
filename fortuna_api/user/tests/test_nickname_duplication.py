"""
닉네임 중복 허용 테스트
"""

from django.test import TestCase
from django.contrib.auth import get_user_model
from datetime import date
from rest_framework.test import APIClient
from rest_framework import status

User = get_user_model()


class NicknameDuplicationTests(TestCase):
    """닉네임 중복 허용 테스트"""

    def setUp(self):
        """테스트 환경 설정"""
        self.client = APIClient()

        # 첫 번째 테스트 사용자 생성
        self.user1 = User.objects.create_user(
            email='test1@example.com',
            first_name='Test User 1',
            nickname='sameNickname',
            birth_date_solar=date(1990, 1, 15),
            birth_time_units='묘시',
            gender='M',
            solar_or_lunar='solar'
        )

    def test_create_user_with_duplicate_nickname_via_model(self):
        """모델을 통해 중복 닉네임으로 사용자 생성 테스트"""
        # 동일한 닉네임으로 두 번째 사용자 생성 시도
        user2 = User.objects.create_user(
            email='test2@example.com',
            first_name='Test User 2',
            nickname='sameNickname',  # 첫 번째 사용자와 동일한 닉네임
            birth_date_solar=date(1991, 2, 20),
            birth_time_units='오시',
            gender='F',
            solar_or_lunar='solar'
        )

        # 두 사용자 모두 생성되었는지 확인
        self.assertEqual(User.objects.count(), 2)

        # 두 사용자의 닉네임이 동일한지 확인
        self.assertEqual(self.user1.nickname, user2.nickname)
        self.assertEqual(user2.nickname, 'sameNickname')

    def test_update_nickname_to_duplicate_via_api(self):
        """API를 통해 닉네임을 중복된 값으로 변경 테스트"""
        # 두 번째 사용자 생성
        user2 = User.objects.create_user(
            email='test3@example.com',
            first_name='Test User 3',
            nickname='differentNickname',
            birth_date_solar=date(1992, 3, 25),
            birth_time_units='신시',
            gender='M',
            solar_or_lunar='solar'
        )

        # 두 번째 사용자로 로그인
        self.client.force_authenticate(user=user2)

        # 첫 번째 사용자와 동일한 닉네임으로 변경 시도
        response = self.client.patch('/api/user/profile/', {
            'nickname': 'sameNickname'  # user1과 동일한 닉네임
        }, format='json')

        # 성공적으로 업데이트되어야 함
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # 닉네임이 변경되었는지 확인
        user2.refresh_from_db()
        self.assertEqual(user2.nickname, 'sameNickname')

        # 두 사용자의 닉네임이 동일한지 확인
        self.assertEqual(self.user1.nickname, user2.nickname)

    def test_multiple_users_with_same_nickname(self):
        """여러 사용자가 동일한 닉네임을 사용할 수 있는지 테스트"""
        # 추가로 3명의 사용자를 동일한 닉네임으로 생성
        for i in range(2, 5):
            User.objects.create_user(
                email=f'test{i}@example.com',
                first_name=f'Test User {i}',
                nickname='sameNickname',  # 모두 동일한 닉네임
                birth_date_solar=date(1990 + i, i, 10),
                birth_time_units='자시',
                gender='M',
                solar_or_lunar='solar'
            )

        # 총 4명의 사용자가 생성되었는지 확인
        self.assertEqual(User.objects.count(), 4)

        # 모든 사용자가 동일한 닉네임을 가지고 있는지 확인
        all_users = User.objects.all()
        for user in all_users:
            self.assertEqual(user.nickname, 'sameNickname')

    def test_nickname_length_validation_still_works(self):
        """닉네임 길이 검증은 여전히 작동하는지 테스트"""
        self.client.force_authenticate(user=self.user1)

        # 1자 닉네임 (너무 짧음)
        response = self.client.patch('/api/user/profile/', {
            'nickname': 'a'
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('nickname', response.data)

        # 7자 닉네임 (너무 김)
        response = self.client.patch('/api/user/profile/', {
            'nickname': '테스트테스트다'  # 7자
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('nickname', response.data)

        # 정상 범위 (2-6자)
        response = self.client.patch('/api/user/profile/', {
            'nickname': '테스트테'  # 4자
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # 6자 정확히 (경계값 테스트)
        response = self.client.patch('/api/user/profile/', {
            'nickname': '테스트테스트'  # 6자
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
