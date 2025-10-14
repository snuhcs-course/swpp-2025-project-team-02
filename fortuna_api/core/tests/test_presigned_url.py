"""
간단한 S3 presigned URL 테스트 - hang 문제 방지
"""

from django.test import TestCase, override_settings
from django.utils import timezone
from unittest.mock import patch, MagicMock
from core.services.image import ImageService
from user.models import User
from core.models import ChakraImage
from datetime import datetime, timedelta


class TestPresignedURLSimple(TestCase):
    """간단한 presigned URL 테스트 - hang 방지"""

    def setUp(self):
        """테스트 사용자 생성"""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='test-key',
        AWS_SECRET_ACCESS_KEY='test-secret',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL='http://localhost:9000',
        AWS_S3_REGION_NAME='us-east-1',
    )
    @patch('core.services.image.ImageService._get_s3_client')
    def test_upload_presigned_url_success(self, mock_get_client):
        """업로드 presigned URL 생성 성공 테스트"""
        # Mock S3 클라이언트 설정
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/upload-url'
        mock_get_client.return_value = mock_s3_client

        # Presigned URL 생성
        result = ImageService.generate_upload_presigned_url(
            user_id=self.user.id,
            chakra_type='test'
        )

        # 결과 검증
        self.assertEqual(result['status'], 'success')
        self.assertIn('upload_url', result['data'])
        self.assertIn('key', result['data'])
        self.assertIn('file_id', result['data'])
        self.assertEqual(result['data']['upload_url'], 'https://s3.example.com/upload-url')
        
        # Mock 호출 확인
        mock_s3_client.generate_presigned_url.assert_called_once()

    @override_settings(USE_S3=False)
    def test_upload_presigned_url_no_s3(self):
        """S3 비활성화 시 테스트"""
        result = ImageService.generate_upload_presigned_url(
            user_id=self.user.id,
            chakra_type='test'
        )

        self.assertEqual(result['status'], 'error')
        self.assertIn('S3 not configured', result['message'])

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='test-key',
        AWS_SECRET_ACCESS_KEY='test-secret',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL='http://localhost:9000',
        AWS_S3_REGION_NAME='us-east-1',
    )
    @patch('core.services.image.ImageService._get_s3_client')
    def test_view_presigned_url_success(self, mock_get_client):
        """뷰 presigned URL 생성 성공 테스트"""
        mock_s3_client = MagicMock()
        mock_s3_client.generate_presigned_url.return_value = 'https://s3.example.com/view-url'
        mock_get_client.return_value = mock_s3_client

        presigned_url = ImageService.generate_view_presigned_url(
            image_key='chakras/1/2024-01-01/test.jpg',
            expires_in=3600
        )

        self.assertEqual(presigned_url, 'https://s3.example.com/view-url')
        mock_s3_client.generate_presigned_url.assert_called_once()

    @override_settings(USE_S3=False)
    def test_view_presigned_url_no_s3(self):
        """S3 비활성화 시 뷰 URL 테스트"""
        presigned_url = ImageService.generate_view_presigned_url(
            image_key='chakras/1/2024-01-01/test.jpg'
        )

        self.assertIsNone(presigned_url)

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='test-key',
        AWS_SECRET_ACCESS_KEY='test-secret',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL='http://localhost:9000',
        AWS_S3_REGION_NAME='us-east-1',
    )
    @patch('core.services.image.ImageService.generate_view_presigned_url')
    def test_user_images_with_presigned_urls(self, mock_generate_view):
        """사용자 이미지 목록 presigned URL 테스트"""
        mock_generate_view.return_value = 'https://s3.example.com/presigned-view-url'

        # 테스트 이미지 생성
        test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))
        chakra_image = ChakraImage.objects.create(
            user_id=self.user.id,
            image='chakras/1/2024-01-01/test.jpg',
            chakra_type='test',
            date=test_date.date(),
            timestamp=test_date
        )

        # presigned URL과 함께 이미지 목록 조회
        images = ImageService.get_user_images_for_date(
            user_id=self.user.id,
            date=test_date,
            generate_presigned=True
        )

        # 검증
        self.assertEqual(len(images), 1)
        self.assertEqual(images[0]['url'], 'https://s3.example.com/presigned-view-url')
        mock_generate_view.assert_called_once()

    @override_settings(USE_S3=False)
    def test_user_images_without_s3(self):
        """S3 없이 사용자 이미지 목록 테스트"""
        test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))
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
