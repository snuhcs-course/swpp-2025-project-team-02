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

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['status'], 'error')
        self.assertIn('No image file', response.data['message'])

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


class TestFortuneAPIEndpoints(APITestCase):
    """Test cases for fortune-related API endpoints."""

    def setUp(self):
        """Set up test fixtures."""
        self.client = APIClient()
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
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