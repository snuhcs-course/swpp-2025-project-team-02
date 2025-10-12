from google.oauth2 import id_token
from google.auth.transport import requests
from django.conf import settings
from rest_framework_simplejwt.tokens import RefreshToken
import logging

logger = logging.getLogger(__name__)

class GoogleOAuthUtils:
    """Google OAuth 관련 유틸리티"""

    # Google OAuth2 허용 이슈어 목록
    ALLOWED_ISSUERS = ['accounts.google.com', 'https://accounts.google.com']

    @staticmethod
    def verify_and_extract_google_user_info(google_id_token: str) -> dict:
        """Google ID Token 검증 및 사용자 정보 추출"""
        try:
            # Google 토큰 검증
            google_token_payload = id_token.verify_oauth2_token(
                google_id_token,
                requests.Request(),
                settings.GOOGLE_OAUTH2_CLIENT_ID
            )

            # 이슈어 검증
            if google_token_payload['iss'] not in GoogleOAuthUtils.ALLOWED_ISSUERS:
                raise ValueError('Invalid token issuer')

            # 사용자 정보 추출
            return {
                'google_id': google_token_payload['sub'],
                'email': google_token_payload['email'],
                'name': google_token_payload.get('name', ''),
                'profile_image': google_token_payload.get('picture', ''),
                'is_verified': google_token_payload.get('email_verified', False)
            }

        except ValueError as error:
            logger.error(f"Google token verification failed: {error}")
            return None
    
    @staticmethod
    def generate_jwt_token_pair(user) -> dict:
        """JWT access 및 refresh 토큰 페어 생성"""
        refresh_token = RefreshToken.for_user(user)
        access_token = refresh_token.access_token

        # 토큰 만료 시간 계산
        access_token_lifetime = settings.SIMPLE_JWT.get('ACCESS_TOKEN_LIFETIME')
        expires_in_seconds = access_token_lifetime.total_seconds()

        return {
            'access_token': str(access_token),
            'refresh_token': str(refresh_token),
            'expires_in': expires_in_seconds
        }


    