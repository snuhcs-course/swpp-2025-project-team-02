"""
DRF ViewSets for Fortuna Core API.
"""

from datetime import datetime, timedelta
from django.conf import settings
from django.db.models import Count
from django.utils import timezone
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from drf_spectacular.utils import extend_schema, extend_schema_view, OpenApiParameter
from drf_spectacular.types import OpenApiTypes
import logging

from user.permissions import DevelopmentOrAuthenticated
from .models import ChakraImage, FortuneResult, element_kr_to_en
from .serializers import (
    ChakraImageSerializer,
    ChakraImageUploadSerializer,
    ChakraCollectSerializer,
    ChakraCollectResponseSerializer,
    ChakraCollectionStatusSerializer,
    PresignedURLRequestSerializer,
    PresignedURLResponseSerializer,
    ImageUploadResponseSerializer,
    ImageListRequestSerializer,
    ImageListResponseSerializer,
    FortuneRequestSerializer,
    FortuneResponseSerializer,
    APIResponseSerializer,
    NeededElementResponseSerializer,
    TodayProgressResponseSerializer,
    MonthlyHistoryResponseSerializer,
    ElementFocusedHistoryResponseSerializer,
)
from .services.image import ImageService
from .services.fortune import FortuneService
from .utils.saju_concepts import TenStems


# Initialize services
image_service = ImageService()
fortune_service = FortuneService(image_service)

# Initialize logger
logger = logging.getLogger(__name__)


def get_absolute_image_url(request, image_field):
    """
    이미지 필드의 절대 URL을 반환합니다.
    S3 사용 시: presigned URL 생성
    로컬 파일 시: build_absolute_uri()로 full URL 생성
    """
    if not image_field:
        return None

    # S3 사용 시 presigned URL 생성
    if getattr(settings, 'USE_S3', False):
        image_key = image_field.name
        presigned_url = image_service.generate_view_presigned_url(image_key)
        if presigned_url:
            return presigned_url
        # Fallback to regular URL if presigned URL generation fails
        return image_field.url

    # 로컬 파일인 경우 절대 URL로 변환
    image_url = image_field.url
    if image_url.startswith('http://') or image_url.startswith('https://'):
        return image_url
    return request.build_absolute_uri(image_url)


def get_user_element_en(user):
    """
    사용자의 일간(日干)에서 오행을 영어로 반환합니다.

    Args:
        user: User 객체

    Returns:
        str: 'wood', 'fire', 'earth', 'metal', 'water' 중 하나
        None: daily_ganji가 없는 경우
    """
    if not user.daily_ganji:
        return None

    try:
        # 일간의 첫 글자 (천간)
        daily_stem_char = user.daily_ganji[0]
        # 천간 찾기
        stem = TenStems.find_by_name(daily_stem_char)
        # 오행 매핑
        element_mapping = {
            '목': 'wood',
            '화': 'fire',
            '토': 'earth',
            '금': 'metal',
            '수': 'water'
        }
        return element_mapping.get(stem.element.chinese)
    except Exception as e:
        logger.error(f"Failed to extract user element: {e}")
        return None


