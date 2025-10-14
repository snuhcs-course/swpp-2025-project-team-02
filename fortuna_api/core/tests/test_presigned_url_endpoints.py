"""
Tests for presigned URL API endpoints.
"""

from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APITestCase
from rest_framework import status
from unittest.mock import patch, MagicMock
from datetime import datetime
from user.models import User


@override_settings(USE_S3=True)
class TestPresignedURLEndpoints(APITestCase):
    """Test presigned URL API endpoints."""

    def setUp(self):
        """Set up test user and authentication."""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        self.client.force_authenticate(user=self.user)

    @patch('core.services.image.ImageService._get_s3_client')
    def test_get_upload_presigned_url_success(self, mock_get_client):
        """Test successful retrieval of upload presigned URL."""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/upload-url'
        mock_get_client.return_value = mock_s3_client

        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url, {'chakra_type': 'test'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertIn('upload_url', response.data['data'])
        self.assertIn('key', response.data['data'])
        self.assertIn('file_id', response.data['data'])
        self.assertIn('expires_in', response.data['data'])

    @patch('core.services.image.ImageService._get_s3_client')
    def test_get_upload_presigned_url_default_chakra_type(self, mock_get_client):
        """Test upload presigned URL with default chakra type."""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/upload-url'
        mock_get_client.return_value = mock_s3_client

        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')

    @patch('core.services.image.ImageService._get_s3_client')
    def test_get_upload_presigned_url_unauthenticated(self, mock_get_client):
        """Test that unauthenticated users cannot get upload URL."""
        self.client.force_authenticate(user=None)

        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch('core.services.image.ImageService._get_s3_client')
    def test_get_upload_presigned_url_s3_error(self, mock_get_client):
        """Test upload presigned URL generation with S3 error."""
        mock_get_client.return_value = None

        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertEqual(response.data['status'], 'error')

    @patch('core.services.image.ImageService.generate_view_presigned_url')
    def test_get_user_images_with_presigned_urls(self, mock_generate_view):
        """Test getting user images returns presigned URLs."""
        from core.models import ChakraImage

        mock_generate_view.return_value = 'https://s3.example.com/view-url'

        # Create test image
        test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))
        ChakraImage.objects.create(
            user_id=self.user.id,
            image='chakras/1/2024-01-01/test.jpg',
            chakra_type='test',
            date=test_date.date(),
            timestamp=test_date
        )

        url = reverse('core:get_user_images')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(len(response.data['data']['images']), 1)
        self.assertEqual(
            response.data['data']['images'][0]['url'],
            'https://s3.example.com/view-url'
        )


@override_settings(USE_S3=False)
class TestPresignedURLEndpointsWithoutS3(APITestCase):
    """Test presigned URL endpoints when S3 is disabled."""

    def setUp(self):
        """Set up test user and authentication."""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        self.client.force_authenticate(user=self.user)

    def test_get_upload_presigned_url_s3_disabled(self):
        """Test upload presigned URL endpoint when S3 is disabled."""
        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertEqual(response.data['status'], 'error')
        self.assertIn('S3 not configured', response.data['message'])

    def test_get_user_images_without_presigned(self):
        """Test getting user images without presigned URLs."""
        from core.models import ChakraImage

        test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))
        ChakraImage.objects.create(
            user_id=self.user.id,
            image='chakras/test.jpg',
            chakra_type='test',
            date=test_date.date(),
            timestamp=test_date
        )

        url = reverse('core:get_user_images')
        response = self.client.get(url, {'date': '2024-01-01'})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'success')
        self.assertEqual(len(response.data['data']['images']), 1)
        # Should use regular URL, not presigned
        self.assertIn('chakras/test.jpg', response.data['data']['images'][0]['url'])
