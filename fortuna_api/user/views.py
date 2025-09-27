from rest_framework import status, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from django.contrib.auth import get_user_model
from django.db import transaction
from django.conf import settings
from drf_spectacular.utils import extend_schema
import logging

from .serializers import (
    GoogleLoginSerializer,
    UserProfileUpdateSerializer
)
from .utils import GoogleOAuthUtils
from .permissions import DevelopmentOrAuthenticated

logger = logging.getLogger(__name__)
User = get_user_model()


@extend_schema(
    summary="Google OAuth Login",
    description="Login or register user with Google OAuth ID token",
    request=GoogleLoginSerializer,
    responses={
        200: {
            'description': 'Authentication successful',
            'content': {
                'application/json': {
                    'example': {
                        'access_token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...',
                        'refresh_token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...',
                        'user_id': 1,
                        'email': 'user@example.com',
                        'name': 'John Doe',
                        'profile_image': 'https://lh3.googleusercontent.com/...',
                        'is_new_user': False,
                        'needs_additional_info': True
                    }
                }
            }
        },
        400: {
            'description': 'Invalid token or authentication failed',
            'content': {
                'application/json': {
                    'example': {
                        'error': 'Invalid token',
                        'message': 'Google ID token verification failed'
                    }
                }
            }
        }
    }
)
class GoogleAuthView(APIView):
    """Google OAuth 로그인/회원가입 처리"""
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        # 1. 입력 데이터 검증
        serializer = GoogleLoginSerializer(data=request.data)
        if not serializer.is_valid():
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

        id_token = serializer.validated_data['id_token']

        # 2. Google Token 검증
        google_data = GoogleOAuthUtils.verify_google_token(id_token)
        if not google_data:
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

        # 3. 사용자 처리 및 토큰 생성
        try:
            with transaction.atomic():
                user, is_new_user = self._get_or_create_user(google_data)
                user.update_last_login()

                tokens = GoogleOAuthUtils.generate_jwt_tokens(user)
                response_data = self._build_auth_response(
                    user, tokens, is_new_user
                )

                logger.info(
                    f"User {'created' if is_new_user else 'logged in'}: {user.email}"
                )
                return Response(response_data, status=status.HTTP_200_OK)

        except Exception as e:
            logger.error(f"Login error: {e}")
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

    def _create_error_response(self, error_type: str, message: str):
        """표준화된 에러 응답 생성"""
        return Response({
            'error': error_type,
            'message': message
        }, status=status.HTTP_400_BAD_REQUEST)

    def _build_auth_response(self, user, tokens: dict, is_new_user: bool) -> dict:
        """인증 성공 시 응답 데이터 구성"""
        return {
            'access_token': tokens['access_token'],
            'refresh_token': tokens['refresh_token'],
            'user_id': user.id,
            'email': user.email,
            'name': user.first_name or user.username,
            'profile_image': user.profile_image or '',
            'is_new_user': is_new_user,
            'needs_additional_info': not user.is_profile_complete
        }

    def _get_or_create_user(self, google_data: dict) -> tuple:
        """사용자 조회 또는 생성"""
        email = google_data['email']
        google_id = google_data['google_id']

        try:
            # 기존 사용자 조회
            user = User.objects.get(email=email)

            # Google ID가 없는 경우 업데이트
            if not user.google_id:
                user.google_id = google_id
                user.save(update_fields=['google_id'])

            return user, False

        except User.DoesNotExist:
            # 새 사용자 생성
            user = User.objects.create_from_google(google_data)
            return user, True


