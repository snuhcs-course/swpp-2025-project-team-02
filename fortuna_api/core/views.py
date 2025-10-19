"""
DRF ViewSets for Fortuna Core API.
"""

from datetime import datetime, timedelta
from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from drf_spectacular.utils import extend_schema, extend_schema_view, OpenApiParameter
from drf_spectacular.types import OpenApiTypes

from user.permissions import DevelopmentOrAuthenticated
from .models import ChakraImage, FortuneResult
from .serializers import (
    ChakraImageSerializer,
    ChakraImageUploadSerializer,
    PresignedURLRequestSerializer,
    PresignedURLResponseSerializer,
    ImageUploadResponseSerializer,
    ImageListRequestSerializer,
    ImageListResponseSerializer,
    FortuneRequestSerializer,
    FortuneResponseSerializer,
    APIResponseSerializer,
)
from .services.image import ImageService
from .services.fortune import FortuneService


# Initialize services
image_service = ImageService()
fortune_service = FortuneService(image_service)


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
        # Check for image file first
        if 'image' not in request.FILES:
            return Response(
                {'status': 'error', 'message': 'No image file provided'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Validate upload data
        upload_serializer = ChakraImageUploadSerializer(data=request.data)

        if not upload_serializer.is_valid():
            error_message = next(iter(upload_serializer.errors.values()))[0]
            return Response({
                'status': 'error',
                'message': str(error_message)
            }, status=status.HTTP_400_BAD_REQUEST)

        image_file = request.FILES['image']
        user_id = request.user.id

        # Get additional data
        additional_data = {
            'chakra_type': upload_serializer.validated_data.get('chakra_type', 'default')
        }

        # Process image upload
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
        summary="Get Tomorrow's Fortune",
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
        request=FortuneRequestSerializer,
        responses={200: FortuneResponseSerializer}
    )
    @action(detail=False, methods=['get', 'post'])
    def tomorrow(self, request):
        """Get tomorrow's fortune (will be deprecated)."""
        # Support both GET and POST for backward compatibility
        if request.method == 'POST':
            request_data = request.data
        else:
            request_data = request.query_params

        # Validate request parameters
        param_serializer = FortuneRequestSerializer(data=request_data)

        if not param_serializer.is_valid():
            error_message = next(iter(param_serializer.errors.values()))[0]
            return Response({
                'status': 'error',
                'message': str(error_message)
            }, status=status.HTTP_400_BAD_REQUEST)

        user_id = request.user.id
        date = param_serializer.validated_data.get('date')

        if not date:
            date = datetime.now()
        else:
            date = datetime.combine(date, datetime.min.time())

        include_photos = param_serializer.validated_data.get('include_photos', True)
        tomorrow = date + timedelta(days=1)

        # Try to get existing fortune
        try:
            fortune_result = FortuneResult.objects.get(
                user_id=user_id,
                for_date=tomorrow.date()
            )

            # Prepare response
            response_data = {
                'status': 'success',
                'data': {
                    'user_id': user_id,
                    'for_date': fortune_result.for_date.isoformat(),
                    'tomorrow_gapja': {
                        'code': fortune_result.gapja_code,
                        'name': fortune_result.gapja_name,
                        'element': fortune_result.gapja_element
                    },
                    'fortune': fortune_result.fortune_data
                }
            }

            return Response(response_data, status=status.HTTP_200_OK)

        except FortuneResult.DoesNotExist:
            # Generate fortune on-the-fly if not exists
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
        summary="Get Today's Fortune",
        description="Get personalized Saju fortune for today with five elements balance score",
        responses={200: FortuneResponseSerializer}
    )
    @action(detail=False, methods=['get'])
    def today(self, request):
        """Get today's fortune with balance score."""
        user = request.user

        result = fortune_service.generate_fortune(
            user=user,
            date=datetime.now()
        )

        if result['status'] == 'success':
            return Response(result, status=status.HTTP_200_OK)
        else:
            return Response(result, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
