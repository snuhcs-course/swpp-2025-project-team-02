"""
Image processing service for Fortuna API.
Handles photo uploads, metadata extraction, and storage.
"""

import os
import uuid
import boto3
from datetime import datetime, timedelta
from typing import Dict, Any, Optional
from PIL import Image
from PIL.ExifTags import TAGS, GPSTAGS
from django.core.files.uploadedfile import InMemoryUploadedFile
from django.conf import settings
from django.core.files.storage import default_storage
from django.utils import timezone
from core.models import ChakraImage
import logging

logger = logging.getLogger(__name__)


class ImageService:
    """Service for handling image uploads and metadata extraction."""

    @staticmethod
    def _get_s3_client():
        """Get configured S3 client for presigned URL generation."""
        if not getattr(settings, 'USE_S3', False):
            return None

        from botocore.config import Config

        # Configure boto3 with timeouts to prevent hanging
        config = Config(
            connect_timeout=5,
            read_timeout=5,
            retries={'max_attempts': 1}
        )

        return boto3.client(
            's3',
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            endpoint_url=getattr(settings, 'AWS_S3_ENDPOINT_URL', None),
            region_name=settings.AWS_S3_REGION_NAME,
            config=config
        )

    @staticmethod
    def generate_upload_presigned_url(user_id: int, chakra_type: str = 'default') -> Dict[str, Any]:
        """
        Generate presigned URL for direct S3 upload.

        Args:
            user_id: ID of the user uploading
            chakra_type: Type of chakra

        Returns:
            Dictionary with presigned URL and metadata
        """
        s3_client = ImageService._get_s3_client()
        if not s3_client:
            return {
                "status": "error",
                "message": "S3 not configured"
            }

        # Generate unique filename
        file_id = str(uuid.uuid4())
        date_path = datetime.now().strftime('%Y-%m-%d')
        key = f'chakras/{user_id}/{date_path}/{file_id}.jpg'

        try:
            presigned_url = s3_client.generate_presigned_url(
                'put_object',
                Params={
                    'Bucket': settings.AWS_STORAGE_BUCKET_NAME,
                    'Key': key,
                    'ContentType': 'image/jpeg'
                },
                ExpiresIn=300  # 5 minutes
            )

            return {
                "status": "success",
                "data": {
                    "upload_url": presigned_url,
                    "key": key,
                    "file_id": file_id,
                    "expires_in": 300  # 5 minutes
                }
            }
        except Exception as e:
            logger.error(f"Failed to generate presigned URL: {e}")
            return {
                "status": "error",
                "message": str(e)
            }

    @staticmethod
    def generate_view_presigned_url(image_key: str, expires_in: int = 3600) -> Optional[str]:
        """
        Generate presigned URL for viewing an image.

        Args:
            image_key: S3 object key
            expires_in: URL expiration time in seconds (default: 1 hour)

        Returns:
            Presigned URL string or None
        """
        s3_client = ImageService._get_s3_client()
        if not s3_client:
            return None

        try:
            presigned_url = s3_client.generate_presigned_url(
                'get_object',
                Params={
                    'Bucket': settings.AWS_STORAGE_BUCKET_NAME,
                    'Key': image_key
                },
                ExpiresIn=expires_in
            )
            return presigned_url
        except Exception as e:
            logger.error(f"Failed to generate view presigned URL: {e}")
            return None

    @staticmethod
    def extract_exif_data(image_file: InMemoryUploadedFile) -> Dict[str, Any]:
        """
        Extract EXIF metadata from uploaded image.

        Args:
            image_file: The uploaded image file

        Returns:
            Dictionary containing extracted metadata
        """
        metadata = {
            "timestamp": None,
            "location": None,
            "device_info": None,
            "image_info": {}
        }

        try:
            image = Image.open(image_file)
            exifdata = image.getexif()

            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = TAGS.get(tag_id, tag_id)

                    # Extract timestamp
                    if tag == "DateTime" or tag == "DateTimeOriginal":
                        try:
                            naive_dt = datetime.strptime(value, "%Y:%m:%d %H:%M:%S")
                            aware_dt = timezone.make_aware(naive_dt, timezone.get_current_timezone())
                            metadata["timestamp"] = aware_dt.isoformat()
                        except Exception as e:
                            logger.warning(f"Failed to parse datetime: {e}")

                    # Extract device info
                    elif tag == "Make":
                        metadata["device_info"] = metadata["device_info"] or {}
                        metadata["device_info"]["make"] = value
                    elif tag == "Model":
                        metadata["device_info"] = metadata["device_info"] or {}
                        metadata["device_info"]["model"] = value

                    # Store general image info
                    elif tag in ["ImageWidth", "ImageLength", "Orientation"]:
                        metadata["image_info"][tag.lower()] = value

                # Extract GPS data
                gps_info = exifdata.get_ifd(34853)  # GPSInfo tag
                if gps_info:
                    gps_data = {}
                    for key in gps_info.keys():
                        decode = GPSTAGS.get(key, key)
                        gps_data[decode] = gps_info[key]

                    location = ImageService.convert_gps_to_decimal(gps_data)
                    if location:
                        metadata["location"] = location

        except Exception as e:
            logger.error(f"Failed to extract EXIF data: {e}")

        # If no timestamp in EXIF, use current time
        if not metadata["timestamp"]:
            metadata["timestamp"] = timezone.now().isoformat()

        return metadata

    @staticmethod
    def convert_gps_to_decimal(gps_data: Dict) -> Optional[Dict[str, float]]:
        """
        Convert GPS coordinates from EXIF format to decimal degrees.

        Args:
            gps_data: GPS information from EXIF

        Returns:
            Dictionary with latitude and longitude in decimal degrees
        """
        try:
            def convert_to_degrees(value):
                """Convert GPS coordinate to decimal degrees."""
                # Handle different formats of GPS data
                if isinstance(value, (list, tuple)) and len(value) >= 3:
                    # Each component might be a tuple (numerator, denominator) or a simple value
                    d = value[0]
                    m = value[1]
                    s = value[2]

                    # Convert each component if it's a tuple
                    if isinstance(d, tuple):
                        d = d[0] / d[1] if d[1] != 0 else 0
                    if isinstance(m, tuple):
                        m = m[0] / m[1] if m[1] != 0 else 0
                    if isinstance(s, tuple):
                        s = s[0] / s[1] if s[1] != 0 else 0

                    return float(d) + (float(m) / 60.0) + (float(s) / 3600.0)
                return 0

            lat = gps_data.get("GPSLatitude")
            lat_ref = gps_data.get("GPSLatitudeRef")
            lon = gps_data.get("GPSLongitude")
            lon_ref = gps_data.get("GPSLongitudeRef")

            if lat and lat_ref and lon and lon_ref:
                latitude = convert_to_degrees(lat)
                if lat_ref == "S":
                    latitude = -latitude

                longitude = convert_to_degrees(lon)
                if lon_ref == "W":
                    longitude = -longitude

                return {
                    "latitude": latitude,
                    "longitude": longitude
                }
        except Exception as e:
            logger.warning(f"Failed to convert GPS data: {e}")

        return None

    @staticmethod
    def save_image(
        image_file: InMemoryUploadedFile,
        user_id: int,
        metadata: Dict[str, Any],
        chakra_type: str
    ) -> ChakraImage:
        """
        Save uploaded image to storage and database.

        Args:
            image_file: The uploaded image file
            user_id: ID of the user uploading the image
            metadata: Extracted metadata
            chakra_type: Type of chakra

        Returns:
            ChakraImage model instance
        """
        from user.models import User

        # Verify user exists
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            raise ValueError(f"User with id {user_id} does not exist")

        timestamp = datetime.fromisoformat(metadata["timestamp"])

        chakra_image = ChakraImage(
            user=user,
            image=image_file,
            chakra_type=chakra_type,
            date=timestamp.date(),
            timestamp=timestamp,
            latitude=metadata.get("location", {}).get("latitude") if metadata.get("location") else None,
            longitude=metadata.get("location", {}).get("longitude") if metadata.get("location") else None,
            device_make=metadata.get("device_info", {}).get("make") if metadata.get("device_info") else None,
            device_model=metadata.get("device_info", {}).get("model") if metadata.get("device_info") else None,
        )
        chakra_image.save()

        return chakra_image

    @staticmethod
    def process_image_upload(
        image_file: InMemoryUploadedFile,
        user_id: int,
        additional_data: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Process complete image upload workflow.

        Args:
            image_file: The uploaded image file
            user_id: ID of the user uploading the image
            additional_data: Optional additional data from the request

        Returns:
            Complete response data including metadata and storage info
        """
        try:
            # Extract metadata
            metadata = ImageService.extract_exif_data(image_file)

            # Reset file pointer after reading EXIF
            image_file.seek(0)

            # Save image
            chakra_type = additional_data.get('chakra_type', 'default') if additional_data else 'default'
            chakra_image = ImageService.save_image(
                image_file, user_id, metadata, chakra_type
            )

            # Prepare response
            return {
                "status": "success",
                "data": {
                    "image_id": chakra_image.id,
                    "file_url": chakra_image.image.url,
                    "metadata": metadata,
                    "uploaded_at": chakra_image.created_at.isoformat(),
                    "user_id": user_id
                }
            }

        except Exception as e:
            logger.error(f"Failed to process image upload: {e}")
            return {
                "status": "error",
                "message": str(e)
            }

    @staticmethod
    def get_user_images_for_date(user_id: int, date: datetime, generate_presigned: bool = True) -> list:
        """
        Retrieve all images uploaded by a user on a specific date.

        Args:
            user_id: ID of the user
            date: The date to query
            generate_presigned: Whether to generate presigned URLs for S3

        Returns:
            List of image data for the specified date
        """
        # Query images from database
        images = ChakraImage.objects.filter(
            user_id=user_id,
            date=date.date()
        ).order_by('-timestamp')

        result = []
        for img in images:
            # Extract S3 key from image field if using S3
            image_url = img.image.url
            if generate_presigned and getattr(settings, 'USE_S3', False):
                # Extract key from image field name
                image_key = img.image.name
                presigned_url = ImageService.generate_view_presigned_url(image_key)
                if presigned_url:
                    image_url = presigned_url

            result.append({
                "id": img.id,
                "url": image_url,
                "chakra_type": img.chakra_type,
                "timestamp": img.timestamp.isoformat(),
                "location": {
                    "latitude": img.latitude,
                    "longitude": img.longitude
                } if img.latitude and img.longitude else None
            })

        return result