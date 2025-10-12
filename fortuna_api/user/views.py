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
        # 1. 입력 데이터 검증
        serializer = GoogleLoginSerializer(data=request.data)
        if not serializer.is_valid():
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

        google_id_token = serializer.validated_data['id_token']

        # 2. Google Token 검증
        google_user_data = GoogleOAuthUtils.verify_and_extract_google_user_info(google_id_token)
        if not google_user_data:
            return self._create_error_response(
                'Invalid token',
                'Google ID token verification failed'
            )

        # 3. 사용자 처리 및 토큰 생성
        try:
            with transaction.atomic():
                user, is_new_user = self._get_or_create_user_from_google(google_user_data)
                user.update_last_login()

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
        """Google OAuth 데이터로부터 사용자 조회 또는 생성"""
        user_email = google_user_data['email']
        user_google_id = google_user_data['google_id']

        try:
            # 기존 사용자 조회
            user = User.objects.get(email=user_email)

            # Google ID가 없는 경우 업데이트
            if not user.google_id:
                user.google_id = user_google_id
                user.save(update_fields=['google_id'])

            return user, False

        except User.DoesNotExist:
            # 새 사용자 생성
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
        # 1. Refresh 토큰 추출
        refresh_token_string = request.data.get('refresh')
        if not refresh_token_string:
            return self._create_token_error_response()

        # 2. 새 Access 토큰 생성
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
        # 1. 토큰 추출
        access_token_string = request.data.get('token')
        if not access_token_string:
            return Response({
                'error': 'Missing token',
                'message': 'Token field is required'
            }, status=status.HTTP_400_BAD_REQUEST)

        # 2. 토큰 검증
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
        # Refresh 토큰 블랙리스트 처리
        refresh_token_string = request.data.get('refresh_token')

        if refresh_token_string:
            try:
                refresh_token = RefreshToken(refresh_token_string)
                refresh_token.blacklist()
            except Exception as error:
                logger.error(f"Token blacklist error: {error}")
                # 에러가 발생해도 로그아웃은 성공으로 처리

        return Response({
            'message': 'Successfully logged out'
        })


