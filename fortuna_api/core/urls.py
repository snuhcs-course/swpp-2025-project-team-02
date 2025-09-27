"""
URL configuration for Fortuna core API endpoints.
"""

from django.urls import path
from rest_framework.decorators import api_view, permission_classes, parser_classes
from rest_framework.permissions import IsAuthenticated
from django.conf import settings
from user.permissions import DevelopmentOrAuthenticated
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from rest_framework.response import Response
from rest_framework import status
from drf_spectacular.utils import extend_schema, OpenApiParameter
from drf_spectacular.types import OpenApiTypes
from datetime import datetime
from .services.image import ImageService
from .services.fortune import FortuneService

app_name = 'core'

# Initialize services
image_service = ImageService()
fortune_service = FortuneService()


@extend_schema(
    summary="Upload Chakra Image",
    description="Upload a photo with automatic metadata extraction (location, timestamp)",
    request={
        'multipart/form-data': {
            'type': 'object',
            'properties': {
                'image': {
                    'type': 'string',
                    'format': 'binary',
                },
                'chakra_type': {
                    'type': 'string',
                    'description': 'Type of chakra collected'
                }
            }
        }
    },
    responses={
        200: {
            'description': 'Image uploaded successfully',
            'content': {
                'application/json': {
                    'example': {
                        'status': 'success',
                        'data': {
                            'image_id': 'uuid-string',
                            'file_url': '/media/chakras/...',
                            'metadata': {
                                'timestamp': '2024-01-01T12:00:00',
                                'location': {
                                    'latitude': 37.5665,
                                    'longitude': 126.9780
                                }
                            }
                        }
                    }
                }
            }
        }
    }
)
@api_view(['POST'])
@permission_classes([DevelopmentOrAuthenticated])
@parser_classes([MultiPartParser, FormParser])
def upload_chakra_image(request):
    """
    Upload a chakra photo with metadata extraction.

    Extracts EXIF data including:
    - GPS location (if available)
    - Timestamp
    - Device information
    """
    if 'image' not in request.FILES:
        return Response(
            {'status': 'error', 'message': 'No image file provided'},
            status=status.HTTP_400_BAD_REQUEST
        )

    image_file = request.FILES['image']

    # 개발환경에서는 mock user_id 사용
    if getattr(settings, 'DEVELOPMENT_MODE', False) and not request.user.is_authenticated:
        user_id = 1  # 개발용 기본 사용자 ID
    else:
        user_id = request.user.id

    # Get additional data from request
    additional_data = {
        'chakra_type': request.data.get('chakra_type', 'default')
    }

    # Process image upload
    result = image_service.process_image_upload(
        image_file=image_file,
        user_id=user_id,
        additional_data=additional_data
    )

    if result['status'] == 'success':
        return Response(result, status=status.HTTP_201_CREATED)
    else:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)