@extend_schema_view(
    list=extend_schema(
        summary="List Chakra Images",
        description="Get all chakra images for a specific date",
        parameters=[
            OpenApiParameter(
                name='date',
                type=OpenApiTypes.DATE,
                location=OpenApiParameter.QUERY,
                description='Date to retrieve images for (YYYY-MM-DD)',
                required=True
            )
        ],
        responses={200: ImageListResponseSerializer}
    ),
    create=extend_schema(
        summary="Upload Chakra Image",
        description="Upload a photo with automatic metadata extraction (location, timestamp)",
        request=ChakraImageUploadSerializer,
        responses={201: ImageUploadResponseSerializer}
    ),
)
class ChakraImageViewSet(viewsets.ModelViewSet):
    """
    ViewSet for managing chakra images.

    Provides endpoints for:
    - Uploading chakra images with metadata extraction
    - Getting presigned URLs for direct S3 uploads
    - Listing images by date
    """

    queryset = ChakraImage.objects.all()
    serializer_class = ChakraImageSerializer
    permission_classes = [DevelopmentOrAuthenticated]
    parser_classes = [MultiPartParser, FormParser, JSONParser]

    def get_queryset(self):
        """Filter chakra images by user."""
        return ChakraImage.objects.filter(user=self.request.user)

    def list(self, request, *args, **kwargs):
        """Get all images for a specific date."""
        # Validate request parameters
        param_serializer = ImageListRequestSerializer(data=request.query_params)

        if not param_serializer.is_valid():
            # Return error in expected format
            error_message = next(iter(param_serializer.errors.values()))[0]
            return Response({
                'status': 'error',
                'message': str(error_message)
            }, status=status.HTTP_400_BAD_REQUEST)

        date_str = param_serializer.validated_data['date']
        date = datetime.combine(date_str, datetime.min.time())

        images = image_service.get_user_images_for_date(request.user.id, date)

        return Response({
            'status': 'success',
            'data': {
                'date': date_str.strftime('%Y-%m-%d'),
                'images': images,
                'count': len(images)
            }
        }, status=status.HTTP_200_OK)

    def create(self, request, *args, **kwargs):
        """Upload a chakra image with metadata extraction."""
        # Validate upload data
        upload_serializer = ChakraImageUploadSerializer(data=request.data)

        if not upload_serializer.is_valid():
            error_message = next(iter(upload_serializer.errors.values()))[0]
            return Response({
                'status': 'error',
                'message': str(error_message)
            }, status=status.HTTP_400_BAD_REQUEST)

        image_file = request.FILES.get('image')
        user_id = request.user.id

        # Get additional data
        additional_data = {
            'chakra_type': upload_serializer.validated_data.get('chakra_type', 'default')
        }

        # Process image upload (image_file can be None)
        result = image_service.process_image_upload(
            image_file=image_file,
            user_id=user_id,
            additional_data=additional_data
        )

        if result['status'] == 'success':
            # Extract image timestamp
            image_timestamp = result['data']['metadata']['timestamp']
            image_date = datetime.fromisoformat(image_timestamp)
            tomorrow = image_date + timedelta(days=1)

            # Get or create fortune result
            fortune, created = FortuneResult.objects.get_or_create(
                user_id=user_id,
                for_date=tomorrow.date(),
                defaults={
                    'status': 'pending',
                    'gapja_code': 0,
                    'gapja_name': '',
                    'gapja_element': '',
                    'fortune_data': {}
                }
            )

            # Trigger async fortune update
            from core.tasks import schedule_fortune_update
            schedule_fortune_update(user_id, image_date.strftime('%Y-%m-%d'))

            # Add fortune info to response
            result['data']['fortune'] = {
                'id': fortune.id,
                'for_date': fortune.for_date.isoformat(),
                'status': fortune.status,
                'created': created
            }

            return Response(result, status=status.HTTP_201_CREATED)
        else:
            return Response(result, status=status.HTTP_400_BAD_REQUEST)

    @extend_schema(
        summary="Get Presigned Upload URL",
        description="Get presigned URL for direct S3 upload",
        parameters=[
            OpenApiParameter(
                name='chakra_type',
                type=OpenApiTypes.STR,
                location=OpenApiParameter.QUERY,
                description='Type of chakra',
                required=False,
                default='default'
            )
        ],
        responses={200: PresignedURLResponseSerializer}
    )
    @action(detail=False, methods=['get'], url_path='presigned-url')
    def presigned_url(self, request):
        """Get presigned URL for direct S3 upload."""
        # Validate request parameters
        param_serializer = PresignedURLRequestSerializer(data=request.query_params)
        param_serializer.is_valid(raise_exception=True)

        user_id = request.user.id
        chakra_type = param_serializer.validated_data.get('chakra_type', 'default')

        result = image_service.generate_upload_presigned_url(
            user_id=user_id,
            chakra_type=chakra_type
        )

        if result['status'] == 'success':
            return Response(result, status=status.HTTP_200_OK)
        else:
            return Response(result, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    @extend_schema(
        summary="Collect Chakra (PoC)",
        description="Collect a chakra element without image upload (for PoC purposes)",
        request=ChakraCollectSerializer,
        responses={
            201: ChakraCollectResponseSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['post'], url_path='collect')
    def collect(self, request):
        """
        PoC endpoint: Collect a chakra without image upload.
        Creates a ChakraImage with a dummy image file.
        """
        # Validate request parameters
        param_serializer = ChakraCollectSerializer(data=request.data)

        if not param_serializer.is_valid():
            error_message = next(iter(param_serializer.errors.values()))[0]
            return Response({
                'status': 'error',
                'message': str(error_message)
            }, status=status.HTTP_400_BAD_REQUEST)

        chakra_type = param_serializer.validated_data['chakra_type']
        # Get date from request or use server's today
        request_date = param_serializer.validated_data.get('date')

        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        now = timezone.now()
        # Use request date if provided, otherwise use server's current date
        collection_date = request_date if request_date else now.date()

        # Create ChakraImage without image (PoC mode)
        try:
            chakra_image = ChakraImage.objects.create(
                user=user,
                image=None,
                chakra_type=chakra_type,
                date=collection_date,
                timestamp=now,
                latitude=None,
                longitude=None,
                device_make='PoC',
                device_model='PoC'
            )

            logger.info(
                f"Chakra collected: user={user.email} (ID: {user.id}), "
                f"type={chakra_type}, chakra_id={chakra_image.id}"
            )

            response_data = {
                'status': 'success',
                'data': {
                    'id': chakra_image.id,
                    'chakra_type': chakra_image.chakra_type,
                    'collected_at': chakra_image.timestamp.isoformat()
                }
            }

            return Response(response_data, status=status.HTTP_201_CREATED)

        except Exception as e:
            logger.error(
                f"Chakra collection failed: user={user.email if user else 'unknown'} (ID: {user.id if user else 'N/A'}), "
                f"type={chakra_type}, error={str(e)}"
            )
            return Response({
                'status': 'error',
                'message': f'Failed to collect chakra: {str(e)}'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    @extend_schema(
        summary="Get Chakra Collection Status",
        description="Get total count of collected chakras by type (cumulative or for specific date)",
        parameters=[
            OpenApiParameter(
                name='date',
                type=OpenApiTypes.DATE,
                location=OpenApiParameter.QUERY,
                description='Date to filter collection (YYYY-MM-DD). If not provided, returns cumulative count.',
                required=False
            )
        ],
        responses={
            200: ChakraCollectionStatusSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='collection-status')
    def collection_status(self, request):
        """
        Get chakra collection status for the current user.
        If date is provided, returns count for that specific date.
        Otherwise, returns cumulative count across all time.
        """
        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Get date parameter (optional)
        date_param = request.query_params.get('date')
        filter_date = None
        if date_param:
            try:
                filter_date = datetime.strptime(date_param, '%Y-%m-%d').date()
            except ValueError:
                return Response({
                    'status': 'error',
                    'message': 'Invalid date format. Use YYYY-MM-DD'
                }, status=status.HTTP_400_BAD_REQUEST)

        # Build query - filter by date if provided
        queryset = ChakraImage.objects.filter(user=user)
        if filter_date:
            queryset = queryset.filter(date=filter_date)

        # Get counts grouped by chakra_type
        collections = queryset.values('chakra_type').annotate(
            count=Count('id')
        ).order_by('chakra_type')

        # Calculate total count
        total_count = sum(item['count'] for item in collections)

        return Response({
            'status': 'success',
            'data': {
                'collections': list(collections),
                'total_count': total_count
            }
        }, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Get Needed Element",
        description="Get the needed element (목/화/토/금/수) for the user based on tomorrow's fortune",
        parameters=[
            OpenApiParameter(
                name='date',
                type=OpenApiTypes.DATE,
                location=OpenApiParameter.QUERY,
                description='Date for which to get needed element (YYYY-MM-DD). Defaults to today.',
                required=False
            )
        ],
        responses={
            200: NeededElementResponseSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='needed-element')
    def needed_element(self, request):
        """
        Get the needed element for harmonizing user's energy with today's energy.
        Returns the element with the smallest count from element_distribution.
        Returns one of: 목, 화, 토, 금, 수
        """
        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Get date parameter (defaults to today)
        date_param = request.query_params.get('date')
        if date_param:
            try:
                today_date = datetime.strptime(date_param, '%Y-%m-%d').date()
            except ValueError:
                return Response({
                    'status': 'error',
                    'message': 'Invalid date format. Use YYYY-MM-DD'
                }, status=status.HTTP_400_BAD_REQUEST)
        else:
            today_date = timezone.now().date()

        # Calculate fortune balance which now includes needed_element
        fortune_score = fortune_service.calculate_fortune_balance(user, datetime.combine(today_date, datetime.min.time()))

        return Response({
            'status': 'success',
            'data': {
                'date': today_date.isoformat(),
                'needed_element': fortune_score.needed_element
            }
        }, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Get Today's Collection Progress",
        description="Get chakra collection progress for a specific date (needed element and count)",
        parameters=[
            OpenApiParameter(
                name='date',
                type=OpenApiTypes.DATE,
                location=OpenApiParameter.QUERY,
                description='Date for progress (YYYY-MM-DD). Defaults to server today if not provided.',
                required=False
            )
        ],
        responses={
            200: TodayProgressResponseSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='today-progress')
    def today_progress(self, request):
        """
        Get chakra collection progress for a specific date.
        Returns the needed element from FortuneResult and current collection count.
        If date is provided, uses that date; otherwise uses server's today.
        """
        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Get date parameter (optional) - defaults to server's today
        date_param = request.query_params.get('date')
        if date_param:
            try:
                target_date = datetime.strptime(date_param, '%Y-%m-%d').date()
            except ValueError:
                return Response({
                    'status': 'error',
                    'message': 'Invalid date format. Use YYYY-MM-DD'
                }, status=status.HTTP_400_BAD_REQUEST)
        else:
            target_date = timezone.now().date()

        # Get FortuneResult for target date
        try:
            fortune_result = FortuneResult.objects.get(
                user=user,
                for_date=target_date
            )
        except FortuneResult.DoesNotExist:
            return Response({
                'status': 'error',
                'message': f'Fortune not generated for {target_date}. Please call /fortune/today first.'
            }, status=status.HTTP_404_NOT_FOUND)

        # Extract needed element from fortune_score
        if not fortune_result.fortune_score or 'needed_element' not in fortune_result.fortune_score:
            return Response({
                'status': 'error',
                'message': 'Fortune score data is incomplete'
            }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        needed_element_kr = fortune_result.fortune_score['needed_element']
        needed_element_en = element_kr_to_en(needed_element_kr)

        # Count collected chakras of target element for target date
        current_count = ChakraImage.objects.filter(
            user=user,
            date=target_date,
            chakra_type=needed_element_en
        ).count()

        # Calculate progress
        target_count = 5  # Default target
        is_completed = current_count >= target_count
        progress_percentage = min(100.0, round((current_count / target_count) * 100, 1))

        return Response({
            'status': 'success',
            'data': {
                'date': target_date.isoformat(),
                'needed_element': needed_element_kr,
                'needed_element_en': needed_element_en,
                'current_count': current_count,
                'target_count': target_count,
                'is_completed': is_completed,
                'progress_percentage': progress_percentage
            }
        }, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Get Monthly Collection History",
        description="Get chakra collection history for a specific month (only days with FortuneResult)",
        parameters=[
            OpenApiParameter(
                name='month',
                type=OpenApiTypes.STR,
                location=OpenApiParameter.QUERY,
                description='Month in YYYY-MM format (e.g., 2025-09)',
                required=True
            )
        ],
        responses={
            200: MonthlyHistoryResponseSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='monthly-history')
    def monthly_history(self, request):
        """
        Get monthly chakra collection history.
        Returns collection progress for each day that has a FortuneResult.
        """
        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Parse month parameter (YYYY-MM format)
        month_param = request.query_params.get('month')
        if not month_param:
            return Response({
                'status': 'error',
                'message': 'Month parameter is required (format: YYYY-MM)'
            }, status=status.HTTP_400_BAD_REQUEST)

        try:
            year, month = month_param.split('-')
            year = int(year)
            month = int(month)
            if not (1 <= month <= 12):
                raise ValueError("Month must be between 1 and 12")
        except (ValueError, AttributeError):
            return Response({
                'status': 'error',
                'message': 'Invalid month format. Use YYYY-MM (e.g., 2025-09)'
            }, status=status.HTTP_400_BAD_REQUEST)

        # Calculate date range for the month
        from calendar import monthrange
        _, last_day = monthrange(year, month)
        start_date = datetime(year, month, 1).date()
        end_date = datetime(year, month, last_day).date()

        # Only include days up to today
        today = timezone.now().date()
        if end_date > today:
            end_date = today

        # Get all FortuneResults for the month
        fortune_results = FortuneResult.objects.filter(
            user=user,
            for_date__gte=start_date,
            for_date__lte=end_date
        ).order_by('for_date')

        # Build day-by-day history
        days_data = []
        total_collected = 0
        completed_days = 0

        for fortune in fortune_results:
            # Extract needed element
            if not fortune.fortune_score or 'needed_element' not in fortune.fortune_score:
                continue  # Skip if incomplete

            needed_element_kr = fortune.fortune_score['needed_element']
            needed_element_en = element_kr_to_en(needed_element_kr)

            # Count collected chakras for this day
            collected_count = ChakraImage.objects.filter(
                user=user,
                date=fortune.for_date,
                chakra_type=needed_element_en
            ).count()

            target_count = 5
            is_completed = collected_count >= target_count
            progress_percentage = min(100.0, round((collected_count / target_count) * 100, 1))

            if is_completed:
                completed_days += 1
            total_collected += collected_count

            days_data.append({
                'date': fortune.for_date.isoformat(),
                'needed_element': needed_element_kr,
                'needed_element_en': needed_element_en,
                'target_count': target_count,
                'collected_count': collected_count,
                'is_completed': is_completed,
                'progress_percentage': progress_percentage
            })

        # Calculate summary
        total_days = len(days_data)
        completion_rate = round((completed_days / total_days * 100), 1) if total_days > 0 else 0.0

        return Response({
            'status': 'success',
            'data': {
                'year': year,
                'month': month,
                'days': days_data,
                'summary': {
                    'total_days': total_days,
                    'completed_days': completed_days,
                    'completion_rate': completion_rate,
                    'total_collected': total_collected
                }
            }
        }, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Get Element-Focused Collection History",
        description="Get collection history for a specific element type, sorted by date descending",
        parameters=[
            OpenApiParameter(
                name='element',
                type=OpenApiTypes.STR,
                location=OpenApiParameter.QUERY,
                description='Element type (wood/fire/earth/metal/water)',
                required=True
            )
        ],
        responses={
            200: ElementFocusedHistoryResponseSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='element-focused-history')
    def element_focused_history(self, request):
        """
        Get collection history focused on a specific element.
        Returns all dates where the element was collected, with counts, sorted by date descending.
        """
        # For development: use test user if not authenticated
        if request.user.is_authenticated:
            user = request.user
        else:
            from django.conf import settings
            from user.models import User
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                user = User.objects.filter(email='test@fortuna.com').first()
                if not user:
                    return Response({
                        'status': 'error',
                        'message': 'Test user not found. Please create test user first.'
                    }, status=status.HTTP_400_BAD_REQUEST)
            else:
                return Response({
                    'status': 'error',
                    'message': 'Authentication required'
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Get and validate element parameter
        element_param = request.query_params.get('element')
        if not element_param:
            return Response({
                'status': 'error',
                'message': 'Element parameter is required (wood/fire/earth/metal/water)'
            }, status=status.HTTP_400_BAD_REQUEST)

        # Validate element
        valid_elements = ['wood', 'fire', 'earth', 'metal', 'water']
        element_en = element_param.lower()
        if element_en not in valid_elements:
            return Response({
                'status': 'error',
                'message': f'Invalid element. Must be one of: {", ".join(valid_elements)}'
            }, status=status.HTTP_400_BAD_REQUEST)

        # Map element to Korean
        element_mapping = {
            'wood': '목',
            'fire': '화',
            'earth': '토',
            'metal': '금',
            'water': '수'
        }
        element_kr = element_mapping[element_en]

        # Get all ChakraImages for this element
        chakra_images = ChakraImage.objects.filter(
            user=user,
            chakra_type=element_en
        ).values('date').annotate(
            collected_count=Count('id')
        ).order_by('-date')  # Sort by date descending (most recent first)

        # Build history list
        history = []
        total_count = 0
        for item in chakra_images:
            history.append({
                'date': item['date'].isoformat(),
                'collected_count': item['collected_count']
            })
            total_count += item['collected_count']

        return Response({
            'status': 'success',
            'data': {
                'element': element_en,
                'element_kr': element_kr,
                'total_count': total_count,
                'history': history
            }
        }, status=status.HTTP_200_OK)


@extend_schema_view(
    list=extend_schema(exclude=True),
    create=extend_schema(exclude=True),
    retrieve=extend_schema(exclude=True),
    update=extend_schema(exclude=True),
    destroy=extend_schema(exclude=True),
)
class FortuneViewSet(viewsets.GenericViewSet):
    """
    ViewSet for fortune telling.

    Provides endpoints for:
    - Getting tomorrow's fortune
    - Getting today's fortune with balance score
    """

    permission_classes = [DevelopmentOrAuthenticated]
    serializer_class = FortuneResponseSerializer

    @extend_schema(
        summary="Get Today's Fortune",
        description="Get personalized Saju fortune for a specific date with five elements balance score (DB cached)",
        parameters=[
            OpenApiParameter(
                name='date',
                type=OpenApiTypes.DATE,
                location=OpenApiParameter.QUERY,
                description='Date for fortune (YYYY-MM-DD). Defaults to server today if not provided.',
                required=False
            )
        ],
        responses={200: FortuneResponseSerializer}
    )
    @action(detail=False, methods=['get'])
    def today(self, request):
        """Get fortune with balance score (DB cached, with race condition protection)."""
        user = request.user
        logger.info(f"Fortune today request - user: {user}, is_authenticated: {user.is_authenticated}, type: {type(user)}")

        # Development mode: try to get user from X-Test-User-Id header
        if not user.is_authenticated:
            if getattr(settings, 'DEVELOPMENT_MODE', False):
                test_user_id = request.META.get('HTTP_X_TEST_USER_ID')
                if test_user_id:
                    try:
                        from django.contrib.auth import get_user_model
                        User = get_user_model()
                        user = User.objects.get(id=int(test_user_id))
                        logger.info(f"Development mode: Using test user {user.id}")
                    except (User.DoesNotExist, ValueError) as e:
                        logger.warning(f"Development mode: Failed to get test user: {e}")
                        return Response({
                            'status': 'error',
                            'error': {
                                'code': 'authentication_required',
                                'message': 'User not authenticated. Please provide valid X-Test-User-Id header in development mode.'
                            }
                        }, status=status.HTTP_401_UNAUTHORIZED)
                else:
                    return Response({
                        'status': 'error',
                        'error': {
                            'code': 'authentication_required',
                            'message': 'User not authenticated. Please provide X-Test-User-Id header in development mode.'
                        }
                    }, status=status.HTTP_401_UNAUTHORIZED)
            else:
                return Response({
                    'status': 'error',
                    'error': {
                        'code': 'authentication_required',
                        'message': 'User not authenticated.'
                    }
                }, status=status.HTTP_401_UNAUTHORIZED)

        # Get date parameter (optional) - defaults to server's today
        date_param = request.query_params.get('date')
        if date_param:
            try:
                today_date = datetime.strptime(date_param, '%Y-%m-%d').date()
            except ValueError:
                return Response({
                    'status': 'error',
                    'error': {
                        'code': 'invalid_date',
                        'message': 'Invalid date format. Use YYYY-MM-DD'
                    }
                }, status=status.HTTP_400_BAD_REQUEST)
        else:
            today_date = timezone.now().date()

        # Helper functions to convert GanJi/Saju objects
        def ganji_to_dict(ganji):
            if ganji is None:
                return None
            return {
                'two_letters': ganji.two_letters,
                'stem': {
                    'korean_name': ganji.stem.korean_name,
                    'element': ganji.stem.element.chinese,
                    'element_color': ganji.stem.element.color,
                    'yin_yang': ganji.stem.yin_yang.value
                },
                'branch': {
                    'korean_name': ganji.branch.korean_name,
                    'element': ganji.branch.element.chinese,
                    'element_color': ganji.branch.element.color,
                    'animal': ganji.branch.animal,
                    'yin_yang': ganji.branch.yin_yang.value
                }
            }

        def saju_to_dict(saju):
            if saju is None:
                return None
            return {
                'yearly': ganji_to_dict(saju.yearly),
                'monthly': ganji_to_dict(saju.monthly),
                'daily': ganji_to_dict(saju.daily),
                'hourly': ganji_to_dict(saju.hourly)
            }

        # Generate fortune (handles DB caching and race conditions internally)
        yesterday = today_date - timedelta(days=1)
        result = fortune_service.generate_fortune(
            user=user,
            date=datetime.combine(yesterday, datetime.min.time())
        )

        # Convert Response[FortuneResponse] to dict
        if result.status == 'success' and result.data:
            # Get the saved fortune result to retrieve image URL
            try:
                fortune_result = FortuneResult.objects.get(
                    user=user,
                    for_date=today_date
                )

                # Check if user collected 5+ needed elements today
                if fortune_result.fortune_score and 'needed_element' in fortune_result.fortune_score:
                    needed_element_kr = fortune_result.fortune_score['needed_element']
                    needed_element_en = element_kr_to_en(needed_element_kr)

                    # Count today's collection
                    collected_count = ChakraImage.objects.filter(
                        user=user,
                        date=today_date,
                        chakra_type=needed_element_en
                    ).count()

                    # If completed (5+), return complete image
                    if collected_count >= 5:
                        user_element_en = get_user_element_en(user)
                        if user_element_en:
                            # Build complete image path
                            complete_image_path = f'complete/result_{user_element_en}_{needed_element_en}.png'
                            complete_image_url = request.build_absolute_uri(
                                settings.STATIC_URL + complete_image_path
                            )
                            fortune_image_url = complete_image_url
                        else:
                            # Fallback to regular image
                            fortune_image_url = get_absolute_image_url(request, fortune_result.fortune_image)
                    else:
                        # Not completed, use regular fortune image
                        fortune_image_url = get_absolute_image_url(request, fortune_result.fortune_image)
                else:
                    # No fortune_score, use regular image
                    fortune_image_url = get_absolute_image_url(request, fortune_result.fortune_image)

            except FortuneResult.DoesNotExist:
                fortune_image_url = None

            response_data = {
                'status': 'success',
                'data': {
                    'date': result.data.date,
                    'user_id': result.data.user_id,
                    'fortune': result.data.fortune.model_dump(),
                    'fortune_score': result.data.fortune_score.model_dump(),
                    'fortune_image_url': fortune_image_url,
                    'saju_date': saju_to_dict(result.data.saju_date),
                    'saju_user': saju_to_dict(result.data.saju_user),
                    'daewoon': ganji_to_dict(result.data.daewoon)
                }
            }

            return Response(response_data, status=status.HTTP_200_OK)
        else:
            # Handle error case
            error_response = {
                'status': 'error',
                'error': result.error.model_dump() if result.error else {'code': 'unknown', 'message': 'Unknown error'}
            }
            return Response(error_response, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
