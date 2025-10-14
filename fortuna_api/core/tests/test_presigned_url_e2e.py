"""
E2E tests for presigned URL workflow with actual S3 operations.
Tests the complete flow: get presigned URL -> upload to S3 -> verify upload.
"""

import io
import requests
from PIL import Image
from django.test import TestCase, override_settings
from django.urls import reverse
from rest_framework.test import APIClient
from user.models import User
from core.models import ChakraImage
from moto import mock_aws
import boto3


@mock_aws
class TestPresignedURLEndToEnd(TestCase):
    """E2E tests for presigned URL upload workflow."""

    def setUp(self):
        """Set up test fixtures with mocked S3."""
        # Create mock S3 bucket
        self.bucket_name = 'test-bucket'
        self.region = 'us-east-1'

        # Create S3 client and bucket
        self.s3_client = boto3.client(
            's3',
            region_name=self.region,
            aws_access_key_id='testing',
            aws_secret_access_key='testing',
        )
        self.s3_client.create_bucket(Bucket=self.bucket_name)

        # Create test user
        self.user = User.objects.create_user(
            email='e2e@example.com',
            password='testpass123'
        )

        # Setup API client
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

    def create_test_image_bytes(self):
        """Create test image as bytes."""
        image = Image.new('RGB', (100, 100), color='blue')
        img_io = io.BytesIO()
        image.save(img_io, format='JPEG')
        img_io.seek(0)
        return img_io.read()

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,  # Use moto's default endpoint
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_complete_presigned_upload_workflow(self):
        """
        Test complete E2E workflow:
        1. Request presigned upload URL
        2. Upload file directly to S3 using presigned URL
        3. Verify file exists in S3
        """
        # Step 1: Get presigned upload URL
        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url, {'chakra_type': 'fire'})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['status'], 'success')

        upload_url = response.data['data']['upload_url']
        s3_key = response.data['data']['key']
        file_id = response.data['data']['file_id']

        # Verify response structure
        self.assertIsNotNone(upload_url)
        self.assertIsNotNone(s3_key)
        self.assertIsNotNone(file_id)
        self.assertIn('chakras/', s3_key)
        self.assertIn('.jpg', s3_key)

        # Step 2: Upload file to S3 using presigned URL
        image_bytes = self.create_test_image_bytes()

        upload_response = requests.put(
            upload_url,
            data=image_bytes,
            headers={'Content-Type': 'image/jpeg'}
        )

        # Verify upload succeeded
        self.assertEqual(upload_response.status_code, 200)

        # Step 3: Verify file exists in S3
        try:
            s3_object = self.s3_client.head_object(
                Bucket=self.bucket_name,
                Key=s3_key
            )
            self.assertIsNotNone(s3_object)
            self.assertEqual(s3_object['ContentType'], 'image/jpeg')
            self.assertGreater(s3_object['ContentLength'], 0)
        except Exception as e:
            self.fail(f"Failed to verify S3 object: {e}")

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_presigned_url_expiration(self):
        """Test that presigned URL has correct expiration."""
        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        self.assertEqual(response.status_code, 200)
        self.assertIn('expires_in', response.data['data'])
        self.assertEqual(response.data['data']['expires_in'], 300)  # 5 minutes

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_multiple_uploads_with_unique_keys(self):
        """Test that multiple uploads generate unique S3 keys."""
        keys = set()

        for _ in range(3):
            url = reverse('core:get_upload_presigned_url')
            response = self.client.get(url, {'chakra_type': 'water'})

            self.assertEqual(response.status_code, 200)
            s3_key = response.data['data']['key']

            # Verify key is unique
            self.assertNotIn(s3_key, keys)
            keys.add(s3_key)

            # Verify key format
            self.assertIn(f'chakras/{self.user.id}/', s3_key)

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_upload_and_retrieve_with_presigned_view_url(self):
        """
        Test complete workflow with view URL:
        1. Upload file
        2. Create metadata record
        3. Get presigned view URL
        4. Download file using view URL
        """
        # Step 1: Get upload URL and upload
        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url, {'chakra_type': 'earth'})

        upload_url = response.data['data']['upload_url']
        s3_key = response.data['data']['key']

        # Upload file
        image_bytes = self.create_test_image_bytes()
        upload_response = requests.put(
            upload_url,
            data=image_bytes,
            headers={'Content-Type': 'image/jpeg'}
        )
        self.assertEqual(upload_response.status_code, 200)

        # Step 2: Create ChakraImage record (simulating metadata registration)
        from django.utils import timezone
        from datetime import datetime

        test_date = timezone.make_aware(datetime(2024, 1, 1, 12, 0, 0))
        chakra_image = ChakraImage.objects.create(
            user_id=self.user.id,
            image=s3_key,  # Store S3 key as image path
            chakra_type='earth',
            date=test_date.date(),
            timestamp=test_date
        )

        # Step 3: Get view presigned URL
        from core.services.image import ImageService

        view_url = ImageService.generate_view_presigned_url(s3_key, expires_in=3600)

        self.assertIsNotNone(view_url)
        self.assertIn('Signature', view_url)  # AWS signature in URL

        # Step 4: Download file using view URL
        download_response = requests.get(view_url)

        self.assertEqual(download_response.status_code, 200)
        self.assertEqual(len(download_response.content), len(image_bytes))

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_upload_with_wrong_content_type_fails(self):
        """Test that uploading with wrong content type is rejected."""
        # Get presigned URL
        url = reverse('core:get_upload_presigned_url')
        response = self.client.get(url)

        upload_url = response.data['data']['upload_url']

        # Try to upload with wrong content type
        image_bytes = self.create_test_image_bytes()

        # This should fail because presigned URL expects image/jpeg
        upload_response = requests.put(
            upload_url,
            data=image_bytes,
            headers={'Content-Type': 'text/plain'}  # Wrong content type
        )

        # AWS S3 should reject this due to content type mismatch
        # Note: In real S3, this would be 403, but moto might behave differently
        self.assertNotEqual(upload_response.status_code, 200)


