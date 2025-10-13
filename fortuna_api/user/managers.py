from django.contrib.auth.models import BaseUserManager
from django.db import models

class ActiveUserManager(BaseUserManager):
    """활성 사용자만 조회하는 매니저 (탈퇴한 사용자 제외)"""

    def get_queryset(self):
        """탈퇴하지 않은 사용자만 조회"""
        return super().get_queryset().filter(deleted_at__isnull=True)


class UserManager(ActiveUserManager):
    """커스텀 사용자 매니저"""

    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError('이메일은 필수입니다.')
        
        email = self.normalize_email(email)
        user = self.model(email=email, username=email, **extra_fields)
        
        if password:
            user.set_password(password)
        user.save(using=self._db)
        return user
    
    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        
        return self.create_user(email, password, **extra_fields)
    
    def create_user_from_google_oauth(self, google_user_data):
        """Google OAuth 인증 정보로부터 사용자 생성"""
        user = self.create_user(
            email=google_user_data['email'],
            first_name=google_user_data.get('name', ''),
            google_id=google_user_data['google_id'],
            profile_image=google_user_data.get('profile_image', '')
        )

        return user