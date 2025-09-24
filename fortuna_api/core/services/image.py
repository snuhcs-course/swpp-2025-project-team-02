"""
Image processing service for Fortuna API.
Handles photo uploads, metadata extraction, and storage.
"""

import os
import uuid
import json
from datetime import datetime
from typing import Dict, Any, Optional, Tuple
from PIL import Image
from PIL.ExifTags import TAGS, GPSTAGS
from django.core.files.uploadedfile import InMemoryUploadedFile
from django.conf import settings
from django.core.files.storage import default_storage
import logging

logger = logging.getLogger(__name__)


class ImageService:
    """Service for handling image uploads and metadata extraction."""

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
                            metadata["timestamp"] = datetime.strptime(
                                value, "%Y:%m:%d %H:%M:%S"
                            ).isoformat()
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
                gps_info = exifdata.get(34853)  # GPSInfo tag
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
            metadata["timestamp"] = datetime.now().isoformat()

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
        metadata: Dict[str, Any]
    ) -> Tuple[str, str]:
        """
        Save uploaded image to storage.

        Args:
            image_file: The uploaded image file
            user_id: ID of the user uploading the image
            metadata: Extracted metadata

        Returns:
            Tuple of (file_path, file_url)
        """
        # Generate unique filename
        ext = os.path.splitext(image_file.name)[1]
        filename = f"{uuid.uuid4().hex}{ext}"

        # Create directory structure: media/chakras/user_id/YYYY-MM-DD/filename
        timestamp = datetime.fromisoformat(metadata["timestamp"])
        file_path = os.path.join(
            "chakras",
            str(user_id),
            timestamp.strftime("%Y-%m-%d"),
            filename
        )

        # Save file
        saved_path = default_storage.save(file_path, image_file)
        file_url = default_storage.url(saved_path)

        return saved_path, file_url

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
            file_path, file_url = ImageService.save_image(
                image_file, user_id, metadata
            )

            # Prepare response
            response = {
                "status": "success",
                "data": {
                    "image_id": str(uuid.uuid4()),
                    "file_path": file_path,
                    "file_url": file_url,
                    "metadata": metadata,
                    "uploaded_at": datetime.now().isoformat(),
                    "user_id": user_id
                }
            }

            # Add any additional data from request
            if additional_data:
                response["data"]["additional_data"] = additional_data

            return response

        except Exception as e:
            logger.error(f"Failed to process image upload: {e}")
            return {
                "status": "error",
                "message": str(e)
            }

    @staticmethod
    def get_user_images_for_date(user_id: int, date: datetime) -> list:
        """
        Retrieve all images uploaded by a user on a specific date.

        Args:
            user_id: ID of the user
            date: The date to query

        Returns:
            List of image data for the specified date
        """
        # This would typically query from database
        # For now, return mock data structure
        # In production, this should query from a Django model

        images = []
        date_str = date.strftime("%Y-%m-%d")
        base_path = os.path.join(
            settings.MEDIA_ROOT,
            "chakras",
            str(user_id),
            date_str
        )

        if os.path.exists(base_path):
            for filename in os.listdir(base_path):
                if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.gif')):
                    file_path = os.path.join(base_path, filename)
                    images.append({
                        "filename": filename,
                        "path": file_path,
                        "url": f"{settings.MEDIA_URL}chakras/{user_id}/{date_str}/{filename}"
                    })

        return images