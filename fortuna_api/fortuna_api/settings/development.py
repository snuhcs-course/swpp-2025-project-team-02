"""
Development settings for fortuna_api project.
"""

from .base import *
from loguru import logger

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

# Allow all hosts in development
ALLOWED_HOSTS = ['*']

# Development database (PostgreSQL recommended, SQLite for quick setup)
if config('USE_SQLITE', default=False, cast=bool):
    DATABASES = {
        'default': {
            'ENGINE': 'django.db.backends.sqlite3',
            'NAME': BASE_DIR / 'db.sqlite3',
        }
    }

# Development-specific apps
INSTALLED_APPS += [
    'debug_toolbar',
]

# Development-specific middleware
MIDDLEWARE = [
    'core.middleware.TestAuthenticationMiddleware',  # X-Test-User-Id 헤더 지원
] + MIDDLEWARE + [
    'debug_toolbar.middleware.DebugToolbarMiddleware',
]

# Django Debug Toolbar configuration
INTERNAL_IPS = [
    '127.0.0.1',
    'localhost',
]

DEBUG_TOOLBAR_CONFIG = {
    'DISABLE_PANELS': [
        'debug_toolbar.panels.redirects.RedirectsPanel',
        'debug_toolbar.panels.profiling.ProfilingPanel',  # 프로파일링 충돌 방지
    ],
    'SHOW_TEMPLATE_CONTEXT': True,
}

# Email backend for development (console backend)
EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'

# CORS settings for development (more permissive)
CORS_ALLOW_ALL_ORIGINS = True

# Disable HTTPS redirects in development
SECURE_SSL_REDIRECT = False

# Less restrictive CORS in development
CORS_ALLOW_HEADERS = [
    'accept',
    'accept-encoding',
    'authorization',
    'content-type',
    'dnt',
    'origin',
    'user-agent',
    'x-csrftoken',
    'x-requested-with',
    'x-forwarded-for',
    'x-forwarded-proto',
    'x-test-user-id',  # 테스트용 인증 우회 헤더
]

# Disable CSRF for development API testing
# Note: Only use this for development!
# SessionAuthentication 제거 → CSRF 체크 비활성화 (curl 테스트용)
REST_FRAMEWORK['DEFAULT_AUTHENTICATION_CLASSES'] = [
    'rest_framework_simplejwt.authentication.JWTAuthentication',
]

# 개발환경에서는 인증 없이 API 사용 가능
DEVELOPMENT_MODE = True
DEFAULT_DEV_PERMISSION_CLASSES = ['rest_framework.permissions.AllowAny']

# Django Extensions settings
SHELL_PLUS_PRINT_SQL = True

# Celery settings for development
CELERY_TASK_ALWAYS_EAGER = config('CELERY_ALWAYS_EAGER', default=False, cast=bool)
CELERY_TASK_EAGER_PROPAGATES = True

# Cache settings for development (dummy cache)
CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}

# JWT Token settings for development (shorter expiration for testing)
SIMPLE_JWT.update({
    'ACCESS_TOKEN_LIFETIME': timedelta(hours=1),
    'REFRESH_TOKEN_LIFETIME': timedelta(days=180),
})

# Console-only logging in development (파일 로깅 제거)
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'simple': {
            'format': '{levelname} {asctime} {module} {message}',
            'style': '{',
        },
    },
    'handlers': {
        'console': {
            'level': 'DEBUG',
            'class': 'logging.StreamHandler',
            'formatter': 'simple',
        },
    },
    # 'root': {
    #     'handlers': ['console'],
    #     'level': 'DEBUG',
    # },
    'loggers': {
        'django': {
            'handlers': ['console'],
            'level': 'INFO',
            'propagate': False,
        },
        'user': {
            'handlers': ['console'],
            'level': 'DEBUG',
            'propagate': False,
        },
    },
}

logger.info(f"using development settings")