"""
Fortuna API 통합 테스트
6가지 핵심 API의 유저 스토리 기반 통합 테스트
"""

import json
from unittest.mock import patch, MagicMock
from django.test import TestCase
from django.urls import reverse
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework import status
from datetime import date

User = get_user_model()


class FortunaAPIIntegrationTests(APITestCase):
    """
    Fortuna 앱의 6가지 핵심 API 통합 테스트

    테스트하는 API들:
    1. Google 로그인/회원가입 (POST /auth/google/)
    2. 토큰 갱신 (POST /auth/refresh/)
    3. 토큰 검증 (GET /auth/verify/)
    4. 프로필 조회 (GET /profile/)
    5. 프로필 업데이트 (PATCH /profile/)
    6. 로그아웃 (POST /auth/logout/)
    """

    def setUp(self):
        """테스트 환경 설정"""
        # 테스트용 Google 사용자 데이터
        self.valid_google_user_data = {
            'google_id': 'test_google_id_12345',
            'email': 'testuser@gmail.com',
            'name': '김철수',
            'profile_image': 'https://example.com/profile.jpg',
            'is_verified': True
        }

        # 테스트용 생년월일 데이터 (사주 계산용)
        self.birth_data = {
            'nickname': '운세왕',
            'birth_date': '1995-03-15',  # 1995년 3월 15일
            'solar_or_lunar': 'solar',   # 양력
            'birth_time_units': '오시',  # 오전 11시-오후 1시
            'gender': 'M'                # 남성
        }

    def test_complete_user_journey_new_user(self):
        """
        시나리오 1: 새로운 사용자의 완전한 여정

        사용자 스토리:
        "새로운 사용자로서, Google 계정으로 회원가입하고,
         프로필을 설정하여 사주 정보를 입력하고 싶습니다."

        단계:
        1. Google 로그인 → 새 계정 생성
        2. 프로필 조회 → 불완전한 프로필 확인
        3. 프로필 업데이트 → 생년월일, 성별 등 입력
        4. 프로필 재조회 → 완성된 프로필 및 사주 확인
        5. 로그아웃
        """

        # === 1단계: Google 로그인으로 새 계정 생성 ===
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data

            login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'fake_google_token_123'},
                format='json'
            )

        # 로그인 성공 검증
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)

        login_data = login_response.json()
        self.assertIn('access_token', login_data)
        self.assertIn('refresh_token', login_data)
        self.assertTrue(login_data['is_new_user'])          # 새 사용자임을 확인
        self.assertTrue(login_data['needs_additional_info'])  # 추가 정보 필요함을 확인

        # 발급받은 토큰들 저장
        access_token = login_data['access_token']
        refresh_token = login_data['refresh_token']
        user_id = login_data['user_id']

        # === 2단계: 초기 프로필 조회 ===
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')

        initial_profile_response = self.client.get(reverse('user:user_profile'))

        # 프로필 조회 성공 검증
        self.assertEqual(initial_profile_response.status_code, status.HTTP_200_OK)

        initial_profile = initial_profile_response.json()
        self.assertEqual(initial_profile['email'], self.valid_google_user_data['email'])
        self.assertEqual(initial_profile['name'], self.valid_google_user_data['name'])
        self.assertIsNone(initial_profile['birth_date_solar'])  # 아직 생년월일 없음
        self.assertIsNone(initial_profile['gender'])           # 아직 성별 없음

        # === 3단계: 프로필 업데이트 (생년월일, 성별, 닉네임 입력) ===
        profile_update_response = self.client.patch(
            reverse('user:user_profile'),
            data=self.birth_data,
            format='json'
        )

        # 프로필 업데이트 성공 검증
        self.assertEqual(profile_update_response.status_code, status.HTTP_200_OK)

        update_data = profile_update_response.json()
        self.assertEqual(update_data['message'], 'Profile updated successfully')

        updated_user = update_data['user']
        self.assertEqual(updated_user['nickname'], self.birth_data['nickname'])
        self.assertEqual(updated_user['gender'], '남자')  # 'M' → '남자'로 변환 확인
        self.assertIsNotNone(updated_user['birth_date_solar'])
        self.assertIsNotNone(updated_user['birth_date_lunar'])

        # 사주 계산 결과 확인
        self.assertIsNotNone(updated_user['yearly_ganji'])   # 년주 계산됨
        self.assertIsNotNone(updated_user['monthly_ganji'])  # 월주 계산됨
        self.assertIsNotNone(updated_user['daily_ganji'])    # 일주 계산됨
        self.assertIsNotNone(updated_user['hourly_ganji'])   # 시주 계산됨

        # === 4단계: 완성된 프로필 재조회 ===
        final_profile_response = self.client.get(reverse('user:user_profile'))

        self.assertEqual(final_profile_response.status_code, status.HTTP_200_OK)

        final_profile = final_profile_response.json()
        self.assertEqual(final_profile['nickname'], self.birth_data['nickname'])
        self.assertEqual(final_profile['gender'], '남자')
        self.assertIsNotNone(final_profile['yearly_ganji'])

        # === 5단계: 로그아웃 ===
        logout_response = self.client.post(
            reverse('user:logout'),
            data={'refresh_token': refresh_token},
            format='json'
        )

        # 로그아웃 성공 검증
        self.assertEqual(logout_response.status_code, status.HTTP_200_OK)
        logout_data = logout_response.json()
        self.assertEqual(logout_data['message'], 'Successfully logged out')

    def test_complete_user_journey_existing_user(self):
        """
        시나리오 2: 기존 사용자의 로그인 및 프로필 수정

        사용자 스토리:
        "기존 사용자로서, Google 계정으로 로그인하고,
         내 프로필 정보를 수정하고 싶습니다."

        단계:
        1. 기존 사용자 데이터 생성 (사전 조건)
        2. Google 로그인 → 기존 계정 인식
        3. 프로필 조회 → 기존 정보 확인
        4. 프로필 수정 → 닉네임 변경
        5. 토큰 갱신 테스트
        6. 로그아웃
        """

        # === 사전 조건: 기존 사용자 생성 ===
        existing_user = User.objects.create_user(
            email=self.valid_google_user_data['email'],
            first_name='기존사용자',
            google_id=self.valid_google_user_data['google_id'],
            nickname='기존닉네임'
        )

        # === 1단계: 기존 사용자 Google 로그인 ===
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data

            login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'fake_google_token_456'},
                format='json'
            )

        # 기존 사용자 로그인 검증
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)

        login_data = login_response.json()
        self.assertFalse(login_data['is_new_user'])  # 기존 사용자임을 확인
        self.assertEqual(login_data['user_id'], existing_user.id)

        access_token = login_data['access_token']
        refresh_token = login_data['refresh_token']

        # === 2단계: 기존 프로필 조회 ===
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')

        profile_response = self.client.get(reverse('user:user_profile'))

        self.assertEqual(profile_response.status_code, status.HTTP_200_OK)
        profile_data = profile_response.json()
        self.assertEqual(profile_data['nickname'], '기존닉네임')

        # === 3단계: 닉네임 변경 ===
        nickname_update_response = self.client.patch(
            reverse('user:user_profile'),
            data={'nickname': '새로운닉네임'},
            format='json'
        )

        self.assertEqual(nickname_update_response.status_code, status.HTTP_200_OK)

        update_data = nickname_update_response.json()
        self.assertEqual(update_data['user']['nickname'], '새로운닉네임')

        # === 4단계: 토큰 갱신 테스트 ===
        token_refresh_response = self.client.post(
            reverse('user:token_refresh'),
            data={'refresh': refresh_token},
            format='json'
        )

        # 토큰 갱신 성공 검증
        self.assertEqual(token_refresh_response.status_code, status.HTTP_200_OK)

        refresh_data = token_refresh_response.json()
        self.assertIn('access', refresh_data)

        new_access_token = refresh_data['access']
        self.assertNotEqual(new_access_token, access_token)  # 새 토큰 발급 확인

        # === 5단계: 새 토큰으로 API 접근 테스트 ===
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {new_access_token}')

        new_token_profile_response = self.client.get(reverse('user:user_profile'))
        self.assertEqual(new_token_profile_response.status_code, status.HTTP_200_OK)

        # === 6단계: 로그아웃 ===
        logout_response = self.client.post(
            reverse('user:logout'),
            data={'refresh_token': refresh_token},
            format='json'
        )

        self.assertEqual(logout_response.status_code, status.HTTP_200_OK)

    def test_token_verification_api(self):
        """
        시나리오 3: 토큰 검증 API 테스트

        목적: JWT 토큰의 유효성을 검증하는 API가 올바르게 작동하는지 확인
        """

        # 사용자 생성 및 로그인
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data

            login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'fake_token'},
                format='json'
            )

        access_token = login_response.json()['access_token']

        # === 유효한 토큰 검증 ===
        verify_response = self.client.post(
            reverse('user:token_verify'),
            data={'token': access_token},
            format='json'
        )

        # 토큰 검증 성공 확인
        self.assertEqual(verify_response.status_code, status.HTTP_200_OK)

        # === 잘못된 토큰 검증 ===
        invalid_verify_response = self.client.post(
            reverse('user:token_verify'),
            data={'token': 'invalid_token_12345'},
            format='json'
        )

        # 잘못된 토큰 거부 확인
        self.assertEqual(invalid_verify_response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_error_scenarios(self):
        """
        시나리오 4: 에러 상황 테스트

        목적: API들이 잘못된 요청에 대해 적절한 에러를 반환하는지 확인
        """

        # === 1. 잘못된 Google 토큰으로 로그인 시도 ===
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = None  # Google 토큰 검증 실패

            invalid_login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'invalid_google_token'},
                format='json'
            )

        self.assertEqual(invalid_login_response.status_code, status.HTTP_400_BAD_REQUEST)

        error_data = invalid_login_response.json()
        self.assertEqual(error_data['error'], 'Invalid token')

        # === 2. 인증 없이 프로필 접근 시도 ===
        unauthorized_profile_response = self.client.get(reverse('user:user_profile'))

        self.assertEqual(unauthorized_profile_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # === 3. 잘못된 refresh token으로 갱신 시도 ===
        invalid_refresh_response = self.client.post(
            reverse('user:token_refresh'),
            data={'refresh': 'invalid_refresh_token_123'},
            format='json'
        )

        self.assertEqual(invalid_refresh_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # === 4. 필수 데이터 없이 프로필 업데이트 시도 ===
        # 먼저 정상 로그인
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data

            login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'fake_token'},
                format='json'
            )

        access_token = login_response.json()['access_token']
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')

        # 잘못된 데이터로 프로필 업데이트 시도
        invalid_update_response = self.client.patch(
            reverse('user:user_profile'),
            data={
                'birth_date': 'invalid_date_format',  # 잘못된 날짜 형식
                'solar_or_lunar': 'invalid_calendar',  # 잘못된 달력 타입
                'gender': 'invalid_gender'             # 잘못된 성별
            },
            format='json'
        )

        # 데이터 검증 실패 확인
        self.assertEqual(invalid_update_response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_saju_calculation_integration(self):
        """
        시나리오 5: 사주 계산 통합 테스트

        목적: 프로필 업데이트 시 사주가 올바르게 계산되는지 확인
        """

        # 로그인
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data

            login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'fake_token'},
                format='json'
            )

        access_token = login_response.json()['access_token']
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')

        # === 양력 생년월일로 사주 계산 테스트 ===
        solar_birth_data = {
            'nickname': '사주테스트',
            'birth_date': '1990-05-15',
            'solar_or_lunar': 'solar',
            'birth_time_units': '진시',
            'gender': 'F'
        }

        solar_update_response = self.client.patch(
            reverse('user:user_profile'),
            data=solar_birth_data,
            format='json'
        )

        self.assertEqual(solar_update_response.status_code, status.HTTP_200_OK)

        solar_user = solar_update_response.json()['user']

        # 사주 계산 결과 검증
        self.assertIsNotNone(solar_user['birth_date_solar'])
        self.assertIsNotNone(solar_user['birth_date_lunar'])
        self.assertIsNotNone(solar_user['yearly_ganji'])
        self.assertIsNotNone(solar_user['monthly_ganji'])
        self.assertIsNotNone(solar_user['daily_ganji'])
        self.assertIsNotNone(solar_user['hourly_ganji'])

        # 각 간지가 2글자인지 확인 (한국 전통 간지 형식)
        self.assertEqual(len(solar_user['yearly_ganji']), 2)
        self.assertEqual(len(solar_user['monthly_ganji']), 2)
        self.assertEqual(len(solar_user['daily_ganji']), 2)
        self.assertEqual(len(solar_user['hourly_ganji']), 2)

        # === 음력 생년월일로 사주 계산 테스트 ===
        lunar_birth_data = {
            'birth_date': '1985-12-01',  # 음력 - 다른 년도와 달
            'solar_or_lunar': 'lunar',
            'birth_time_units': '해시',  # 다른 시진
        }

        lunar_update_response = self.client.patch(
            reverse('user:user_profile'),
            data=lunar_birth_data,
            format='json'
        )

        self.assertEqual(lunar_update_response.status_code, status.HTTP_200_OK)

        lunar_user = lunar_update_response.json()['user']

        # 음력→양력 변환 및 사주 재계산 확인
        self.assertIsNotNone(lunar_user['birth_date_solar'])
        self.assertIsNotNone(lunar_user['birth_date_lunar'])

        # 달력 타입 변경으로 인한 사주 변경 확인
        self.assertNotEqual(solar_user['yearly_ganji'], lunar_user['yearly_ganji'])

    def tearDown(self):
        """테스트 후 정리"""
        # 테스트 중 생성된 모든 사용자 데이터 삭제
        User.objects.all().delete()


if __name__ == '__main__':
    """
    테스트 실행 가이드:

    1. 전체 통합 테스트 실행:
       python manage.py test user.test_integration

    2. 특정 시나리오만 실행:
       python manage.py test user.test_integration.FortunaAPIIntegrationTests.test_complete_user_journey_new_user

    3. 상세한 로그와 함께 실행:
       python manage.py test user.test_integration --verbosity=2
    """
    pass