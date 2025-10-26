from rest_framework import status, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken, UntypedToken
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
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
    request={
        'application/json': {
            'type': 'object',
            'properties': {
                'id_token': {
                    'type': 'string',
                    'description': 'Google OAuth ID token',
                    'example': 'eyJhbGciOiJSUzI1NiIsImtpZCI6IjdkYzBiZGVjNjJhZGE5Njc4NDY0YjA0YzBmYzY0MTg3Y2Y2YmRkYjMiLCJ0eXAiOiJKV1QifQ...'
                }
            },
            'required': ['id_token'],
            'example': {
                'id_token': 'eyJhbGciOiJSUzI1NiIsImtpZCI6IjdkYzBiZGVjNjJhZGE5Njc4NDY0YjA0YzBmYzY0MTg3Y2Y2YmRkYjMiLCJ0eXAiOiJKV1QifQ...'
            }
        }
    },
    responses={
        200: {
            'description': 'Authentication successful',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'access_token': {
                                'type': 'string',
                                'description': 'JWT access token'
                            },
                            'refresh_token': {
                                'type': 'string',
                                'description': 'JWT refresh token'
                            },
                            'user_id': {
                                'type': 'integer',
                                'description': 'User ID'
                            },
                            'email': {
                                'type': 'string',
                                'description': 'User email'
                            },
                            'name': {
                                'type': 'string',
                                'description': 'User display name'
                            },
                            'profile_image': {
                                'type': 'string',
                                'description': 'Google profile image URL'
                            },
                            'is_new_user': {
                                'type': 'boolean',
                                'description': 'Whether this is a newly registered user'
                            },
                            'needs_additional_info': {
                                'type': 'boolean',
                                'description': 'Whether user needs to complete profile'
                            }
                        },
                        'required': ['access_token', 'refresh_token', 'user_id', 'email', 'name', 'is_new_user', 'needs_additional_info']
                    },
                    'example': {
                        'access_token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzA2NzkyNjc0fQ...',
                        'refresh_token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTcwNjc5MjY3NH0...',
                        'user_id': 1,
                        'email': 'user@example.com',
                        'name': 'John Doe',
                        'profile_image': 'https://lh3.googleusercontent.com/a-/AOh14GhXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX',
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
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'error': {'type': 'string'},
                            'message': {'type': 'string'}
                        }
                    },
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
        """
        Google OAuth 로그인 처리

        1. 클라이언트가 전송한 Google ID token 검증
        2. Google 사용자 정보로 DB 조회 또는 신규 사용자 생성
        3. JWT 토큰 페어 생성 및 반환
        """
        # 입력 데이터 검증
        validation_result = self._validate_request_data(request.data)
        if validation_result:
            return validation_result

        # Google Token 검증 및 사용자 정보 추출
        google_user_data = self._verify_google_token(request.data['id_token'])
        if not google_user_data:
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

        # 사용자 조회/생성 및 JWT 토큰 발급
        return self._process_user_login(google_user_data)

    def _validate_request_data(self, data):
        """요청 데이터 검증"""
        serializer = GoogleLoginSerializer(data=data)
        if not serializer.is_valid():
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )
        return None

    def _verify_google_token(self, google_id_token: str):
        """Google ID Token 검증"""
        return GoogleOAuthUtils.verify_google_token(google_id_token)

    def _process_user_login(self, google_user_data: dict):
        """
        사용자 로그인 처리

        사용자 조회/생성, 로그인 시간 업데이트, JWT 토큰 발급을 트랜잭션으로 처리
        """
        try:
            with transaction.atomic():
                # 기존 사용자 조회 또는 신규 사용자 생성
                user, is_new_user = self._get_or_create_user_from_google(google_user_data)

                # 마지막 로그인 시간 업데이트
                user.update_last_login()
                user.save(update_fields=['last_login'])

                # JWT 토큰 페어 생성 및 응답 구성
                jwt_token_pair = GoogleOAuthUtils.generate_jwt_token_pair(user)
                response_data = self._build_auth_response(
                    user, jwt_token_pair, is_new_user
                )

                logger.info(
                    f"User {'created' if is_new_user else 'logged in'}: {user.email}"
                )
                return Response(response_data, status=status.HTTP_200_OK)

        except Exception as error:
            logger.error(f"Login error: {error}")
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

    def _build_auth_response(self, user, jwt_token_pair: dict, is_new_user: bool) -> dict:
        """인증 성공 시 응답 데이터 구성"""
        return {
            'access_token': jwt_token_pair['access_token'],
            'refresh_token': jwt_token_pair['refresh_token'],
            'user_id': user.id,
            'email': user.email,
            'name': user.first_name or user.username,
            'profile_image': user.profile_image or '',
            'is_new_user': is_new_user,
            'needs_additional_info': not user.is_profile_complete
        }

    def _get_or_create_user_from_google(self, google_user_data: dict) -> tuple:
        """
        Google OAuth 데이터로부터 사용자 조회 또는 생성

        탈퇴한 사용자가 재가입하는 경우 새로운 계정 생성
        (과거 데이터와 완전히 분리)

        Returns:
            tuple: (User 객체, 신규 생성 여부)
        """
        user_email = google_user_data['email']
        user_google_id = google_user_data['google_id']

        # 탈퇴한 사용자를 포함하여 조회
        deleted_user = User.all_objects.filter(
            email=user_email,
            deleted_at__isnull=False
        ).first()

        # 탈퇴한 사용자가 재가입하는 경우
        if deleted_user:
            with transaction.atomic():
                # 기존 이메일과 username을 변경하여 새 계정이 사용할 수 있도록 함
                new_email = f"{deleted_user.email}_deleted_{deleted_user.id}"
                deleted_user.email = new_email
                deleted_user.username = new_email  # username도 email과 동일하게 변경
                deleted_user.google_id = None  # Google ID도 해제
                deleted_user.save(update_fields=['email', 'username', 'google_id'])

                # 새 계정 생성
                user = User.objects.create_user_from_google_oauth(google_user_data)
                return user, True

        try:
            # 활성 사용자 조회
            user = User.objects.get(email=user_email)

            # Google ID가 없는 경우 업데이트 (초기 이메일 가입자가 나중에 Google 로그인 사용)
            if not user.google_id:
                user.google_id = user_google_id
                user.save(update_fields=['google_id'])

            return user, False

        except User.DoesNotExist:
            # 신규 사용자 생성
            user = User.objects.create_user_from_google_oauth(google_user_data)
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
                    'description': 'Refresh token',
                    'example': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTcwNjc5MjY3NH0...'
                }
            },
            'required': ['refresh'],
            'example': {
                'refresh': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTcwNjc5MjY3NH0...'
            }
        }
    },
    responses={
        200: {
            'description': 'Token refreshed successfully',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'access': {
                                'type': 'string',
                                'description': 'New access token'
                            }
                        },
                        'required': ['access']
                    },
                    'example': {
                        'access': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzA2NzkyNjc0fQ...'
                    }
                }
            }
        },
        400: {
            'description': 'Missing refresh token',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'detail': {'type': 'string'},
                            'code': {'type': 'string'}
                        }
                    },
                    'example': {
                        'detail': 'Token is invalid or expired',
                        'code': 'token_not_valid'
                    }
                }
            }
        },
        401: {
            'description': 'Invalid or expired refresh token',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'detail': {'type': 'string'},
                            'code': {'type': 'string'}
                        }
                    },
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
        """
        JWT 토큰 갱신

        Refresh 토큰을 이용해 새로운 Access 토큰 발급
        """
        # Refresh 토큰 추출
        refresh_token_string = request.data.get('refresh')
        if not refresh_token_string:
            return self._create_token_error_response()

        # Refresh 토큰을 사용하여 새 Access 토큰 생성
        try:
            refresh_token = RefreshToken(refresh_token_string)
            new_access_token = str(refresh_token.access_token)

            return Response({
                'access': new_access_token
            })

        except Exception as error:
            logger.error(f"Token refresh error: {error}")
            return self._create_token_error_response()

    def _create_token_error_response(self):
        """토큰 관련 에러 응답 생성"""
        return Response({
            'detail': 'Token is invalid or expired',
            'code': 'token_not_valid'
        }, status=status.HTTP_401_UNAUTHORIZED)


