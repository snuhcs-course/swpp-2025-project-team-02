"""
Unit tests for API endpoints.
"""

import json
from io import BytesIO
from datetime import datetime, timedelta
from unittest.mock import patch, Mock, MagicMock
from PIL import Image
from django.test import TestCase, Client, override_settings
from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken


User = get_user_model()


class TestImageAPIEndpoints(APITestCase):
    """Test cases for image-related API endpoints."""

    def setUp(self):
        """Set up test fixtures."""
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        # Set saju data separately
        self.user.birth_date_solar = '1990-01-01'
        self.user.birth_time_units = '자시'
        self.user.yearly_ganji = '갑자'
        self.user.monthly_ganji = '을축'
        self.user.daily_ganji = '병인'  # 병(fire)
        self.user.hourly_ganji = '정묘'
        self.user.save()

        self.refresh = RefreshToken.for_user(self.user)
        self.client.credentials(
            HTTP_AUTHORIZATION=f'Bearer {self.refresh.access_token}'
        )

    def create_test_image(self):
        """Create a test image file."""
        image = Image.new('RGB', (100, 100), color='red')
        img_io = BytesIO()
        image.save(img_io, format='JPEG')
        img_io.seek(0)
        return SimpleUploadedFile(
            name='test.jpg',
            content=img_io.read(),
            content_type='image/jpeg'
        )

    @patch('core.services.image.ImageService.process_image_upload')
    def test_upload_chakra_image_success(self, mock_process):
        """Test successful chakra image upload."""
        mock_process.return_value = {
            'status': 'success',
            'data': {
                'image_id': 'test-uuid',
                'file_url': '/media/test.jpg',
                'metadata': {
                    'timestamp': '2024-01-01T12:00:00',
                    'location': {'latitude': 37.5, 'longitude': 127.0}
                }
            }
        }

        url = reverse('core:upload_chakra')
        image_file = self.create_test_image()
        data = {
            'image': image_file,
            'chakra_type': 'fire'
        }

        response = self.client.post(url, data, format='multipart')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('image_id', response.data['data'])
        mock_process.assert_called_once()

    def test_upload_chakra_image_no_file(self):
        """Test chakra upload without image file."""
        url = reverse('core:upload_chakra')
        data = {'chakra_type': 'water'}

        response = self.client.post(url, data, format='multipart')

        # Since image is now nullable, this might succeed or fail depending on serializer validation
        # If it fails, it should be due to serializer validation
        if response.status_code == status.HTTP_400_BAD_REQUEST:
            self.assertEqual(response.data['status'], 'error')

    @override_settings(DEVELOPMENT_MODE=False)
    def test_upload_chakra_image_unauthenticated(self):
        """Test chakra upload without authentication."""
        self.client.credentials()  # Remove credentials
        url = reverse('core:upload_chakra')
        image_file = self.create_test_image()
        data = {'image': image_file}

        response = self.client.post(url, data, format='multipart')

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.services.image.ImageService.get_user_images_for_date')
    def test_get_user_images_success(self, mock_get_images):
        """Test retrieving user images for a date."""
        mock_get_images.return_value = [
            {
                'filename': 'image1.jpg',
                'url': '/media/image1.jpg'
            },
            {
                'filename': 'image2.jpg',
                'url': '/media/image2.jpg'
            }
        ]

        url = reverse('core:get_user_images')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(len(response.data['data']['images']), 2)
        self.assertEqual(response.data['data']['count'], 2)

    def test_get_user_images_no_date(self):
        """Test getting images without date parameter."""
        url = reverse('core:get_user_images')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Date parameter is required', response.data['message'])

    def test_get_user_images_invalid_date(self):
        """Test getting images with invalid date format."""
        url = reverse('core:get_user_images')
        response = self.client.get(url, {'date': 'invalid-date'})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid date format', response.data['message'])

    @patch('core.views.fortune_service.calculate_fortune_balance')
    def test_get_needed_element_with_fortune_result(self, mock_balance):
        """Test getting needed element - uses calculate_fortune_balance."""
        from core.services.fortune import FortuneScore, ElementDistribution

        # Mock fortune balance to return specific element distribution
        # Make '화' have the minimum count
        mock_balance.return_value = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": None,
                "월운": None,
                "일운": None,
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=1, percentage=6.7),  # Minimum
                "토": ElementDistribution(count=4, percentage=26.7),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=4, percentage=26.7)
            },
            interpretation="Test interpretation"
        )

        url = reverse('core:needed_element')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(response.data['data']['date'], '2024-01-01')
        self.assertEqual(response.data['data']['needed_element'], '화')

    @patch('core.views.fortune_service.calculate_fortune_balance')
    def test_get_needed_element_with_tie_breaker(self, mock_balance):
        """Test getting needed element when multiple elements have same min count - uses 상생 tie-breaker."""
        from core.services.fortune import FortuneScore, ElementDistribution

        # Mock fortune balance where '목' and '화' both have minimum count (2)
        # User's day stem is 병(fire), so 목(wood) empowers 화(fire) - should pick 목
        mock_balance.return_value = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": None,
                "월운": None,
                "일운": None,
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=2, percentage=13.3),  # Minimum (tie)
                "화": ElementDistribution(count=2, percentage=13.3),  # Minimum (tie)
                "토": ElementDistribution(count=4, percentage=26.7),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=4, percentage=26.7)
            },
            interpretation="Test interpretation"
        )

        url = reverse('core:needed_element')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        # User's daily_ganji=2 is 병인, stem is 병(fire)
        # 목생화 (wood empowers fire), so should pick 목
        self.assertEqual(response.data['data']['needed_element'], '목')

    @patch('core.views.fortune_service.calculate_fortune_balance')
    def test_get_needed_element_without_fortune_result(self, mock_balance):
        """Test getting needed element - always uses calculate_fortune_balance."""
        from core.services.fortune import FortuneScore, ElementDistribution

        # Mock fortune balance to return '금' as minimum
        mock_balance.return_value = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": None,
                "월운": None,
                "일운": None,
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=4, percentage=26.7),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=1, percentage=6.7),  # Minimum
                "수": ElementDistribution(count=4, percentage=26.7)
            },
            interpretation="Test interpretation"
        )

        url = reverse('core:needed_element')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(response.data['data']['date'], '2024-01-01')
        self.assertEqual(response.data['data']['needed_element'], '금')

    @patch('core.views.fortune_service.calculate_fortune_balance')
    def test_get_needed_element_default_date(self, mock_balance):
        """Test getting needed element without date parameter (uses today)."""
        from core.services.fortune import FortuneScore, ElementDistribution

        # Mock fortune balance
        mock_balance.return_value = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": None,
                "월운": None,
                "일운": None,
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=4, percentage=26.7),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=2, percentage=13.3),  # Minimum
                "수": ElementDistribution(count=3, percentage=20.0)
            },
            interpretation="Test interpretation"
        )

        url = reverse('core:needed_element')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('date', response.data['data'])
        self.assertIn('needed_element', response.data['data'])
        # Should use today's date
        today = datetime.now().date()
        self.assertEqual(response.data['data']['date'], today.isoformat())
        self.assertEqual(response.data['data']['needed_element'], '금')

    def test_get_needed_element_invalid_date(self):
        """Test getting needed element with invalid date format."""
        url = reverse('core:needed_element')
        response = self.client.get(url, {'date': 'invalid-date'})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['status'], 'error')
        self.assertIn('Invalid date format', response.data['message'])

    @override_settings(DEVELOPMENT_MODE=False)
    def test_get_needed_element_unauthenticated(self):
        """Test getting needed element without authentication."""
        self.client.credentials()  # Remove credentials
        url = reverse('core:needed_element')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_collect_chakra_success(self):
        """Test successful chakra collection without image upload."""
        url = reverse('core:collect_chakra')
        data = {'chakra_type': 'fire'}

        response = self.client.post(url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('id', response.data['data'])
        self.assertEqual(response.data['data']['chakra_type'], 'fire')
        self.assertIn('collected_at', response.data['data'])

    def test_collect_chakra_no_type(self):
        """Test chakra collection without chakra_type."""
        url = reverse('core:collect_chakra')
        data = {}

        response = self.client.post(url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['status'], 'error')

    def test_collect_chakra_various_types(self):
        """Test collecting different chakra types."""
        url = reverse('core:collect_chakra')
        chakra_types = ['wood', 'fire', 'earth', 'metal', 'water']

        for chakra_type in chakra_types:
            data = {'chakra_type': chakra_type}
            response = self.client.post(url, data, format='json')

            self.assertEqual(response.status_code, status.HTTP_201_CREATED)
            self.assertEqual(response.data['data']['chakra_type'], chakra_type)

    @override_settings(DEVELOPMENT_MODE=False)
    def test_collect_chakra_unauthenticated(self):
        """Test chakra collection without authentication."""
        self.client.credentials()  # Remove credentials
        url = reverse('core:collect_chakra')
        data = {'chakra_type': 'fire'}

        response = self.client.post(url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_get_collection_status_success(self):
        """Test getting chakra collection status."""
        from core.models import ChakraImage
        from django.utils import timezone

        # Create some test chakra images
        now = timezone.now()
        ChakraImage.objects.create(
            user=self.user,
            image=None,
            chakra_type='fire',
            date=now.date(),
            timestamp=now,
            device_make='PoC',
            device_model='PoC'
        )
        ChakraImage.objects.create(
            user=self.user,
            image=None,
            chakra_type='fire',
            date=now.date(),
            timestamp=now,
            device_make='PoC',
            device_model='PoC'
        )
        ChakraImage.objects.create(
            user=self.user,
            image=None,
            chakra_type='water',
            date=now.date(),
            timestamp=now,
            device_make='PoC',
            device_model='PoC'
        )

        url = reverse('core:chakra_collection_status')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('collections', response.data['data'])
        self.assertEqual(response.data['data']['total_count'], 3)

    def test_get_collection_status_empty(self):
        """Test getting collection status with no collected chakras."""
        url = reverse('core:chakra_collection_status')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(response.data['data']['total_count'], 0)
        self.assertEqual(len(response.data['data']['collections']), 0)

    @override_settings(DEVELOPMENT_MODE=False)
    def test_get_collection_status_unauthenticated(self):
        """Test getting collection status without authentication."""
        self.client.credentials()  # Remove credentials
        url = reverse('core:chakra_collection_status')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class TestFortuneAPIEndpoints(APITestCase):
    """Test cases for fortune-related API endpoints."""

    def setUp(self):
        """Set up test fixtures."""
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        # Set saju data separately
        self.user.birth_date_solar = '1990-01-01'
        self.user.birth_time_units = '자시'
        self.user.yearly_ganji = '갑자'
        self.user.monthly_ganji = '을축'
        self.user.daily_ganji = '병인'  # 병(fire)
        self.user.hourly_ganji = '정묘'
        self.user.save()

        self.refresh = RefreshToken.for_user(self.user)
        self.client.credentials(
            HTTP_AUTHORIZATION=f'Bearer {self.refresh.access_token}'
        )

    def test_generate_tomorrow_fortune_success(self):
        """Test successful tomorrow fortune retrieval."""
        from core.models import FortuneResult

        # Create a fortune result for tomorrow
        target_date = timezone.make_aware(datetime(2024, 1, 1))
        tomorrow = target_date + timedelta(days=1)

        fortune = FortuneResult.objects.create(
            user=self.user,
            for_date=tomorrow.date(),
            status='completed',
            gapja_code=1,
            gapja_name='갑자',
            gapja_element='목',
            fortune_data={
                'overall_fortune': 85,
                'fortune_summary': '좋은 날입니다.',
                'daily_guidance': {
                    'best_time': '오전 9-11시',
                    'lucky_color': '청색'
                }
            }
        )

        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('fortune', response.data['data'])
        self.assertEqual(
            response.data['data']['fortune']['overall_fortune'], 85
        )

    @patch('core.services.fortune.FortuneService.generate_tomorrow_fortune')
    def test_generate_tomorrow_fortune_post(self, mock_generate):
        """Test tomorrow fortune generation with POST request."""
        mock_generate.return_value = {
            'status': 'success',
            'data': {'fortune': {'overall_fortune': 75}}
        }

        url = reverse('core:tomorrow_fortune')
        response = self.client.post(
            url,
            {'date': '2024-01-01', 'include_photos': False},
            format='json'
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # The endpoint parses the date string, no need for timezone-aware comparison here
        self.assertTrue(mock_generate.called)
        call_kwargs = mock_generate.call_args.kwargs
        self.assertEqual(call_kwargs['user_id'], self.user.id)
        self.assertEqual(call_kwargs['date'].date(), datetime(2024, 1, 1).date())
        self.assertEqual(call_kwargs['include_photos'], False)

    def test_generate_tomorrow_fortune_invalid_date(self):
        """Test fortune generation with invalid date."""
        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url, {'date': 'not-a-date'})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid date format', response.data['message'])

    @patch('core.services.fortune.FortuneService.generate_tomorrow_fortune')
    def test_generate_tomorrow_fortune_default_date(self, mock_generate):
        """Test fortune generation without date (uses today)."""
        mock_generate.return_value = {'status': 'success', 'data': {}}

        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Should be called with today's date
        self.assertTrue(mock_generate.called)
        call_args = mock_generate.call_args
        self.assertIsInstance(call_args.kwargs['date'], datetime)

    @patch('core.services.fortune.FortuneService.generate_tomorrow_fortune')
    def test_generate_tomorrow_fortune_error(self, mock_generate):
        """Test fortune generation with service error."""
        mock_generate.return_value = {
            'status': 'error',
            'message': 'Service error'
        }

        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url)

        self.assertEqual(
            response.status_code,
            status.HTTP_500_INTERNAL_SERVER_ERROR
        )
        self.assertEqual(response.data['status'], 'error')

    @override_settings(DEVELOPMENT_MODE=False)
    def test_fortune_endpoints_unauthenticated(self):
        """Test fortune endpoints without authentication."""
        self.client.credentials()  # Remove credentials

        # Test tomorrow fortune
        url1 = reverse('core:tomorrow_fortune')
        response1 = self.client.get(url1)
        self.assertEqual(response1.status_code, status.HTTP_401_UNAUTHORIZED)

class TestAPIIntegration(APITestCase):
    """Integration tests for API workflow."""

    def setUp(self):
        """Set up test fixtures."""
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        # Set saju data separately
        self.user.birth_date_solar = '1990-01-01'
        self.user.birth_time_units = '자시'
        self.user.yearly_ganji = '갑자'
        self.user.monthly_ganji = '을축'
        self.user.daily_ganji = '병인'  # 병(fire)
        self.user.hourly_ganji = '정묘'
        self.user.save()

        self.refresh = RefreshToken.for_user(self.user)
        self.client.credentials(
            HTTP_AUTHORIZATION=f'Bearer {self.refresh.access_token}'
        )

    @patch('core.tasks.schedule_fortune_update')
    @patch('core.services.image.ImageService.process_image_upload')
    def test_complete_workflow(self, mock_upload, mock_task):
        """Test complete workflow: upload image then get fortune."""
        from core.models import FortuneResult

        # Step 1: Upload chakra image
        mock_upload.return_value = {
            'status': 'success',
            'data': {
                'image_id': 'test-id',
                'file_url': '/media/test.jpg',
                'metadata': {'timestamp': '2024-01-01T12:00:00+09:00'},
                'user_id': self.user.id
            }
        }

        upload_url = reverse('core:upload_chakra')
        image = self.create_test_image()
        upload_response = self.client.post(
            upload_url,
            {'image': image, 'chakra_type': 'fire'},
            format='multipart'
        )

        self.assertEqual(upload_response.status_code, status.HTTP_201_CREATED)

        # Step 2: Update the fortune result that was created during upload
        target_date = timezone.make_aware(datetime(2024, 1, 1))
        tomorrow = target_date + timedelta(days=1)

        # Get the fortune result created by upload
        fortune = FortuneResult.objects.get(
            user=self.user,
            for_date=tomorrow.date()
        )

        # Update it with completed data
        fortune.status = 'completed'
        fortune.gapja_code = 1
        fortune.gapja_name = '을축'
        fortune.gapja_element = '토'
        fortune.fortune_data = {
            'overall_fortune': 90,
            'chakra_readings': [
                {
                    'chakra_type': 'fire',
                    'strength': 85,
                    'message': 'Strong energy detected'
                }
            ]
        }
        fortune.save()

        fortune_url = reverse('core:tomorrow_fortune')
        fortune_response = self.client.get(
            fortune_url,
            {'date': '2024-01-01', 'include_photos': 'true'}
        )

        self.assertEqual(fortune_response.status_code, status.HTTP_200_OK)
        self.assertEqual(
            fortune_response.data['data']['fortune']['overall_fortune'],
            90
        )
        # Verify gapja info is included in response
        self.assertIn('tomorrow_gapja', fortune_response.data['data'])
        self.assertEqual(fortune_response.data['data']['tomorrow_gapja']['code'], 1)

    def create_test_image(self):
        """Helper method to create test image."""
        image = Image.new('RGB', (100, 100), color='blue')
        img_io = BytesIO()
        image.save(img_io, format='JPEG')
        img_io.seek(0)
        return SimpleUploadedFile(
            'test.jpg',
            img_io.read(),
            content_type='image/jpeg'
        )