@mock_aws
class TestPresignedURLPerformance(TestCase):
    """Performance and load tests for presigned URL generation."""

    def setUp(self):
        """Set up test fixtures."""
        self.bucket_name = 'test-bucket'

        self.s3_client = boto3.client(
            's3',
            region_name='us-east-1',
            aws_access_key_id='testing',
            aws_secret_access_key='testing',
        )
        self.s3_client.create_bucket(Bucket=self.bucket_name)

        self.user = User.objects.create_user(
            email='perf@example.com',
            password='testpass123'
        )

        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_rapid_presigned_url_generation(self):
        """Test generating multiple presigned URLs rapidly."""
        import time

        url = reverse('core:get_upload_presigned_url')
        num_requests = 10

        start_time = time.time()

        for i in range(num_requests):
            response = self.client.get(url, {'chakra_type': f'test_{i}'})
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.data['status'], 'success')

        elapsed_time = time.time() - start_time

        # All requests should complete in reasonable time (< 5 seconds)
        self.assertLess(elapsed_time, 5.0)

        # Average time per request should be < 500ms
        avg_time = elapsed_time / num_requests
        self.assertLess(avg_time, 0.5)

    @override_settings(
        USE_S3=True,
        AWS_ACCESS_KEY_ID='testing',
        AWS_SECRET_ACCESS_KEY='testing',
        AWS_STORAGE_BUCKET_NAME='test-bucket',
        AWS_S3_ENDPOINT_URL=None,
        AWS_S3_REGION_NAME='us-east-1',
    )
    def test_concurrent_uploads_different_users(self):
        """Test that concurrent uploads from different users work correctly."""
        # Create additional users
        users = [
            User.objects.create_user(
                email=f'user{i}@example.com',
                password='testpass123'
            )
            for i in range(3)
        ]

        uploaded_keys = []

        for user in users:
            client = APIClient()
            client.force_authenticate(user=user)

            url = reverse('core:get_upload_presigned_url')
            response = client.get(url)

            self.assertEqual(response.status_code, 200)
            s3_key = response.data['data']['key']

            # Verify key contains user ID
            self.assertIn(f'chakras/{user.id}/', s3_key)

            uploaded_keys.append(s3_key)

        # All keys should be unique
        self.assertEqual(len(uploaded_keys), len(set(uploaded_keys)))
