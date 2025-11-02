"""
DRF ViewSets for Fortuna Core API.
"""

from datetime import datetime, timedelta
from django.db.models import Count
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from drf_spectacular.utils import extend_schema, extend_schema_view, OpenApiParameter
from drf_spectacular.types import OpenApiTypes
import logging

from user.permissions import DevelopmentOrAuthenticated
from .models import ChakraImage, FortuneResult
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
)
from .services.image import ImageService
from .services.fortune import FortuneService


# Initialize services
image_service = ImageService()
fortune_service = FortuneService(image_service)

# Initialize logger
logger = logging.getLogger(__name__)


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

        now = datetime.now()

        # Create ChakraImage without image (PoC mode)
        try:
            chakra_image = ChakraImage.objects.create(
                user=user,
                image=None,
                chakra_type=chakra_type,
                date=now.date(),
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
        description="Get total count of collected chakras by type (cumulative)",
        responses={
            200: ChakraCollectionStatusSerializer,
            400: APIResponseSerializer
        }
    )
    @action(detail=False, methods=['get'], url_path='collection-status')
    def collection_status(self, request):
        """
        Get cumulative chakra collection status for the current user.
        Returns count by chakra type across all time.
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

        # Get counts grouped by chakra_type
        collections = ChakraImage.objects.filter(
            user=user
        ).values('chakra_type').annotate(
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
            today_date = datetime.now().date()

        # Calculate fortune balance which now includes needed_element
        fortune_score = fortune_service.calculate_fortune_balance(user, datetime.combine(today_date, datetime.min.time()))

        return Response({
            'status': 'success',
            'data': {
                'date': today_date.isoformat(),
                'needed_element': fortune_score.needed_element
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
        description="Get personalized Saju fortune for today with five elements balance score (DB cached)",
        responses={200: FortuneResponseSerializer}
    )
    @action(detail=False, methods=['get'])
    def today(self, request):
        """Get today's fortune with balance score (DB cached)."""
        user = request.user
        today_date = datetime.now().date()
        tomorrow_date = today_date + timedelta(days=1)

        # Helper functions to convert GanJi/Saju objects (used in both branches)
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

        # Try to get from database first (DB cache)
        try:
            fortune_result = FortuneResult.objects.get(
                user=user,
                for_date=tomorrow_date
            )

            # If fortune exists in DB, return it directly (fast! 두 번째 요청부터 빠름)
            if fortune_result.fortune_data and fortune_result.fortune_score:
                from core.services.daewoon import DaewoonCalculator
                from core.utils.saju_concepts import Saju

                birth_time = user._convert_time_units_to_time(user.birth_time_units)
                saju_date = Saju.from_date(today_date, birth_time)
                saju_user = user.saju()
                daewoon = DaewoonCalculator.calculate_daewoon(user)

                response_data = {
                    'status': 'success',
                    'data': {
                        'date': today_date.isoformat(),
                        'user_id': user.id,
                        'fortune': fortune_result.fortune_data,
                        'fortune_score': fortune_result.fortune_score,
                        'saju_date': saju_to_dict(saju_date),
                        'saju_user': saju_to_dict(saju_user),
                        'daewoon': ganji_to_dict(daewoon)
                    }
                }

                return Response(response_data, status=status.HTTP_200_OK)

        except FortuneResult.DoesNotExist:
            pass  # Generate new fortune below

        # If not in DB, generate new fortune
        result = fortune_service.generate_fortune(
            user=user,
            date=datetime.now()
        )

        # Convert Response[FortuneResponse] to dict
        if result.status == 'success' and result.data:
            response_data = {
                'status': 'success',
                'data': {
                    'date': result.data.date,
                    'user_id': result.data.user_id,
                    'fortune': result.data.fortune.model_dump(),
                    'fortune_score': result.data.fortune_score.model_dump(),
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