@extend_schema(
    summary="Verify JWT Token",
    description="Verify the validity of a JWT access token",
    request={
        'application/json': {
            'type': 'object',
            'properties': {
                'token': {
                    'type': 'string',
                    'description': 'JWT access token to verify',
                    'example': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzA2NzkyNjc0fQ...'
                }
            },
            'required': ['token'],
            'example': {
                'token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzA2NzkyNjc0fQ...'
            }
        }
    },
    responses={
        200: {
            'description': 'Token is valid',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'message': {
                                'type': 'string',
                                'description': 'Verification success message'
                            },
                            'valid': {
                                'type': 'boolean',
                                'description': 'Token validity status'
                            }
                        }
                    },
                    'example': {
                        'message': 'Token is valid',
                        'valid': True
                    }
                }
            }
        },
        400: {
            'description': 'Missing token',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'error': {'type': 'string'},
                            'message': {'type': 'string'}
                        }
                    },
                    'example': {
                        'error': 'Missing token',
                        'message': 'Token field is required'
                    }
                }
            }
        },
        200: {
            'description': 'Token verification result (invalid)',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'message': {'type': 'string'},
                            'valid': {'type': 'boolean'}
                        }
                    },
                    'example': {
                        'message': 'Token is invalid or expired',
                        'valid': False
                    }
                }
            }
        }
    }
)
class CustomTokenVerifyView(APIView):
    """커스텀 토큰 검증 뷰"""
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        """
        JWT 토큰 유효성 검증

        토큰의 서명, 만료 시간 등을 검증하여 유효 여부 반환
        """
        # 토큰 추출
        access_token_string = request.data.get('token')
        if not access_token_string:
            return Response({
                'error': 'Missing token',
                'message': 'Token field is required'
            }, status=status.HTTP_400_BAD_REQUEST)

        # 토큰 검증 (서명, 만료 시간 등)
        try:
            UntypedToken(access_token_string)
            return Response({
                'message': 'Token is valid',
                'valid': True
            }, status=status.HTTP_200_OK)
        except (InvalidToken, TokenError) as error:
            return Response({
                'message': 'Token is invalid or expired',
                'valid': False
            }, status=status.HTTP_200_OK)


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
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'user_id': {'type': 'integer', 'description': 'User ID'},
                                'email': {'type': 'string', 'format': 'email', 'description': 'User email'},
                                'name': {'type': 'string', 'description': 'User display name'},
                                'profile_image': {'type': 'string', 'format': 'uri', 'description': 'Profile image URL'},
                                'nickname': {'type': 'string', 'description': 'User nickname'},
                                'birth_date_solar': {'type': 'string', 'format': 'date', 'description': 'Solar calendar birth date'},
                                'birth_date_lunar': {'type': 'string', 'format': 'date', 'description': 'Lunar calendar birth date', 'nullable': True},
                                'solar_or_lunar': {'type': 'string', 'enum': ['solar', 'lunar'], 'description': 'Calendar type'},
                                'birth_time_units': {'type': 'integer', 'minimum': 1, 'maximum': 12, 'description': 'Birth time in 12 hour units'},
                                'gender': {'type': 'string', 'enum': ['Male', 'Female'], 'description': 'User gender'},
                                'yearly_ganji': {'type': 'string', 'description': 'Yearly Ganji (연간)', 'nullable': True},
                                'monthly_ganji': {'type': 'string', 'description': 'Monthly Ganji (월간)', 'nullable': True},
                                'daily_ganji': {'type': 'string', 'description': 'Daily Ganji (일간)', 'nullable': True},
                                'hourly_ganji': {'type': 'string', 'description': 'Hourly Ganji (시간)', 'nullable': True},
                                'created_at': {'type': 'string', 'format': 'date-time', 'description': 'Account creation date'},
                                'last_login': {'type': 'string', 'format': 'date-time', 'description': 'Last login date', 'nullable': True}
                            },
                            'required': ['user_id', 'email', 'name', 'created_at']
                        },
                        'example': {
                            'user_id': 1,
                            'email': 'user@example.com',
                            'name': 'John Doe',
                            'profile_image': 'https://lh3.googleusercontent.com/a-/AOh14GhXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX',
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
                            'created_at': '2024-01-01T12:00:00Z',
                            'last_login': '2024-01-15T14:30:00Z'
                        }
                    }
                }
            },
            401: {
                'description': 'Authentication required',
                'content': {
                    'application/json': {
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'error': {'type': 'string'}
                            }
                        },
                        'example': {
                            'error': 'Authentication required'
                        }
                    }
                }
            },
            404: {
                'description': 'User not found (development mode with user_id)',
                'content': {
                    'application/json': {
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'error': {'type': 'string'}
                            }
                        },
                        'example': {
                            'error': 'User with id 999 not found'
                        }
                    }
                }
            }
        }
    )
    def get(self, request):
        """프로필 조회"""
        # 사용자 확인
        user = self._get_user_for_profile(request)
        if isinstance(user, Response):
            return user

        # 프로필 응답 생성
        profile_data = self._build_profile_response(user)
        return Response(profile_data)

    def _get_user_for_profile(self, request):
        """
        프로필 조회를 위한 사용자 확인

        개발환경에서는 user_id 파라미터로 다른 사용자 조회 가능
        프로덕션환경에서는 본인의 프로필만 조회 가능
        """
        # 개발 모드: user_id 파라미터를 통한 다른 사용자 조회 허용
        if getattr(settings, 'DEVELOPMENT_MODE', False):
            return self._handle_development_mode_user(request)

        # 프로덕션 모드: 인증된 사용자만 본인의 프로필 조회
        if not request.user.is_authenticated:
            return Response(
                {'error': 'Authentication required'},
                status=status.HTTP_401_UNAUTHORIZED
            )

        return request.user

    def _handle_development_mode_user(self, request):
        """
        개발 모드에서 사용자 처리

        user_id 파라미터가 있으면 해당 사용자 조회
        없으면 인증된 현재 사용자 사용
        """
        user_id = request.GET.get('user_id')

        # user_id 파라미터가 주어진 경우 해당 사용자 조회
        if user_id:
            return self._get_user_by_id(user_id)

        # user_id가 없으면 인증된 사용자 사용
        if not request.user.is_authenticated:
            return Response(
                {'error': 'Authentication required or provide user_id parameter in development'},
                status=status.HTTP_401_UNAUTHORIZED
            )

        return request.user

    def _get_user_by_id(self, user_id: str):
        """ID로 사용자 조회"""
        try:
            return User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response(
                {'error': f'User with id {user_id} not found'},
                status=status.HTTP_404_NOT_FOUND
            )

    def _build_profile_response(self, user) -> dict:
        """프로필 조회 응답 데이터 구성"""
        return {
            **self._get_basic_user_info(user),
            **self._get_birth_date_info(user),
            **self._get_saju_info(user),
            **self._get_metadata(user),
            'collection_status': self._get_collection_status(user)
        }

    def _get_basic_user_info(self, user) -> dict:
        """기본 사용자 정보 추출"""
        return {
            'user_id': user.id,
            'email': user.email,
            'name': user.first_name or user.username,
            'profile_image': user.profile_image or '',
            'nickname': user.nickname,
        }

    def _get_birth_date_info(self, user) -> dict:
        """생년월일 정보 추출"""
        return {
            'birth_date_solar': self._format_date(user.birth_date_solar),
            'birth_date_lunar': self._format_date(user.birth_date_lunar),
            'solar_or_lunar': user.solar_or_lunar,
            'birth_time_units': user.birth_time_units,
            'gender': user.get_gender_display() if user.gender else None,
        }

    @staticmethod
    def _format_date(date_value) -> str:
        """날짜를 ISO 형식 문자열로 변환 (None-safe)"""
        return date_value.isoformat() if date_value else None

    def _get_saju_info(self, user) -> dict:
        """사주 정보 추출"""
        return {
            'yearly_ganji': user.yearly_ganji,
            'monthly_ganji': user.monthly_ganji,
            'daily_ganji': user.daily_ganji,
            'hourly_ganji': user.hourly_ganji,
        }

    def _get_metadata(self, user) -> dict:
        """메타데이터 추출"""
        return {
            'created_at': user.created_at.isoformat(),
            'last_login': self._format_date(user.last_login)
        }

    def _get_collection_status(self, user) -> dict:
        """사용자의 chakra 수집 현황 조회"""
        from core.models import ChakraImage
        from django.db.models import Count

        # core/views.py의 collection_status와 동일한 로직
        collections = ChakraImage.objects.filter(
            user=user
        ).values('chakra_type').annotate(
            count=Count('id')
        ).order_by('chakra_type')

        total_count = sum(item['count'] for item in collections)

        return {
            'collections': list(collections),
            'total_count': total_count
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
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'message': {'type': 'string', 'description': 'Success message'},
                                'user': {
                                    'type': 'object',
                                    'properties': {
                                        'user_id': {'type': 'integer', 'description': 'User ID'},
                                        'email': {'type': 'string', 'format': 'email', 'description': 'User email'},
                                        'name': {'type': 'string', 'description': 'User display name'},
                                        'nickname': {'type': 'string', 'description': 'User nickname'},
                                        'birth_date_solar': {'type': 'string', 'format': 'date', 'description': 'Solar calendar birth date', 'nullable': True},
                                        'birth_date_lunar': {'type': 'string', 'format': 'date', 'description': 'Lunar calendar birth date', 'nullable': True},
                                        'solar_or_lunar': {'type': 'string', 'enum': ['solar', 'lunar'], 'description': 'Calendar type'},
                                        'birth_time_units': {'type': 'integer', 'minimum': 1, 'maximum': 12, 'description': 'Birth time in 12 hour units', 'nullable': True},
                                        'gender': {'type': 'string', 'enum': ['Male', 'Female'], 'description': 'User gender', 'nullable': True},
                                        'yearly_ganji': {'type': 'string', 'description': 'Yearly Ganji (연간)', 'nullable': True},
                                        'monthly_ganji': {'type': 'string', 'description': 'Monthly Ganji (월간)', 'nullable': True},
                                        'daily_ganji': {'type': 'string', 'description': 'Daily Ganji (일간)', 'nullable': True},
                                        'hourly_ganji': {'type': 'string', 'description': 'Hourly Ganji (시간)', 'nullable': True}
                                    },
                                    'required': ['user_id', 'email', 'name']
                                }
                            },
                            'required': ['message', 'user']
                        },
                        'example': {
                            'message': 'Profile updated successfully',
                            'user': {
                                'user_id': 1,
                                'email': 'user@example.com',
                                'name': 'John Doe',
                                'nickname': 'john_doe_updated',
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
                        'schema': {
                            'type': 'object',
                            'additionalProperties': {
                                'type': 'array',
                                'items': {'type': 'string'}
                            }
                        },
                        'example': {
                            'nickname': ['닉네임은 2-20자 사이여야 합니다.'],
                            'birth_date': ['유효한 날짜를 입력해주세요.']
                        }
                    }
                }
            },
            401: {
                'description': 'Authentication required',
                'content': {
                    'application/json': {
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'error': {'type': 'string'}
                            }
                        },
                        'example': {
                            'error': 'Authentication required'
                        }
                    }
                }
            }
        }
    )
    def patch(self, request):
        """
        사용자 프로필 업데이트

        닉네임, 생년월일, 성별 등의 프로필 정보를 업데이트하고
        사주팔자를 자동으로 계산하여 저장
        """
        # 입력 데이터 검증
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

        # 프로필 업데이트 (사주 계산 포함)
        updated_user = serializer.save()
        user_data = self._build_profile_update_response(updated_user)

        return Response({
            'message': 'Profile updated successfully',
            'user': user_data
        })

    def _build_profile_update_response(self, user) -> dict:
        """프로필 업데이트 응답 데이터 구성"""
        return {
            **self._get_basic_user_info(user),
            **self._get_birth_date_info(user),
            **self._get_saju_info(user)
        }


