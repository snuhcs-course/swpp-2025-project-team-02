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


class ChakraCollectSerializer(serializers.Serializer):
    """Serializer for PoC chakra collection (no image required)."""

    CHAKRA_TYPE_CHOICES = ['fire', 'water', 'earth', 'metal', 'wood']

    chakra_type = serializers.ChoiceField(
        choices=CHAKRA_TYPE_CHOICES,
        required=True,
        error_messages={
            'required': 'chakra_type is required',
            'invalid_choice': 'chakra_type must be one of: fire, water, earth, metal, wood'
        }
    )


class ChakraCollectionItemSerializer(serializers.Serializer):
    """Serializer for individual chakra collection count."""

    chakra_type = serializers.CharField()
    count = serializers.IntegerField()


class ChakraCollectResponseDataSerializer(serializers.Serializer):
    """Serializer for chakra collect response data."""

    id = serializers.IntegerField()
    chakra_type = serializers.CharField()
    collected_at = serializers.DateTimeField()


class ChakraCollectResponseSerializer(serializers.Serializer):
    """Serializer for chakra collect response."""

    status = serializers.CharField()
    data = ChakraCollectResponseDataSerializer()


class ChakraCollectionStatusDataSerializer(serializers.Serializer):
    """Serializer for chakra collection status data."""

    collections = ChakraCollectionItemSerializer(many=True)
    total_count = serializers.IntegerField()


class ChakraCollectionStatusSerializer(serializers.Serializer):
    """Serializer for chakra collection status response."""

    status = serializers.CharField()
    data = ChakraCollectionStatusDataSerializer()


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


class StemSerializer(serializers.Serializer):
    """Serializer for Heavenly Stem (천간) information."""

    korean_name = serializers.CharField(help_text="Korean name (e.g., '갑')")
    element = serializers.CharField(help_text="Five element (e.g., '木')")
    element_color = serializers.CharField(help_text="Element color")
    yin_yang = serializers.CharField(help_text="Yin/Yang (음/양)")


class BranchSerializer(serializers.Serializer):
    """Serializer for Earthly Branch (지지) information."""

    korean_name = serializers.CharField(help_text="Korean name (e.g., '자')")
    element = serializers.CharField(help_text="Five element (e.g., '水')")
    element_color = serializers.CharField(help_text="Element color")
    animal = serializers.CharField(help_text="Zodiac animal (e.g., '쥐')")
    yin_yang = serializers.CharField(help_text="Yin/Yang (음/양)")


class GanjiSerializer(serializers.Serializer):
    """Serializer for Ganji (간지) with full stem and branch information."""

    two_letters = serializers.CharField(help_text="Ganji name (e.g., '갑자')")
    stem = StemSerializer(help_text="Heavenly stem details")
    branch = BranchSerializer(help_text="Earthly branch details")


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
    """Serializer for complete fortune response (deprecated)."""

    status = serializers.CharField()
    data = FortuneResponseDataSerializer()


# ============================================================================
# New Fortune Response Serializers (Pydantic-based)
# ============================================================================

class SajuSerializer(serializers.Serializer):
    """Serializer for complete Saju (four pillars)."""

    yearly = GanjiSerializer(help_text="Year pillar (년주)")
    monthly = GanjiSerializer(help_text="Month pillar (월주)")
    daily = GanjiSerializer(help_text="Day pillar (일주)")
    hourly = GanjiSerializer(help_text="Hour pillar (시주)")


class TodayFortuneResponseDataSerializer(serializers.Serializer):
    """Serializer for today's fortune response data."""

    date = serializers.DateField(format='%Y-%m-%d', help_text="Date for fortune")
    user_id = serializers.IntegerField(help_text="User ID")
    fortune = FortuneAIResponseSerializer(help_text="AI-generated fortune")
    fortune_score = FortuneScoreSerializer(help_text="Five elements balance score")
    saju_date = SajuSerializer(help_text="Saju calculated from date")
    saju_user = SajuSerializer(help_text="User's birth Saju")
    daewoon = GanjiSerializer(allow_null=True, help_text="Current Daewoon (대운)")


class TodayFortuneResponseSerializer(serializers.Serializer):
    """Serializer for /fortune/today endpoint response."""

    status = serializers.CharField()
    data = TodayFortuneResponseDataSerializer(required=False, allow_null=True)
    error = serializers.DictField(required=False, allow_null=True)
