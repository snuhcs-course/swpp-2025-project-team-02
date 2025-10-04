"""
Tests for presigned URL functionality.
"""

from django.test import TestCase, override_settings
from unittest.mock import patch, MagicMock
from core.services.image import ImageService
from user.models import User
from core.models import ChakraImage
from datetime import datetime


@override_settings(USE_S3=True)
class TestPresignedURLGeneration(TestCase):
    """Test presigned URL generation for upload and viewing."""

    def setUp(self):
        """Set up test user."""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_upload_presigned_url_success(self, mock_get_client):
        """Test successful generation of upload presigned URL."""
        # Mock S3 client
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/upload-url'
        mock_get_client.return_value = mock_s3_client

        # Generate presigned URL
        result = ImageService.generate_upload_presigned_url(
            user_id=self.user.id,
            chakra_type='test'
        )

        # Assertions
        self.assertEqual(result['status'], 'success')
        self.assertIn('upload_url', result['data'])
        self.assertIn('key', result['data'])
        self.assertIn('file_id', result['data'])
        self.assertEqual(result['data']['upload_url'], 'https://s3.example.com/upload-url')
        mock_s3_client.generate_presigned_url.assert_called_once()

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_upload_presigned_url_no_s3(self, mock_get_client):
        """Test presigned URL generation when S3 is not configured."""
        mock_get_client.return_value = None

        result = ImageService.generate_upload_presigned_url(
            user_id=self.user.id,
            chakra_type='test'
        )

        self.assertEqual(result['status'], 'error')
        self.assertIn('S3 not configured', result['message'])

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_upload_presigned_url_exception(self, mock_get_client):
        """Test presigned URL generation when exception occurs."""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.side_effect = Exception('S3 error')
        mock_get_client.return_value = mock_s3_client

        result = ImageService.generate_upload_presigned_url(
            user_id=self.user.id,
            chakra_type='test'
        )

        self.assertEqual(result['status'], 'error')
        self.assertIn('S3 error', result['message'])

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_view_presigned_url_success(self, mock_get_client):
        """Test successful generation of view presigned URL."""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/view-url'
        mock_get_client.return_value = mock_s3_client

        presigned_url = ImageService.generate_view_presigned_url(
            image_key='chakras/1/2024-01-01/test.jpg',
            expires_in=3600
        )

        self.assertEqual(presigned_url, 'https://s3.example.com/view-url')
        mock_s3_client.generate_presigned_url.assert_called_once()

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_view_presigned_url_no_s3(self, mock_get_client):
        """Test view presigned URL generation when S3 is not configured."""
        mock_get_client.return_value = None

        presigned_url = ImageService.generate_view_presigned_url(
            image_key='chakras/1/2024-01-01/test.jpg'
        )

        self.assertIsNone(presigned_url)

    @patch('core.services.image.ImageService._get_s3_client')
    def test_generate_view_presigned_url_exception(self, mock_get_client):
        """Test view presigned URL generation when exception occurs."""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.side_effect = Exception('S3 error')
        mock_get_client.return_value = mock_s3_client

        presigned_url = ImageService.generate_view_presigned_url(
            image_key='chakras/1/2024-01-01/test.jpg'
        )

        self.assertIsNone(presigned_url)

    @patch('core.services.image.ImageService.generate_view_presigned_url')
    def test_get_user_images_with_presigned_urls(self, mock_generate_view):
        """Test getting user images with presigned URLs."""
        mock_generate_view.return_value = 'https://s3.example.com/presigned-view-url'

        # Create test image
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        chakra_image = ChakraImage.objects.create(
            user_id=self.user.id,
            image='chakras/1/2024-01-01/test.jpg',
            chakra_type='test',
            date=test_date.date(),
            timestamp=test_date
        )

        # Get images with presigned URLs
        images = ImageService.get_user_images_for_date(
            user_id=self.user.id,
            date=test_date,
            generate_presigned=True
        )

        # Assertions
        self.assertEqual(len(images), 1)
        self.assertEqual(images[0]['url'], 'https://s3.example.com/presigned-view-url')
        mock_generate_view.assert_called_once()


@override_settings(USE_S3=False)
class TestWithoutS3(TestCase):
    """Test functionality when S3 is disabled."""

    def setUp(self):
        """Set up test user."""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )

    def test_get_user_images_without_s3(self):
        """Test getting user images without S3 presigned URLs."""
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        chakra_image = ChakraImage.objects.create(
            user_id=self.user.id,
            image='chakras/test.jpg',
            chakra_type='test',
            date=test_date.date(),
            timestamp=test_date
        )

        images = ImageService.get_user_images_for_date(
            user_id=self.user.id,
            date=test_date,
            generate_presigned=False
        )

        self.assertEqual(len(images), 1)
        self.assertIn('test.jpg', images[0]['url'])
