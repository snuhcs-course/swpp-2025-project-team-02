"""
DRF Serializers for Fortuna Core API.
"""

from rest_framework import serializers
from .models import ChakraImage, FortuneResult
from typing import Any, Dict, List, Optional


# ============================================================================
# Chakra Image Serializers
# ============================================================================

class ChakraImageSerializer(serializers.ModelSerializer):
    """Serializer for ChakraImage model."""

    class Meta:
        model = ChakraImage
        fields = [
            'id',
            'chakra_type',
            'date',
            'timestamp',
            'latitude',
            'longitude',
            'device_make',
            'device_model',
            'image',
            'created_at',
        ]
        read_only_fields = ['id', 'created_at', 'timestamp', 'latitude', 'longitude']


class ChakraImageUploadSerializer(serializers.Serializer):
    """Serializer for chakra image upload request."""

    image = serializers.ImageField(required=True)
    chakra_type = serializers.CharField(
        max_length=50,
        default='default',
        required=False
    )


class PresignedURLRequestSerializer(serializers.Serializer):
    """Serializer for presigned URL request."""

    chakra_type = serializers.CharField(
        max_length=50,
        default='default',
        required=False
    )


class PresignedURLDataSerializer(serializers.Serializer):
    """Serializer for presigned URL response data."""

    upload_url = serializers.URLField()
    key = serializers.CharField()
    file_id = serializers.CharField()
    expires_in = serializers.IntegerField()


class ImageListSerializer(serializers.Serializer):
    """Serializer for image list response."""

    filename = serializers.CharField()
    url = serializers.URLField()


# ============================================================================
# Fortune Serializers (based on Pydantic models)
# ============================================================================

class TomorrowGapjaSerializer(serializers.Serializer):
    """Serializer for tomorrow's gapja information."""

    code = serializers.IntegerField()
    name = serializers.CharField()
    stem = serializers.CharField()
    branch = serializers.CharField()
    element = serializers.CharField()
    element_color = serializers.CharField()
    animal = serializers.CharField()


class ChakraReadingSerializer(serializers.Serializer):
    """Serializer for individual Chakra reading."""

    chakra_type = serializers.CharField()
    strength = serializers.IntegerField(min_value=1, max_value=100)
    message = serializers.CharField()
    location_significance = serializers.CharField()


class DailyGuidanceSerializer(serializers.Serializer):
    """Serializer for daily guidance."""

    best_time = serializers.CharField()
    lucky_direction = serializers.CharField()
    lucky_color = serializers.CharField()
    activities_to_embrace = serializers.ListField(child=serializers.CharField())
    activities_to_avoid = serializers.ListField(child=serializers.CharField())
    key_advice = serializers.CharField()


class FortuneAIResponseSerializer(serializers.Serializer):
    """Serializer for complete AI fortune response."""

    tomorrow_date = serializers.DateField(format='%Y-%m-%d')
    saju_compatibility = serializers.CharField()
    overall_fortune = serializers.IntegerField(min_value=1, max_value=100)
    fortune_summary = serializers.CharField()
    element_balance = serializers.CharField()
    chakra_readings = ChakraReadingSerializer(many=True)
    daily_guidance = DailyGuidanceSerializer()
    special_message = serializers.CharField()


class GanjiSerializer(serializers.Serializer):
    """Serializer for Ganji (간지) information."""

    name = serializers.CharField()
    stem = serializers.CharField()
    branch = serializers.CharField()
    element = serializers.CharField()


class ElementDistributionSerializer(serializers.Serializer):
    """Serializer for element distribution."""

    count = serializers.IntegerField()
    percentage = serializers.FloatField()


class FortuneScoreSerializer(serializers.Serializer):
    """Serializer for fortune score with entropy-based balance."""

    entropy_score = serializers.FloatField()
    elements = serializers.DictField(
        child=GanjiSerializer(allow_null=True),
        help_text="8 pillars: 대운, 세운, 월운, 일운, 년주, 월주, 일주, 시주"
    )
    element_distribution = serializers.DictField(
        child=ElementDistributionSerializer(),
        help_text="Distribution of 5 elements: 목, 화, 토, 금, 수"
    )
    interpretation = serializers.CharField()


class FortuneResultSerializer(serializers.ModelSerializer):
    """Serializer for FortuneResult model."""

    class Meta:
        model = FortuneResult
        fields = [
            'id',
            'user',
            'for_date',
            'gapja_code',
            'gapja_name',
            'gapja_element',
            'fortune_data',
            'status',
            'created_at',
            'updated_at',
        ]
        read_only_fields = ['id', 'created_at', 'updated_at']


# ============================================================================
# Request Serializers
# ============================================================================

class FortuneRequestSerializer(serializers.Serializer):
    """Serializer for fortune request parameters."""

    date = serializers.DateField(
        required=False,
        input_formats=['%Y-%m-%d'],
        error_messages={
            'invalid': 'Invalid date format. Use YYYY-MM-DD'
        },
        help_text="Date in YYYY-MM-DD format. Defaults to today."
    )
    include_photos = serializers.BooleanField(
        default=True,
        required=False,
        help_text="Include photo analysis in fortune generation"
    )


class ImageListRequestSerializer(serializers.Serializer):
    """Serializer for image list request parameters."""

    date = serializers.DateField(
        required=True,
        input_formats=['%Y-%m-%d'],
        error_messages={
            'required': 'Date parameter is required',
            'invalid': 'Invalid date format. Use YYYY-MM-DD'
        },
        help_text="Date in YYYY-MM-DD format"
    )


# ============================================================================
# Response Serializers
# ============================================================================

class APIResponseSerializer(serializers.Serializer):
    """Generic API response serializer."""

    status = serializers.ChoiceField(choices=['success', 'error'])
    message = serializers.CharField(required=False, allow_null=True)
    data = serializers.DictField(required=False, allow_null=True)


class PresignedURLResponseSerializer(serializers.Serializer):
    """Serializer for presigned URL response."""

    status = serializers.CharField()
    data = PresignedURLDataSerializer()


class ImageUploadResponseSerializer(serializers.Serializer):
    """Serializer for image upload response."""

    status = serializers.CharField()
    data = serializers.DictField()


class ImageListResponseSerializer(serializers.Serializer):
    """Serializer for image list response."""

    status = serializers.CharField()
    data = serializers.DictField(
        child=serializers.DictField()
    )


class FortuneResponseDataSerializer(serializers.Serializer):
    """Serializer for fortune response data."""

    fortune_id = serializers.IntegerField()
    user_id = serializers.IntegerField()
    generated_at = serializers.DateTimeField()
    for_date = serializers.DateField(format='%Y-%m-%d')
    tomorrow_gapja = TomorrowGapjaSerializer()
    fortune = FortuneAIResponseSerializer()
    fortune_score = FortuneScoreSerializer(required=False)


class FortuneResponseSerializer(serializers.Serializer):
    """Serializer for complete fortune response."""

    status = serializers.CharField()
    data = FortuneResponseDataSerializer()
