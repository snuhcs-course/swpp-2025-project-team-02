"""
Unit tests for core middleware.
"""

from django.test import TestCase, RequestFactory, override_settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import AnonymousUser
from core.middleware import TestAuthenticationMiddleware
from django.http import Http404

User = get_user_model()


class TestAuthenticationMiddlewareTestCase(TestCase):
    """Test cases for TestAuthenticationMiddleware."""

    def setUp(self):
        """Set up test fixtures."""
        self.factory = RequestFactory()
        self.middleware = TestAuthenticationMiddleware(lambda r: None)

        # 테스트 유저 생성
        self.test_user = User.objects.create_user(
            email='testuser@example.com',
            password='testpass123'
        )

    @override_settings(DEVELOPMENT_MODE=True)
    def test_auth_bypass_with_valid_user_id(self):
        """X-Test-User-Id 헤더로 유효한 유저 인증 우회"""
        request = self.factory.get('/', HTTP_X_TEST_USER_ID=str(self.test_user.id))
        request.user = AnonymousUser()

        self.middleware(request)

        self.assertEqual(request.user, self.test_user)
        self.assertTrue(request.user.is_authenticated)

    @override_settings(DEVELOPMENT_MODE=True)
    def test_auth_bypass_with_invalid_user_id(self):
        """존재하지 않는 user_id로 요청 시 인증 실패"""
        request = self.factory.get('/', HTTP_X_TEST_USER_ID='99999')
        request.user = AnonymousUser()

        # Http404 Error
        with self.assertRaises(Http404):
            self.middleware(request)

    @override_settings(DEVELOPMENT_MODE=True)
    def test_auth_bypass_with_non_numeric_id(self):
        """숫자가 아닌 user_id로 요청 시 인증 실패"""
        request = self.factory.get('/', HTTP_X_TEST_USER_ID='invalid')
        request.user = AnonymousUser()

        self.middleware(request)

        self.assertIsInstance(request.user, AnonymousUser)

    @override_settings(DEVELOPMENT_MODE=True)
    def test_no_header_provided(self):
        """X-Test-User-Id 헤더 없이 요청 시 기존 흐름 유지"""
        request = self.factory.get('/')
        request.user = AnonymousUser()

        self.middleware(request)

        self.assertIsInstance(request.user, AnonymousUser)

    @override_settings(DEVELOPMENT_MODE=False, TESTING_MODE=False)
    def test_middleware_disabled_in_production(self):
        """프로덕션 환경에서는 미들웨어 비활성화"""
        # 프로덕션 환경용 새로운 미들웨어 인스턴스 생성
        middleware = TestAuthenticationMiddleware(lambda r: None)
        request = self.factory.get('/', HTTP_X_TEST_USER_ID=str(self.test_user.id))
        request.user = AnonymousUser()

        middleware(request)

        # 프로덕션에서는 헤더가 있어도 인증 우회 안 됨
        self.assertIsInstance(request.user, AnonymousUser)

    @override_settings(DEVELOPMENT_MODE=True)
    def test_multiple_users(self):
        """여러 유저로 인증 우회 테스트"""
        user2 = User.objects.create_user(
            email='user2@example.com',
            password='testpass123'
        )

        # 첫 번째 유저
        request1 = self.factory.get('/', HTTP_X_TEST_USER_ID=str(self.test_user.id))
        request1.user = AnonymousUser()
        self.middleware(request1)
        self.assertEqual(request1.user.id, self.test_user.id)

        # 두 번째 유저
        request2 = self.factory.get('/', HTTP_X_TEST_USER_ID=str(user2.id))
        request2.user = AnonymousUser()
        self.middleware(request2)
        self.assertEqual(request2.user.id, user2.id)
