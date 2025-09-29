from django.urls import path

from . import views

app_name = 'user'

urlpatterns = [
    # 인증 관련
    path('auth/google/', views.GoogleAuthView.as_view(), name='google_auth'),
    path('auth/refresh/', views.CustomTokenRefreshView.as_view(), name='token_refresh'),
    path('auth/verify/', views.CustomTokenVerifyView.as_view(), name='token_verify'),
    path('auth/logout/', views.LogoutView.as_view(), name='logout'),

    # 사용자 프로필
    path('profile/', views.UserProfileView.as_view(), name='user_profile'),
]