@extend_schema(
    summary="Refresh JWT Token",
    description="Refresh access token using refresh token",
    request={
        'application/json': {
            'type': 'object',
            'properties': {
                'refresh': {
                    'type': 'string',
                    'description': 'Refresh token'
                }
            },
            'required': ['refresh']
        }
    },
    responses={
        200: {
            'description': 'Token refreshed successfully',
            'content': {
                'application/json': {
                    'example': {
                        'access': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...'
                    }
                }
            }
        },
        401: {
            'description': 'Invalid or expired refresh token',
            'content': {
                'application/json': {
                    'example': {
                        'detail': 'Token is invalid or expired',
                        'code': 'token_not_valid'
                    }
                }
            }
        }
    }
)
class CustomTokenRefreshView(APIView):
    """커스텀 토큰 리프레시 뷰"""
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        # 1. Refresh 토큰 추출
        refresh_token = request.data.get('refresh')
        if not refresh_token:
            return self._create_token_error_response()

        # 2. 새 Access 토큰 생성
        try:
            refresh = RefreshToken(refresh_token)
            new_access_token = str(refresh.access_token)

            return Response({
                'access': new_access_token
            })

        except Exception as e:
            logger.error(f"Token refresh error: {e}")
            return self._create_token_error_response()

    def _create_token_error_response(self):
        """토큰 관련 에러 응답 생성"""
        return Response({
            'detail': 'Token is invalid or expired',
            'code': 'token_not_valid'
        }, status=status.HTTP_401_UNAUTHORIZED)


