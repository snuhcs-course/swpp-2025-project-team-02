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

    # chakra collection endpoints
    path('chakra/collect/', ChakraImageViewSet.as_view({'post': 'collect'}), name='collect_chakra'),
    path('chakra/collection-status/', ChakraImageViewSet.as_view({'get': 'collection_status'}), name='chakra_collection_status'),
    path('chakra/needed-element/', ChakraImageViewSet.as_view({'get': 'needed_element'}), name='needed_element'),
    path('chakra/today-progress/', ChakraImageViewSet.as_view({'get': 'today_progress'}), name='chakra_today_progress'),
    path('chakra/monthly-history/', ChakraImageViewSet.as_view({'get': 'monthly_history'}), name='chakra_monthly_history'),
    path('chakra/element-focused-history/', ChakraImageViewSet.as_view({'get': 'element_focused_history'}), name='chakra_element_history'),

    path('fortune/today/', FortuneViewSet.as_view({'get': 'today'}), name='today_fortune'),
]
