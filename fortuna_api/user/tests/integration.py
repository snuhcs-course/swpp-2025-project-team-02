"""
Fortuna API 통합 테스트
6가지 핵심 API의 유저 스토리 기반 통합 테스트
"""

import json
from unittest.mock import patch, MagicMock
from django.test import TestCase, override_settings
from django.urls import reverse
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework import status
from datetime import date

User = get_user_model()


@override_settings(DEVELOPMENT_MODE=False)
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
            'input_birth_date': '1995-03-15',  # 1995년 3월 15일
            'input_calendar_type': 'solar',     # 양력
            'birth_time_units': '오시',         # 오전 11시-오후 1시
            'gender': 'M'                       # 남성
        }

    # ========== Helper Methods ==========

    def _mock_google_login(self, id_token='fake_google_token'):
        """Google 로그인을 모킹하고 응답을 반환하는 헬퍼"""
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = self.valid_google_user_data
            response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': id_token},
                format='json'
            )
        return response

    def _set_auth_header(self, access_token):
        """인증 헤더를 설정하는 헬퍼"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')

    def _get_profile(self):
        """프로필 조회 헬퍼"""
        return self.client.get(reverse('user:user_profile'))

    def _update_profile(self, data):
        """프로필 업데이트 헬퍼"""
        return self.client.patch(reverse('user:user_profile'), data=data, format='json')

    def _refresh_token(self, refresh_token):
        """토큰 갱신 헬퍼"""
        return self.client.post(
            reverse('user:token_refresh'),
            data={'refresh': refresh_token},
            format='json'
        )

    def _logout(self, refresh_token):
        """로그아웃 헬퍼"""
        return self.client.post(
            reverse('user:logout'),
            data={'refresh_token': refresh_token},
            format='json'
        )

    def _assert_login_successful(self, response, is_new_user=True):
        """로그인 성공 검증 헬퍼"""
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        self.assertIn('access_token', data)
        self.assertIn('refresh_token', data)
        self.assertEqual(data['is_new_user'], is_new_user)
        return data

    def _assert_profile_complete(self, profile_data):
        """프로필 완성 검증 헬퍼"""
        self.assertIsNotNone(profile_data['birth_date_solar'])
        self.assertIsNotNone(profile_data['birth_date_lunar'])
        self.assertIsNotNone(profile_data['yearly_ganji'])
        self.assertIsNotNone(profile_data['monthly_ganji'])
        self.assertIsNotNone(profile_data['daily_ganji'])
        self.assertIsNotNone(profile_data['hourly_ganji'])

    def _assert_saju_calculated(self, user_data):
        """사주 계산 결과 검증 헬퍼"""
        ganji_fields = ['yearly_ganji', 'monthly_ganji', 'daily_ganji', 'hourly_ganji']
        for field in ganji_fields:
            self.assertIsNotNone(user_data[field], f"{field} should be calculated")
            self.assertEqual(len(user_data[field]), 2, f"{field} should be 2 characters")

    # ========== Test Cases ==========

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

        # 1단계: Google 로그인으로 새 계정 생성
        login_response = self._mock_google_login(id_token='fake_google_token_123')
        login_data = self._assert_login_successful(login_response, is_new_user=True)
        self.assertTrue(login_data['needs_additional_info'])

        access_token = login_data['access_token']
        refresh_token = login_data['refresh_token']

        # 2단계: 초기 프로필 조회 - 불완전한 프로필 확인
        self._set_auth_header(access_token)
        initial_profile_response = self._get_profile()

        self.assertEqual(initial_profile_response.status_code, status.HTTP_200_OK)
        initial_profile = initial_profile_response.json()
        self.assertEqual(initial_profile['email'], self.valid_google_user_data['email'])
        self.assertEqual(initial_profile['name'], self.valid_google_user_data['name'])
        self.assertIsNone(initial_profile['birth_date_solar'])
        self.assertIsNone(initial_profile['gender'])

        # 3단계: 프로필 업데이트 - 생년월일, 성별, 닉네임 입력
        update_response = self._update_profile(self.birth_data)

        self.assertEqual(update_response.status_code, status.HTTP_200_OK)
        update_data = update_response.json()
        self.assertEqual(update_data['message'], 'Profile updated successfully')

        updated_user = update_data['user']
        self.assertEqual(updated_user['nickname'], self.birth_data['nickname'])
        self.assertEqual(updated_user['gender'], '남자')
        self._assert_saju_calculated(updated_user)

        # 4단계: 완성된 프로필 재조회
        final_profile_response = self._get_profile()
        self.assertEqual(final_profile_response.status_code, status.HTTP_200_OK)

        final_profile = final_profile_response.json()
        self.assertEqual(final_profile['nickname'], self.birth_data['nickname'])
        self.assertEqual(final_profile['gender'], '남자')
        self._assert_profile_complete(final_profile)

        # 5단계: 로그아웃
        logout_response = self._logout(refresh_token)
        self.assertEqual(logout_response.status_code, status.HTTP_200_OK)
        self.assertEqual(logout_response.json()['message'], 'Successfully logged out')

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

        # 사전 조건: 기존 사용자 생성
        existing_user = User.objects.create_user(
            email=self.valid_google_user_data['email'],
            first_name='기존사용자',
            google_id=self.valid_google_user_data['google_id'],
            nickname='기존닉네임'
        )

        # 1단계: 기존 사용자 Google 로그인
        login_response = self._mock_google_login(id_token='fake_google_token_456')
        login_data = self._assert_login_successful(login_response, is_new_user=False)
        self.assertEqual(login_data['user_id'], existing_user.id)

        access_token = login_data['access_token']
        refresh_token = login_data['refresh_token']

        # 2단계: 기존 프로필 조회
        self._set_auth_header(access_token)
        profile_response = self._get_profile()

        self.assertEqual(profile_response.status_code, status.HTTP_200_OK)
        self.assertEqual(profile_response.json()['nickname'], '기존닉네임')

        # 3단계: 닉네임 변경
        update_response = self._update_profile({'nickname': '새로운닉네임'})

        self.assertEqual(update_response.status_code, status.HTTP_200_OK)
        self.assertEqual(update_response.json()['user']['nickname'], '새로운닉네임')

        # 4단계: 토큰 갱신 테스트
        refresh_response = self._refresh_token(refresh_token)
        self.assertEqual(refresh_response.status_code, status.HTTP_200_OK)

        refresh_data = refresh_response.json()
        self.assertIn('access', refresh_data)
        new_access_token = refresh_data['access']
        self.assertNotEqual(new_access_token, access_token)

        # 5단계: 새 토큰으로 API 접근 테스트
        self._set_auth_header(new_access_token)
        new_token_profile_response = self._get_profile()
        self.assertEqual(new_token_profile_response.status_code, status.HTTP_200_OK)

        # 6단계: 로그아웃
        logout_response = self._logout(refresh_token)
        self.assertEqual(logout_response.status_code, status.HTTP_200_OK)

    def test_token_verification_api(self):
        """
        시나리오 3: 토큰 검증 API 테스트

        목적: JWT 토큰의 유효성을 검증하는 API가 올바르게 작동하는지 확인
        """

        # 사용자 생성 및 로그인
        login_response = self._mock_google_login()
        access_token = login_response.json()['access_token']

        # 유효한 토큰 검증
        verify_response = self.client.post(
            reverse('user:token_verify'),
            data={'token': access_token},
            format='json'
        )
        self.assertEqual(verify_response.status_code, status.HTTP_200_OK)
        self.assertTrue(verify_response.json()['valid'])

        # 잘못된 토큰 검증
        invalid_verify_response = self.client.post(
            reverse('user:token_verify'),
            data={'token': 'invalid_token_12345'},
            format='json'
        )
        self.assertEqual(invalid_verify_response.status_code, status.HTTP_200_OK)
        self.assertFalse(invalid_verify_response.json()['valid'])

    def test_error_scenarios(self):
        """
        시나리오 4: 에러 상황 테스트

        목적: API들이 잘못된 요청에 대해 적절한 에러를 반환하는지 확인
        """

        # 1. 잘못된 Google 토큰으로 로그인 시도
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = None
            invalid_login_response = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'invalid_google_token'},
                format='json'
            )

        self.assertEqual(invalid_login_response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(invalid_login_response.json()['error'], 'Invalid token')

        # 2. 인증 없이 프로필 접근 시도
        unauthorized_response = self._get_profile()
        self.assertEqual(unauthorized_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # 3. 잘못된 refresh token으로 갱신 시도
        invalid_refresh_response = self._refresh_token('invalid_refresh_token_123')
        self.assertEqual(invalid_refresh_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # 4. 필수 데이터 없이 프로필 업데이트 시도
        login_response = self._mock_google_login()
        access_token = login_response.json()['access_token']
        self._set_auth_header(access_token)

        invalid_data = {
            'birth_date': 'invalid_date_format',
            'solar_or_lunar': 'invalid_calendar',
            'gender': 'invalid_gender'
        }
        invalid_update_response = self._update_profile(invalid_data)
        self.assertEqual(invalid_update_response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_saju_calculation_integration(self):
        """
        시나리오 5: 사주 계산 통합 테스트

        목적: 프로필 업데이트 시 사주가 올바르게 계산되는지 확인
        """

        # 로그인 및 인증 설정
        login_response = self._mock_google_login()
        access_token = login_response.json()['access_token']
        self._set_auth_header(access_token)

        # 양력 생년월일로 사주 계산 테스트
        solar_birth_data = {
            'nickname': '사주테스트',
            'input_birth_date': '1990-05-15',
            'input_calendar_type': 'solar',
            'birth_time_units': '진시',
            'gender': 'F'
        }
        solar_response = self._update_profile(solar_birth_data)
        self.assertEqual(solar_response.status_code, status.HTTP_200_OK)

        solar_user = solar_response.json()['user']
        self._assert_saju_calculated(solar_user)

        # 음력 생년월일로 사주 계산 테스트
        lunar_birth_data = {
            'input_birth_date': '1985-12-01',
            'input_calendar_type': 'lunar',
            'birth_time_units': '해시',
        }
        lunar_response = self._update_profile(lunar_birth_data)
        self.assertEqual(lunar_response.status_code, status.HTTP_200_OK)

        lunar_user = lunar_response.json()['user']
        self._assert_saju_calculated(lunar_user)

        # 달력 타입 변경으로 인한 사주 변경 확인
        self.assertNotEqual(solar_user['yearly_ganji'], lunar_user['yearly_ganji'])

    def test_user_deletion_and_reregistration(self):
        """
        시나리오 6: 사용자 탈퇴 및 재가입 테스트

        사용자 스토리:
        "사용자로서, 앱에서 탈퇴하고, 나중에 같은 Google 계정으로
         다시 가입할 수 있어야 합니다."

        단계:
        1. Google 로그인으로 신규 가입
        2. 프로필 업데이트
        3. 계정 탈퇴
        4. 동일 Google 계정으로 재로그인
        5. 새 계정 생성 확인 (과거 데이터 없음)
        """

        # 1단계: 첫 번째 가입
        login_response = self._mock_google_login()
        first_login_data = self._assert_login_successful(login_response, is_new_user=True)

        first_access_token = first_login_data['access_token']
        first_user_id = first_login_data['user_id']

        # 2단계: 프로필 업데이트
        self._set_auth_header(first_access_token)
        update_response = self._update_profile(self.birth_data)
        self.assertEqual(update_response.status_code, status.HTTP_200_OK)

        # 3단계: 계정 탈퇴
        delete_response = self.client.delete(reverse('user:user_deletion'))
        self.assertEqual(delete_response.status_code, status.HTTP_200_OK)
        delete_data = delete_response.json()
        self.assertEqual(delete_data['message'], 'Account deleted successfully')
        self.assertIn('deleted_at', delete_data)

        # 탈퇴 후 프로필 조회 불가 확인
        profile_response = self._get_profile()
        self.assertEqual(profile_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # 4단계: 동일 Google 계정으로 재로그인
        # 인증 헤더 제거 (탈퇴한 사용자의 토큰 제거)
        self.client.credentials()
        relogin_response = self._mock_google_login(id_token='fake_google_token_456')
        relogin_data = self._assert_login_successful(relogin_response, is_new_user=True)

        # 5단계: 새 계정 확인
        second_user_id = relogin_data['user_id']
        self.assertNotEqual(first_user_id, second_user_id)
        self.assertTrue(relogin_data['needs_additional_info'])

        # 새 계정의 프로필이 비어있는지 확인
        self._set_auth_header(relogin_data['access_token'])
        new_profile_response = self._get_profile()
        self.assertEqual(new_profile_response.status_code, status.HTTP_200_OK)

        new_profile = new_profile_response.json()
        self.assertIsNone(new_profile['birth_date_solar'])
        self.assertIsNone(new_profile['nickname'])

    def test_user_deletion_authentication_after_delete(self):
        """
        시나리오 7: 탈퇴 후 인증 검증

        목적: 탈퇴한 사용자의 토큰이 더 이상 유효하지 않음을 확인
        """

        # 로그인
        login_response = self._mock_google_login()
        access_token = login_response.json()['access_token']
        self._set_auth_header(access_token)

        # 계정 탈퇴
        delete_response = self.client.delete(reverse('user:user_deletion'))
        self.assertEqual(delete_response.status_code, status.HTTP_200_OK)

        # 탈퇴 후에는 해당 토큰으로 API 호출 불가 (인증 실패)
        profile_response = self._get_profile()
        self.assertEqual(profile_response.status_code, status.HTTP_401_UNAUTHORIZED)

        # 탈퇴 후 재탈퇴 시도도 인증 실패
        second_delete_response = self.client.delete(reverse('user:user_deletion'))
        self.assertEqual(second_delete_response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_saju_calculation_solar_vs_lunar_same_date(self):
        """
        시나리오 8: 양력/음력 입력에 따른 사주 계산 검증

        사용자 스토리:
        "양력 2000년 3월 17일을 입력한 유저 A와
         음력 2000년 2월 12일을 입력한 유저 B의
         사주 계산 결과가 동일해야 합니다. (같은 날이므로)"

        테스트 목적:
        - 양력/음력 변환이 올바르게 작동하는지 확인
        - 변환 후 DB에 저장되는 사주 값이 정확한지 검증
        - 같은 날짜에 대해 입력 달력 타입과 상관없이 동일한 사주가 계산되는지 확인
        """

        # 유저 A: 양력 2000년 3월 17일로 가입
        login_response_a = self._mock_google_login(id_token='user_a_token')
        user_a_data = self._assert_login_successful(login_response_a, is_new_user=True)

        self._set_auth_header(user_a_data['access_token'])

        # 유저 A 프로필 업데이트 (양력 입력)
        user_a_birth_data = {
            'nickname': '유저A_양력',
            'input_birth_date': '2000-03-17',  # 양력 2000년 3월 17일
            'input_calendar_type': 'solar',
            'birth_time_units': '미시',  # 미시 (13:00-15:00, 14시 포함)
            'gender': 'M'
        }

        update_response_a = self._update_profile(user_a_birth_data)
        self.assertEqual(update_response_a.status_code, status.HTTP_200_OK)

        user_a_result = update_response_a.json()['user']

        # 유저 A 검증: 양력과 음력이 모두 저장되었는지 확인
        self.assertIsNotNone(user_a_result['birth_date_solar'])
        self.assertIsNotNone(user_a_result['birth_date_lunar'])
        self._assert_saju_calculated(user_a_result)

        # 유저 A DB에서 재조회하여 음력 변환 확인
        profile_a = self._get_profile()
        self.assertEqual(profile_a.status_code, status.HTTP_200_OK)
        user_a_profile = profile_a.json()

        # 양력 2000-03-17 -> 음력 2000-02-12로 변환되었는지 확인
        self.assertEqual(user_a_profile['birth_date_solar'], '2000-03-17')
        self.assertEqual(user_a_profile['birth_date_lunar'], '2000-02-12')

        # ========================================
        # 유저 B: 음력 2000년 2월 12일로 가입
        # ========================================

        # 새 유저를 위해 인증 헤더 제거
        self.client.credentials()

        login_response_b = self._mock_google_login(id_token='user_b_token')

        # 새로운 Google 사용자 데이터 (다른 이메일)
        with patch('user.utils.GoogleOAuthUtils.verify_google_token') as mock_verify:
            mock_verify.return_value = {
                'google_id': 'test_google_id_67890',
                'email': 'userb@gmail.com',
                'name': '김민수',
                'profile_image': 'https://example.com/userb.jpg',
                'is_verified': True
            }
            login_response_b = self.client.post(
                reverse('user:google_auth'),
                data={'id_token': 'user_b_token'},
                format='json'
            )

        user_b_data = self._assert_login_successful(login_response_b, is_new_user=True)
        self._set_auth_header(user_b_data['access_token'])

        # 유저 B 프로필 업데이트 (음력 입력)
        user_b_birth_data = {
            'nickname': '유저B_음력',
            'input_birth_date': '2000-02-12',  # 음력 2000년 2월 12일
            'input_calendar_type': 'lunar',
            'birth_time_units': '미시',  # 미시 (13:00-15:00, 14시 포함)
            'gender': 'M'
        }

        update_response_b = self._update_profile(user_b_birth_data)
        self.assertEqual(update_response_b.status_code, status.HTTP_200_OK)

        user_b_result = update_response_b.json()['user']

        # 유저 B 검증: 양력과 음력이 모두 저장되었는지 확인
        self.assertIsNotNone(user_b_result['birth_date_solar'])
        self.assertIsNotNone(user_b_result['birth_date_lunar'])
        self._assert_saju_calculated(user_b_result)

        # 유저 B DB에서 재조회하여 양력 변환 확인
        profile_b = self._get_profile()
        self.assertEqual(profile_b.status_code, status.HTTP_200_OK)
        user_b_profile = profile_b.json()

        # 음력 2000-02-12 -> 양력 2000-03-17로 변환되었는지 확인
        self.assertEqual(user_b_profile['birth_date_lunar'], '2000-02-12')
        self.assertEqual(user_b_profile['birth_date_solar'], '2000-03-17')

        # ========================================
        # 두 유저의 사주 계산 결과 비교
        # ========================================

        # 같은 양력 날짜이므로 사주 계산 결과가 동일해야 함
        self.assertEqual(
            user_a_profile['yearly_ganji'],
            user_b_profile['yearly_ganji'],
            "년주가 동일해야 합니다"
        )
        self.assertEqual(
            user_a_profile['monthly_ganji'],
            user_b_profile['monthly_ganji'],
            "월주가 동일해야 합니다"
        )
        self.assertEqual(
            user_a_profile['daily_ganji'],
            user_b_profile['daily_ganji'],
            "일주가 동일해야 합니다"
        )
        self.assertEqual(
            user_a_profile['hourly_ganji'],
            user_b_profile['hourly_ganji'],
            "시주가 동일해야 합니다 (같은 시진)"
        )

        # 입력한 달력 타입은 다르지만 결과는 동일
        self.assertEqual(user_a_profile['solar_or_lunar'], 'solar')
        self.assertEqual(user_b_profile['solar_or_lunar'], 'lunar')

        # 최종 검증: 두 유저의 사주가 완전히 일치하는지 확인
        self.assertEqual(user_a_profile['birth_date_solar'], user_b_profile['birth_date_solar'])
        self.assertEqual(user_a_profile['birth_date_lunar'], user_b_profile['birth_date_lunar'])

        # ========================================
        # 예상되는 사주 값 검증
        # ========================================
        # 양력 2000-03-17 / 음력 2000-02-12, 미시(14시) 기준 사주
        expected_saju = {
            'yearly_ganji': '경진',
            'monthly_ganji': '기묘',
            'daily_ganji': '갑술',
            'hourly_ganji': '신미'
        }

        # 유저 A (양력 입력) 사주 검증
        self.assertEqual(user_a_profile['yearly_ganji'], expected_saju['yearly_ganji'],
                        f"유저 A 년주가 {expected_saju['yearly_ganji']}여야 합니다")
        self.assertEqual(user_a_profile['monthly_ganji'], expected_saju['monthly_ganji'],
                        f"유저 A 월주가 {expected_saju['monthly_ganji']}여야 합니다")
        self.assertEqual(user_a_profile['daily_ganji'], expected_saju['daily_ganji'],
                        f"유저 A 일주가 {expected_saju['daily_ganji']}여야 합니다")
        self.assertEqual(user_a_profile['hourly_ganji'], expected_saju['hourly_ganji'],
                        f"유저 A 시주가 {expected_saju['hourly_ganji']}여야 합니다")

        # 유저 B (음력 입력) 사주 검증
        self.assertEqual(user_b_profile['yearly_ganji'], expected_saju['yearly_ganji'],
                        f"유저 B 년주가 {expected_saju['yearly_ganji']}여야 합니다")
        self.assertEqual(user_b_profile['monthly_ganji'], expected_saju['monthly_ganji'],
                        f"유저 B 월주가 {expected_saju['monthly_ganji']}여야 합니다")
        self.assertEqual(user_b_profile['daily_ganji'], expected_saju['daily_ganji'],
                        f"유저 B 일주가 {expected_saju['daily_ganji']}여야 합니다")
        self.assertEqual(user_b_profile['hourly_ganji'], expected_saju['hourly_ganji'],
                        f"유저 B 시주가 {expected_saju['hourly_ganji']}여야 합니다")

    def tearDown(self):
        """테스트 후 정리"""
        # 테스트 중 생성된 모든 사용자 데이터 삭제 (탈퇴한 사용자 포함)
        User.all_objects.all().delete()


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