@extend_schema(
    summary="User Profile Management",
    description="Get or update user profile information including birth data and Saju info"
)
class UserProfileView(APIView):
    """사용자 프로필 조회/업데이트"""
    permission_classes = [DevelopmentOrAuthenticated]

    @extend_schema(
        summary="Get User Profile",
        description="Retrieve current user's profile information",
        responses={
            200: {
                'description': 'Profile retrieved successfully',
                'content': {
                    'application/json': {
                        'example': {
                            'user_id': 1,
                            'email': 'user@example.com',
                            'name': 'John Doe',
                            'profile_image': 'https://lh3.googleusercontent.com/...',
                            'nickname': 'john_doe',
                            'birth_date_solar': '1990-01-15',
                            'birth_date_lunar': None,
                            'solar_or_lunar': 'solar',
                            'birth_time_units': 3,
                            'gender': 'Male',
                            'yearly_ganji': '경오',
                            'monthly_ganji': '정축',
                            'daily_ganji': '갑자',
                            'hourly_ganji': '병인',
                            'created_at': '2024-01-01T12:00:00',
                            'last_login': '2024-01-15T14:30:00'
                        }
                    }
                }
            }
        }
    )
    def get(self, request):
        """프로필 조회"""
        # 개발환경에서 user_id 파라미터로 특정 사용자 조회 허용
        if getattr(settings, 'DEVELOPMENT_MODE', False):
            user_id = request.GET.get('user_id')
            if user_id:
                try:
                    user = User.objects.get(id=user_id)
                    profile_data = self._build_profile_response(user)
                    return Response(profile_data)
                except User.DoesNotExist:
                    return Response(
                        {'error': f'User with id {user_id} not found'},
                        status=status.HTTP_404_NOT_FOUND
                    )
            elif not request.user.is_authenticated:
                return Response(
                    {'error': 'Authentication required or provide user_id parameter in development'},
                    status=status.HTTP_401_UNAUTHORIZED
                )
        elif not request.user.is_authenticated:
            return Response(
                {'error': 'Authentication required'},
                status=status.HTTP_401_UNAUTHORIZED
            )

        user = request.user
        profile_data = self._build_profile_response(user)
        return Response(profile_data)

    def _build_profile_response(self, user) -> dict:
        """프로필 조회 응답 데이터 구성"""
        return {
            # 기본 정보
            'user_id': user.id,
            'email': user.email,
            'name': user.first_name or user.username,
            'profile_image': user.profile_image or '',
            'nickname': user.nickname,

            # 생년월일 정보
            'birth_date_solar': (
                user.birth_date_solar.isoformat()
                if user.birth_date_solar else None
            ),
            'birth_date_lunar': (
                user.birth_date_lunar.isoformat()
                if user.birth_date_lunar else None
            ),
            'solar_or_lunar': user.solar_or_lunar,
            'birth_time_units': user.birth_time_units,
            'gender': user.get_gender_display() if user.gender else None,

            # 사주 정보
            'yearly_ganji': user.yearly_ganji,
            'monthly_ganji': user.monthly_ganji,
            'daily_ganji': user.daily_ganji,
            'hourly_ganji': user.hourly_ganji,

            # 메타데이터
            'created_at': user.created_at.isoformat(),
            'last_login': (
                user.last_login.isoformat()
                if user.last_login else None
            )
        }

    @extend_schema(
        summary="Update User Profile",
        description="Update user profile information including birth data and personal info",
        request=UserProfileUpdateSerializer,
        responses={
            200: {
                'description': 'Profile updated successfully',
                'content': {
                    'application/json': {
                        'example': {
                            'message': 'Profile updated successfully',
                            'user': {
                                'user_id': 1,
                                'email': 'user@example.com',
                                'name': 'John Doe',
                                'nickname': 'john_doe',
                                'birth_date_solar': '1990-01-15',
                                'birth_date_lunar': None,
                                'solar_or_lunar': 'solar',
                                'birth_time_units': 3,
                                'gender': 'Male',
                                'yearly_ganji': '경오',
                                'monthly_ganji': '정축',
                                'daily_ganji': '갑자',
                                'hourly_ganji': '병인'
                            }
                        }
                    }
                }
            },
            400: {
                'description': 'Validation error',
                'content': {
                    'application/json': {
                        'example': {
                            'nickname': ['닉네임은 2-20자 사이여야 합니다.'],
                            'birth_date': ['유효한 날짜를 입력해주세요.']
                        }
                    }
                }
            }
        }
    )
    def patch(self, request):
        """프로필 업데이트"""
        # 1. 데이터 검증
        serializer = UserProfileUpdateSerializer(
            request.user,
            data=request.data,
            partial=True
        )

        if not serializer.is_valid():
            return Response(
                serializer.errors,
                status=status.HTTP_400_BAD_REQUEST
            )

        # 2. 프로필 업데이트
        updated_user = serializer.save()
        user_data = self._build_profile_update_response(updated_user)

        return Response({
            'message': 'Profile updated successfully',
            'user': user_data
        })

    def _build_profile_update_response(self, user) -> dict:
        """프로필 업데이트 응답 데이터 구성"""
        return {
            'user_id': user.id,
            'email': user.email,
            'name': user.first_name or user.username,
            'nickname': user.nickname,

            # 생년월일 정보
            'birth_date_solar': (
                user.birth_date_solar.isoformat()
                if user.birth_date_solar else None
            ),
            'birth_date_lunar': (
                user.birth_date_lunar.isoformat()
                if user.birth_date_lunar else None
            ),
            'solar_or_lunar': user.solar_or_lunar,
            'birth_time_units': user.birth_time_units,
            'gender': user.get_gender_display() if user.gender else None,

            # 계산된 사주 정보
            'yearly_ganji': user.yearly_ganji,
            'monthly_ganji': user.monthly_ganji,
            'daily_ganji': user.daily_ganji,
            'hourly_ganji': user.hourly_ganji
        }


@extend_schema(
    summary="User Logout",
    description="Logout user and blacklist refresh token",
    request={
        'application/json': {
            'type': 'object',
            'properties': {
                'refresh_token': {
                    'type': 'string',
                    'description': 'Refresh token to blacklist (optional)'
                }
            }
        }
    },
    responses={
        200: {
            'description': 'Logout successful',
            'content': {
                'application/json': {
                    'example': {
                        'message': 'Successfully logged out'
                    }
                }
            }
        }
    }
)
class LogoutView(APIView):
    """로그아웃 처리"""
    permission_classes = [DevelopmentOrAuthenticated]

    def post(self, request):
        # Refresh 토큰 블랙리스트 처리
        refresh_token = request.data.get('refresh_token')

        if refresh_token:
            try:
                token = RefreshToken(refresh_token)
                token.blacklist()
            except Exception as e:
                logger.error(f"Token blacklist error: {e}")
                # 에러가 발생해도 로그아웃은 성공으로 처리

        return Response({
            'message': 'Successfully logged out'
        })


