"""
Unit tests for ImageService.
"""

import os
import tempfile
from io import BytesIO
from datetime import datetime
from unittest.mock import Mock, patch, MagicMock
from PIL import Image
import pytest
from django.test import TestCase, override_settings
from django.core.files.uploadedfile import SimpleUploadedFile, InMemoryUploadedFile
from django.conf import settings
from django.contrib.auth import get_user_model
from django.utils import timezone
from ..services.image import ImageService
from ..models import ChakraImage

User = get_user_model()


class TestImageService(TestCase):
    """Test cases for ImageService."""

    def setUp(self):
        """Set up test fixtures."""
        self.service = ImageService()
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )
        self.user_id = self.user.id
        self.test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))

    def create_test_image(self, with_exif=False):
        """Create a test image for testing."""
        # Create a simple image
        image = Image.new('RGB', (100, 100), color='red')

        # Add EXIF data if requested
        if with_exif:
            from PIL.ExifTags import TAGS, GPSTAGS
            exif_data = image.getexif()

            # Add basic EXIF tags
            exif_data[0x0132] = "2024:01:01 12:00:00"  # DateTime
            exif_data[0x010F] = "TestMake"  # Make
            exif_data[0x0110] = "TestModel"  # Model

            # Add GPS data (simplified)
            # GPS tags are more complex and would require IFD structure
            # For testing, we'll mock this in the test

        # Save to BytesIO
        img_io = BytesIO()
        image.save(img_io, format='JPEG')
        img_io.seek(0)

        return SimpleUploadedFile(
            name='test_image.jpg',
            content=img_io.read(),
            content_type='image/jpeg'
        )

    def test_extract_exif_data_without_exif(self):
        """Test extracting EXIF data from image without EXIF."""
        image_file = self.create_test_image(with_exif=False)

        metadata = self.service.extract_exif_data(image_file)

        self.assertIsNotNone(metadata['timestamp'])
        self.assertIsNone(metadata['location'])
        self.assertIsNone(metadata['device_info'])
        self.assertEqual(metadata['image_info'], {})

    @patch('PIL.Image.open')
    def test_extract_exif_data_with_exif(self, mock_open):
        """Test extracting EXIF data from image with EXIF."""
        # Create mock image with EXIF data
        mock_image = Mock()
        mock_exif = {
            0x0132: "2024:01:01 12:00:00",  # DateTime
            0x010F: "Samsung",  # Make
            0x0110: "Galaxy S22",  # Model
            0x0100: 1920,  # ImageWidth
            0x0101: 1080,  # ImageLength
            34853: {  # GPSInfo
                1: 'N',  # GPSLatitudeRef
                2: ((37, 1), (33, 1), (59, 1)),  # GPSLatitude
                3: 'E',  # GPSLongitudeRef
                4: ((126, 1), (58, 1), (41, 1)),  # GPSLongitude
            }
        }
        mock_image.getexif.return_value = mock_exif
        mock_open.return_value = mock_image

        image_file = self.create_test_image(with_exif=False)
        metadata = self.service.extract_exif_data(image_file)

        self.assertTrue(metadata['timestamp'].startswith("2024-01-01T12:00:00"))
        self.assertEqual(metadata['device_info']['make'], "Samsung")
        self.assertEqual(metadata['device_info']['model'], "Galaxy S22")

    def test_convert_gps_to_decimal(self):
        """Test GPS coordinate conversion."""
        gps_data = {
            'GPSLatitude': ((37, 1), (33, 1), (59, 1)),
            'GPSLatitudeRef': 'N',
            'GPSLongitude': ((126, 1), (58, 1), (41, 1)),
            'GPSLongitudeRef': 'E'
        }

        location = self.service.convert_gps_to_decimal(gps_data)

        self.assertIsNotNone(location)
        self.assertAlmostEqual(location['latitude'], 37.5664, places=4)
        self.assertAlmostEqual(location['longitude'], 126.9781, places=4)

    def test_convert_gps_to_decimal_south_west(self):
        """Test GPS conversion for Southern and Western hemispheres."""
        gps_data = {
            'GPSLatitude': ((33, 1), (51, 1), (0, 1)),
            'GPSLatitudeRef': 'S',
            'GPSLongitude': ((151, 1), (12, 1), (0, 1)),
            'GPSLongitudeRef': 'W'
        }

        location = self.service.convert_gps_to_decimal(gps_data)

        self.assertIsNotNone(location)
        self.assertLess(location['latitude'], 0)  # Southern hemisphere
        self.assertLess(location['longitude'], 0)  # Western hemisphere

    @override_settings(USE_S3=False)
    def test_save_image(self):
        """Test saving image to storage and database."""
        image_file = self.create_test_image()
        metadata = {
            'timestamp': self.test_date.isoformat(),
            'location': {'latitude': 37.5665, 'longitude': 126.9780},
            'device_info': {'make': 'TestMake', 'model': 'TestModel'}
        }

        chakra_image = self.service.save_image(
            image_file, self.user_id, metadata, 'test'
        )

        self.assertIsInstance(chakra_image, ChakraImage)
        self.assertEqual(chakra_image.user_id, self.user_id)
        self.assertEqual(chakra_image.chakra_type, 'test')
        self.assertEqual(chakra_image.date, self.test_date.date())
        self.assertEqual(chakra_image.latitude, 37.5665)
        self.assertEqual(chakra_image.longitude, 126.9780)

    @patch.object(ImageService, 'save_image')
    @patch.object(ImageService, 'extract_exif_data')
    def test_process_image_upload_success(self, mock_extract, mock_save):
        """Test successful image upload processing."""
        mock_extract.return_value = {
            'timestamp': self.test_date.isoformat(),
            'location': {'latitude': 37.5665, 'longitude': 126.9780},
            'device_info': None,
            'image_info': {}
        }

        mock_chakra = Mock(spec=ChakraImage)
        mock_chakra.id = 1
        mock_chakra.image.url = '/media/chakras/1/2024-01-01/test.jpg'
        mock_chakra.created_at = self.test_date
        mock_save.return_value = mock_chakra

        image_file = self.create_test_image()
        additional_data = {'chakra_type': 'fire'}

        result = self.service.process_image_upload(
            image_file, self.user_id, additional_data
        )

        self.assertEqual(result['status'], 'success')
        self.assertIn('image_id', result['data'])
        self.assertEqual(result['data']['user_id'], self.user_id)

    @patch.object(ImageService, 'extract_exif_data')
    def test_process_image_upload_failure(self, mock_extract):
        """Test image upload processing with error."""
        mock_extract.side_effect = Exception("Test error")

        image_file = self.create_test_image()
        result = self.service.process_image_upload(image_file, self.user_id)

        self.assertEqual(result['status'], 'error')
        self.assertEqual(result['message'], 'Test error')

    @override_settings(USE_S3=False)
    def test_get_user_images_for_date(self):
        """Test retrieving user images for a specific date."""
        # Create test images
        img1 = ChakraImage.objects.create(
            user_id=self.user_id,
            image='test1.jpg',
            chakra_type='fire',
            date=self.test_date.date(),
            timestamp=self.test_date,
            latitude=37.5,
            longitude=126.9
        )
        img2 = ChakraImage.objects.create(
            user_id=self.user_id,
            image='test2.jpg',
            chakra_type='water',
            date=self.test_date.date(),
            timestamp=self.test_date
        )

        images = self.service.get_user_images_for_date(
            self.user_id, self.test_date
        )

        self.assertEqual(len(images), 2)
        self.assertEqual(images[0]['id'], img2.id)  # Ordered by -timestamp
        self.assertEqual(images[1]['id'], img1.id)
        self.assertIsNotNone(images[1]['location'])
        self.assertIsNone(images[0]['location'])

    def test_get_user_images_for_date_empty(self):
        """Test retrieving images when no images exist."""
        images = self.service.get_user_images_for_date(
            self.user_id, self.test_date
        )

        self.assertEqual(images, [])


