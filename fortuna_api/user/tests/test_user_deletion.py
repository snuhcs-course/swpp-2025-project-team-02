"""
사용자 탈퇴 기능 유닛 테스트
"""

from django.test import TestCase
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import timedelta

User = get_user_model()


class UserModelDeletionTests(TestCase):
    """User 모델의 soft delete 기능 테스트"""

    def setUp(self):
        """테스트 환경 설정"""
        self.user = User.objects.create_user(
            email='test@example.com',
            first_name='Test User',
            nickname='테스트'
        )

    def test_soft_delete_sets_deleted_at(self):
        """soft_delete() 호출 시 deleted_at이 설정되는지 테스트"""
        # deleted_at이 None인지 확인
        self.assertIsNone(self.user.deleted_at)

        # soft_delete 실행
        self.user.soft_delete()
        self.user.save()

        # deleted_at이 설정되었는지 확인
        self.assertIsNotNone(self.user.deleted_at)
        self.assertIsInstance(self.user.deleted_at, timezone.datetime)

    def test_is_deleted_returns_false_for_active_user(self):
        """활성 사용자에 대해 is_deleted()가 False를 반환하는지 테스트"""
        self.assertFalse(self.user.is_deleted())

    def test_is_deleted_returns_true_for_deleted_user(self):
        """탈퇴한 사용자에 대해 is_deleted()가 True를 반환하는지 테스트"""
        self.user.soft_delete()
        self.user.save()

        self.assertTrue(self.user.is_deleted())

    def test_deleted_at_timestamp_is_recent(self):
        """deleted_at 타임스탬프가 현재 시간과 가까운지 테스트"""
        before_deletion = timezone.now()
        self.user.soft_delete()
        self.user.save()
        after_deletion = timezone.now()

        self.assertGreaterEqual(self.user.deleted_at, before_deletion)
        self.assertLessEqual(self.user.deleted_at, after_deletion)


class UserManagerDeletionTests(TestCase):
    """UserManager의 soft delete 필터링 테스트"""

    def setUp(self):
        """테스트 환경 설정"""
        # 활성 사용자 생성
        self.active_user1 = User.objects.create_user(
            email='active1@example.com',
            first_name='Active User 1',
            nickname='active1'
        )
        self.active_user2 = User.objects.create_user(
            email='active2@example.com',
            first_name='Active User 2',
            nickname='active2'
        )

        # 탈퇴한 사용자 생성
        self.deleted_user = User.objects.create_user(
            email='deleted@example.com',
            first_name='Deleted User',
            nickname='탈퇴자'
        )
        self.deleted_user.soft_delete()
        self.deleted_user.save()

    def test_objects_manager_excludes_deleted_users(self):
        """기본 objects 매니저가 탈퇴한 사용자를 제외하는지 테스트"""
        active_users = User.objects.all()

        self.assertEqual(active_users.count(), 2)
        self.assertIn(self.active_user1, active_users)
        self.assertIn(self.active_user2, active_users)
        self.assertNotIn(self.deleted_user, active_users)

    def test_all_objects_manager_includes_deleted_users(self):
        """all_objects 매니저가 탈퇴한 사용자를 포함하는지 테스트"""
        all_users = User.all_objects.all()

        self.assertEqual(all_users.count(), 3)
        self.assertIn(self.active_user1, all_users)
        self.assertIn(self.active_user2, all_users)
        self.assertIn(self.deleted_user, all_users)

    def test_objects_filter_by_email_excludes_deleted(self):
        """이메일로 필터링 시 탈퇴한 사용자가 제외되는지 테스트"""
        result = User.objects.filter(email='deleted@example.com')

        self.assertEqual(result.count(), 0)

    def test_all_objects_filter_by_email_includes_deleted(self):
        """all_objects로 이메일 필터링 시 탈퇴한 사용자를 찾을 수 있는지 테스트"""
        result = User.all_objects.filter(email='deleted@example.com')

        self.assertEqual(result.count(), 1)
        self.assertEqual(result.first(), self.deleted_user)

    def test_get_method_raises_exception_for_deleted_user(self):
        """objects.get()으로 탈퇴한 사용자 조회 시 예외 발생 테스트"""
        with self.assertRaises(User.DoesNotExist):
            User.objects.get(email='deleted@example.com')

    def test_all_objects_get_method_finds_deleted_user(self):
        """all_objects.get()으로 탈퇴한 사용자 조회 가능 테스트"""
        user = User.all_objects.get(email='deleted@example.com')

        self.assertEqual(user, self.deleted_user)
        self.assertTrue(user.is_deleted())


class UserReRegistrationTests(TestCase):
    """탈퇴한 사용자의 재가입 시나리오 테스트"""

    def test_deleted_user_email_can_be_reused_after_email_change(self):
        """탈퇴한 사용자의 이메일을 변경하면 동일 이메일로 새 계정 생성 가능"""
        # 첫 번째 사용자 생성 및 탈퇴
        original_user = User.objects.create_user(
            email='reuse@example.com',
            first_name='Original User',
            nickname='원래자'
        )
        original_user.soft_delete()
        original_user.save()

        # 탈퇴한 사용자의 이메일과 username 변경
        new_email = f"{original_user.email}_deleted_{original_user.id}"
        original_user.email = new_email
        original_user.username = new_email
        original_user.save()

        # 동일한 이메일로 새 사용자 생성 가능
        new_user = User.objects.create_user(
            email='reuse@example.com',
            first_name='New User',
            nickname='새사용'
        )

        self.assertNotEqual(original_user.id, new_user.id)
        self.assertEqual(new_user.email, 'reuse@example.com')
        self.assertFalse(new_user.is_deleted())

    def test_multiple_users_with_deleted_suffix(self):
        """여러 번 탈퇴/재가입 시 이메일 충돌이 없는지 테스트"""
        email = 'multiple@example.com'

        # 첫 번째 사용자
        user1 = User.objects.create_user(
            email=email,
            first_name='User 1',
            nickname='user1'
        )
        user1.soft_delete()
        user1_new_email = f"{email}_deleted_{user1.id}"
        user1.email = user1_new_email
        user1.username = user1_new_email
        user1.save()

        # 두 번째 사용자
        user2 = User.objects.create_user(
            email=email,
            first_name='User 2',
            nickname='user2'
        )
        user2.soft_delete()
        user2_new_email = f"{email}_deleted_{user2.id}"
        user2.email = user2_new_email
        user2.username = user2_new_email
        user2.save()

        # 세 번째 사용자
        user3 = User.objects.create_user(
            email=email,
            first_name='User 3',
            nickname='user3'
        )

        # 모든 사용자가 서로 다른 이메일을 가지는지 확인
        all_users = User.all_objects.filter(
            email__contains='multiple@example.com'
        )
        emails = [user.email for user in all_users]

        self.assertEqual(len(emails), 3)
        self.assertEqual(len(set(emails)), 3)  # 중복 없음
        self.assertIn(email, emails)  # 현재 활성 사용자가 원래 이메일 사용
