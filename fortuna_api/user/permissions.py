"""
User app permission utilities.
"""

from django.conf import settings
from rest_framework import permissions


def get_permission_classes():
    """
    개발환경과 프로덕션환경에 따라 다른 권한 클래스를 반환합니다.

    개발환경: AllowAny (인증 없이 접근 가능)
    프로덕션환경: IsAuthenticated (인증 필요)
    """
    if getattr(settings, 'DEVELOPMENT_MODE', False):
        return [permissions.AllowAny]
    return [permissions.IsAuthenticated]


class DevelopmentOrAuthenticated(permissions.BasePermission):
    """
    개발환경에서는 모든 접근 허용, 프로덕션에서는 인증 필요
    """

    def has_permission(self, request, view):
        if getattr(settings, 'DEVELOPMENT_MODE', False):
            return True
        return request.user and request.user.is_authenticated