@extend_schema(
    summary="User Account Deletion",
    description="Soft delete user account by setting deleted_at timestamp"
)
class UserDeletionView(APIView):
    """사용자 계정 탈퇴 (soft delete)"""
    permission_classes = [DevelopmentOrAuthenticated]

    @extend_schema(
        summary="Delete User Account",
        description="Soft delete the current user's account. Deleted users can re-register with a new account using the same Google account.",
        responses={
            200: {
                'description': 'Account deleted successfully',
                'content': {
                    'application/json': {
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'message': {
                                    'type': 'string',
                                    'description': 'Success message'
                                },
                                'deleted_at': {
                                    'type': 'string',
                                    'format': 'date-time',
                                    'description': 'Deletion timestamp'
                                }
                            },
                            'required': ['message', 'deleted_at']
                        },
                        'example': {
                            'message': 'Account deleted successfully',
                            'deleted_at': '2024-01-15T14:30:00Z'
                        }
                    }
                }
            },
            401: {
                'description': 'Authentication required',
                'content': {
                    'application/json': {
                        'schema': {
                            'type': 'object',
                            'properties': {
                                'error': {'type': 'string'}
                            }
                        },
                        'example': {
                            'error': 'Authentication required'
                        }
                    }
                }
            }
        }
    )
    def delete(self, request):
        """
        사용자 계정 탈퇴 처리

        soft delete 방식으로 deleted_at 필드에 현재 시간 기록
        탈퇴한 사용자는 동일 Google 계정으로 재가입 가능 (새 계정 생성)
        """
        user = request.user

        # 이미 탈퇴한 사용자인지 확인
        if user.is_deleted():
            return Response({
                'error': 'Account already deleted',
                'message': 'This account has already been deleted'
            }, status=status.HTTP_400_BAD_REQUEST)

        # Soft delete 처리
        with transaction.atomic():
            user.soft_delete()
            user.save(update_fields=['deleted_at'])

            logger.info(f"User deleted: {user.email} (ID: {user.id})")

        return Response({
            'message': 'Account deleted successfully',
            'deleted_at': user.deleted_at.isoformat()
        }, status=status.HTTP_200_OK)