class TestImageServiceIntegration(TestCase):
    """Integration tests for ImageService."""

    def setUp(self):
        """Set up test fixtures."""
        self.service = ImageService()
        self.user = User.objects.create_user(
            email='integration@example.com',
            password='testpass123'
        )
        self.user_id = self.user.id

    @override_settings(USE_S3=False)
    def test_full_image_processing_workflow(self):
        """Test complete image processing workflow."""
        # Create test image
        image = Image.new('RGB', (200, 200), color='blue')
        img_io = BytesIO()
        image.save(img_io, format='JPEG')

        # Get size before seeking
        size = img_io.tell()
        img_io.seek(0)

        image_file = InMemoryUploadedFile(
            img_io,
            None,
            'test.jpg',
            'image/jpeg',
            size,
            None
        )

        # Process image
        result = self.service.process_image_upload(
            image_file,
            self.user_id,
            {'chakra_type': 'water'}
        )

        # Verify result
        self.assertEqual(result['status'], 'success')
        self.assertIsNotNone(result['data']['image_id'])
        self.assertIsNotNone(result['data']['metadata']['timestamp'])

        # Verify database record
        chakra_image = ChakraImage.objects.get(id=result['data']['image_id'])
        self.assertEqual(chakra_image.user_id, self.user_id)
        self.assertEqual(chakra_image.chakra_type, 'water')