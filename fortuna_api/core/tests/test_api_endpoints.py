"""
Unit tests for API endpoints.
"""

import json
from io import BytesIO
from datetime import datetime
from unittest.mock import patch, Mock, MagicMock
from PIL import Image
from django.test import TestCase, Client, override_settings
from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
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
        from datetime import timedelta

        # Create a fortune result for tomorrow
        target_date = datetime(2024, 1, 1)
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

    def test_generate_tomorrow_fortune_post(self):
        """Test tomorrow fortune endpoint does not accept POST."""
        url = reverse('core:tomorrow_fortune')
        response = self.client.post(
            url,
            {'date': '2024-01-01'},
            format='json'
        )

        # GET-only endpoint should return 405 Method Not Allowed
        self.assertEqual(response.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)

    def test_generate_tomorrow_fortune_invalid_date(self):
        """Test fortune generation with invalid date."""
        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url, {'date': 'not-a-date'})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid date format', response.data['message'])

    def test_generate_tomorrow_fortune_default_date(self):
        """Test fortune retrieval without date returns 404 if not exists."""
        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url)

        # Should return 404 if no fortune exists for tomorrow
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data['status'], 'not_found')

    def test_generate_tomorrow_fortune_not_found(self):
        """Test fortune retrieval when fortune doesn't exist."""
        url = reverse('core:tomorrow_fortune')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data['status'], 'not_found')
        self.assertIn('not yet generated', response.data['message'])

    @patch('core.services.fortune.FortuneService.get_hourly_fortune')
    def test_get_hourly_fortune_success(self, mock_hourly):
        """Test successful hourly fortune retrieval."""
        mock_hourly.return_value = {
            'status': 'success',
            'data': {
                'current_time': '2024-01-01T14:30:00',
                'time_unit': '미시',
                'time_element': '토',
                'compatibility': 'neutral',
                'advice': '평온한 시간입니다.'
            }
        }

        url = reverse('core:hourly_fortune')
        response = self.client.get(
            url,
            {'datetime': '2024-01-01T14:30:00'}
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['data']['time_unit'], '미시')
        self.assertEqual(response.data['data']['time_element'], '토')

    def test_get_hourly_fortune_invalid_datetime(self):
        """Test hourly fortune with invalid datetime."""
        url = reverse('core:hourly_fortune')
        response = self.client.get(url, {'datetime': 'invalid'})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid datetime format', response.data['message'])

    @patch('core.services.fortune.FortuneService.get_hourly_fortune')
    def test_get_hourly_fortune_default_time(self, mock_hourly):
        """Test hourly fortune without datetime (uses current time)."""
        mock_hourly.return_value = {
            'status': 'success',
            'data': {'time_unit': '오시'}
        }

        url = reverse('core:hourly_fortune')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(mock_hourly.called)

    @override_settings(DEVELOPMENT_MODE=False)
    def test_fortune_endpoints_unauthenticated(self):
        """Test fortune endpoints without authentication."""
        self.client.credentials()  # Remove credentials

        # Test tomorrow fortune
        url1 = reverse('core:tomorrow_fortune')
        response1 = self.client.get(url1)
        self.assertEqual(response1.status_code, status.HTTP_401_UNAUTHORIZED)

        # Test hourly fortune
        url2 = reverse('core:hourly_fortune')
        response2 = self.client.get(url2)
        self.assertEqual(response2.status_code, status.HTTP_401_UNAUTHORIZED)


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

    @patch('core.tasks.update_fortune_async')
    @patch('core.services.image.ImageService.process_image_upload')
    def test_complete_workflow(self, mock_upload, mock_task):
        """Test complete workflow: upload image then get fortune."""
        from core.models import FortuneResult
        from datetime import timedelta

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
        target_date = datetime(2024, 1, 1)
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
            {'date': '2024-01-01'}
        )

        self.assertEqual(fortune_response.status_code, status.HTTP_200_OK)
        self.assertEqual(
            fortune_response.data['data']['fortune']['overall_fortune'],
            90
        )

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