@extend_schema(
    summary="User Logout",
    description="Logout user and blacklist refresh token",
    request={
        'application/json': {
            'type': 'object',
            'properties': {
                'refresh_token': {
                    'type': 'string',
                    'description': 'Refresh token to blacklist (optional)',
                    'example': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTcwNjc5MjY3NH0...'
                }
            },
            'example': {
                'refresh_token': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTcwNjc5MjY3NH0...'
            }
        }
    },
    responses={
        200: {
            'description': 'Logout successful',
            'content': {
                'application/json': {
                    'schema': {
                        'type': 'object',
                        'properties': {
                            'message': {
                                'type': 'string',
                                'description': 'Success message'
                            }
                        },
                        'required': ['message']
                    },
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
        """
        사용자 로그아웃

        Refresh 토큰을 블랙리스트에 추가하여 재사용 방지
        블랙리스트 실패 시에도 로그아웃 성공으로 처리 (클라이언트에서 토큰 삭제)
        """
        # Refresh 토큰 블랙리스트 처리
        refresh_token_string = request.data.get('refresh_token')

        if refresh_token_string:
            try:
                refresh_token = RefreshToken(refresh_token_string)
                refresh_token.blacklist()
            except Exception as error:
                logger.error(f"Token blacklist error: {error}")
                # 블랙리스트 실패해도 로그아웃은 성공으로 처리 (클라이언트가 토큰을 삭제하므로)

        return Response({
            'message': 'Successfully logged out'
        })


