"""
Core app middleware.
"""

from django.contrib.auth import get_user_model
from django.conf import settings
import logging

from django.http import Http404
from rest_framework import status
from rest_framework.response import Response

logger = logging.getLogger(__name__)
User = get_user_model()


class TestAuthenticationMiddleware:
    """
    테스트 환경에서 X-Test-User-Id 헤더를 통한 인증 우회를 지원하는 미들웨어.

    프로덕션 환경에서는 비활성화됩니다.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # 테스트 환경이 아니면 바로 통과
        if not self._is_test_mode():
            return self.get_response(request)

        # X-Test-User-Id 헤더 확인
        test_user_id = request.META.get('HTTP_X_TEST_USER_ID')

        if test_user_id:
            try:
                user = User.objects.get(id=int(test_user_id))
                request.user = user  # User 객체 할당 시 is_authenticated는 자동으로 True
                request._cached_user = user  # Django 인증 미들웨어 캐시 업데이트
                logger.debug(f"Test auth: User {user.id} authenticated via X-Test-User-Id header")
            except User.DoesNotExist as e:
                logger.warning(f"Test auth failed: {e}")
                raise Http404
            except ValueError as e:
                logger.warning(f"Test auth failed: {e}")
                # 인증 실패 시 기존 흐름 유지 (AnonymousUser)

        return self.get_response(request)

    def _is_test_mode(self) -> bool:
        """개발 모드 확인"""
        # 개발 환경에서만 활성화
        return getattr(settings, 'DEVELOPMENT_MODE', False) or getattr(settings, 'TESTING_MODE', False)
