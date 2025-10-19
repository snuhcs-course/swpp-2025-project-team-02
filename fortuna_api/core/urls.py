"""
URL configuration for Fortuna core API endpoints.
"""

from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import ChakraImageViewSet, FortuneViewSet

app_name = 'core'

# Create router and register viewsets
router = DefaultRouter()
router.register(r'chakras', ChakraImageViewSet, basename='chakra')
router.register(r'fortunes', FortuneViewSet, basename='fortune')

# URL patterns
urlpatterns = [
    # Router URLs
    path('', include(router.urls)),

    # Backward compatibility aliases (will be deprecated)
    path('chakra/upload-url/', ChakraImageViewSet.as_view({'get': 'presigned_url'}), name='get_upload_presigned_url'),
    path('chakra/upload/', ChakraImageViewSet.as_view({'post': 'create'}), name='upload_chakra'),
    path('chakra/images/', ChakraImageViewSet.as_view({'get': 'list'}), name='get_user_images'),
    path('fortune/tomorrow/', FortuneViewSet.as_view({'get': 'tomorrow', 'post': 'tomorrow'}), name='tomorrow_fortune'),
    path('fortune/today/', FortuneViewSet.as_view({'get': 'today'}), name='today_fortune'),
]