@extend_schema(
    summary="Generate Tomorrow's Fortune",
    description="Generate personalized Saju fortune for tomorrow based on today's collected chakras",
    parameters=[
        OpenApiParameter(
            name='date',
            type=OpenApiTypes.DATE,
            location=OpenApiParameter.QUERY,
            description='Date for which to generate fortune (YYYY-MM-DD). Defaults to today.',
            required=False
        ),
        OpenApiParameter(
            name='include_photos',
            type=OpenApiTypes.BOOL,
            location=OpenApiParameter.QUERY,
            description='Include photo analysis in fortune generation',
            required=False,
            default=True
        )
    ],
    responses={
        200: {
            'description': 'Fortune generated successfully',
            'content': {
                'application/json': {
                    'example': {
                        'status': 'success',
                        'data': {
                            'user_id': 1,
                            'for_date': '2024-01-02',
                            'tomorrow_gapja': {
                                'code': 1,
                                'name': '갑자',
                                'element': '목'
                            },
                            'fortune': {
                                'overall_fortune': 85,
                                'fortune_summary': '긍정적인 에너지가 가득한 날입니다.',
                                'daily_guidance': {
                                    'best_time': '오전 9-11시',
                                    'lucky_color': '청색',
                                    'key_advice': '새로운 시작에 좋은 날입니다.'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
)
@api_view(['GET', 'POST'])
@permission_classes([DevelopmentOrAuthenticated])
def generate_tomorrow_fortune(request):
    """
    Generate tomorrow's fortune based on Saju and collected chakras.

    Uses:
    - User's birth date and Saju information
    - Tomorrow's Gapja (60-cycle day)
    - Today's collected chakra photos and locations
    - OpenAI API for personalized interpretation
    """
    # 개발환경에서는 mock user_id 사용
    if getattr(settings, 'DEVELOPMENT_MODE', False) and not request.user.is_authenticated:
        user_id = 1  # 개발용 기본 사용자 ID
    else:
        user_id = request.user.id

    # Get date parameter
    date_str = request.GET.get('date') or request.data.get('date')
    if date_str:
        try:
            date = datetime.strptime(date_str, '%Y-%m-%d')
        except ValueError:
            return Response(
                {'status': 'error', 'message': 'Invalid date format. Use YYYY-MM-DD'},
                status=status.HTTP_400_BAD_REQUEST
            )
    else:
        date = datetime.now()

    # Check if photos should be included from GET or POST data
    include_photos_param = request.GET.get('include_photos') or request.data.get('include_photos')
    if include_photos_param is not None:
        include_photos = str(include_photos_param).lower() in ['true', '1', 'yes']
    else:
        include_photos = True  # Default to True

    # Generate fortune
    result = fortune_service.generate_tomorrow_fortune(
        user_id=user_id,
        date=date,
        include_photos=include_photos
    )

    if result['status'] == 'success':
        return Response(result, status=status.HTTP_200_OK)
    else:
        return Response(result, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@extend_schema(
    summary="Get Hourly Fortune",
    description="Get fortune for a specific hour based on traditional Korean time units",
    parameters=[
        OpenApiParameter(
            name='datetime',
            type=OpenApiTypes.DATETIME,
            location=OpenApiParameter.QUERY,
            description='Target datetime (ISO format). Defaults to current time.',
            required=False
        )
    ],
    responses={
        200: {
            'description': 'Hourly fortune retrieved successfully',
            'content': {
                'application/json': {
                    'example': {
                        'status': 'success',
                        'data': {
                            'current_time': '2024-01-01T14:30:00',
                            'time_unit': '미시',
                            'time_element': '토',
                            'compatibility': 'neutral',
                            'advice': '평온한 시간입니다.'
                        }
                    }
                }
            }
        }
    }
)
@api_view(['GET'])
@permission_classes([DevelopmentOrAuthenticated])
def get_hourly_fortune(request):
    """
    Get fortune for specific hour using traditional Korean time units.

    Traditional time units (시진):
    - 자시 (23:00-01:00): Water
    - 축시 (01:00-03:00): Earth
    - 인시 (03:00-05:00): Wood
    - And so on...
    """
    # 개발환경에서는 mock user_id 사용
    if getattr(settings, 'DEVELOPMENT_MODE', False) and not request.user.is_authenticated:
        user_id = 1  # 개발용 기본 사용자 ID
    else:
        user_id = request.user.id

    # Get datetime parameter
    datetime_str = request.GET.get('datetime')
    if datetime_str:
        try:
            target_datetime = datetime.fromisoformat(datetime_str)
        except ValueError:
            return Response(
                {'status': 'error', 'message': 'Invalid datetime format. Use ISO format'},
                status=status.HTTP_400_BAD_REQUEST
            )
    else:
        target_datetime = datetime.now()

    # Get hourly fortune
    result = fortune_service.get_hourly_fortune(
        user_id=user_id,
        target_datetime=target_datetime
    )

    if result['status'] == 'success':
        return Response(result, status=status.HTTP_200_OK)
    else:
        return Response(result, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@extend_schema(
    summary="Get User Images for Date",
    description="Retrieve all images uploaded by the user on a specific date",
    parameters=[
        OpenApiParameter(
            name='date',
            type=OpenApiTypes.DATE,
            location=OpenApiParameter.QUERY,
            description='Date to retrieve images for (YYYY-MM-DD)',
            required=True
        )
    ],
    responses={
        200: {
            'description': 'Images retrieved successfully',
            'content': {
                'application/json': {
                    'example': {
                        'status': 'success',
                        'data': {
                            'date': '2024-01-01',
                            'images': [
                                {
                                    'filename': 'image1.jpg',
                                    'url': '/media/chakras/1/2024-01-01/image1.jpg'
                                }
                            ],
                            'count': 1
                        }
                    }
                }
            }
        }
    }
)
@api_view(['GET'])
@permission_classes([DevelopmentOrAuthenticated])
def get_user_images(request):
    """
    Get all images uploaded by the user on a specific date.
    """
    # 개발환경에서는 mock user_id 사용
    if getattr(settings, 'DEVELOPMENT_MODE', False) and not request.user.is_authenticated:
        user_id = 1  # 개발용 기본 사용자 ID
    else:
        user_id = request.user.id

    date_str = request.GET.get('date')
    if not date_str:
        return Response(
            {'status': 'error', 'message': 'Date parameter is required'},
            status=status.HTTP_400_BAD_REQUEST
        )

    try:
        date = datetime.strptime(date_str, '%Y-%m-%d')
    except ValueError:
        return Response(
            {'status': 'error', 'message': 'Invalid date format. Use YYYY-MM-DD'},
            status=status.HTTP_400_BAD_REQUEST
        )

    images = image_service.get_user_images_for_date(user_id, date)

    return Response({
        'status': 'success',
        'data': {
            'date': date_str,
            'images': images,
            'count': len(images)
        }
    }, status=status.HTTP_200_OK)


# URL patterns
urlpatterns = [
    # Image endpoints
    path('chakra/upload/', upload_chakra_image, name='upload_chakra'),
    path('chakra/images/', get_user_images, name='get_user_images'),

    # Fortune endpoints
    path('fortune/tomorrow/', generate_tomorrow_fortune, name='tomorrow_fortune'),
    path('fortune/hourly/', get_hourly_fortune, name='hourly_fortune'),
]