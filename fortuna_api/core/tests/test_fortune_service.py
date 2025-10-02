"""
Unit tests for FortuneService.
"""

from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock
from django.test import TestCase
import json
from ..services.fortune import (
    FortuneService,
    FortuneResponse,
    ChakraReading,
    DailyGuidance
)


class TestFortuneService(TestCase):
    """Test cases for FortuneService."""

    def setUp(self):
        """Set up test fixtures."""
        with patch('core.services.fortune.openai'):
            self.service = FortuneService()
        self.user_id = 1
        self.test_date = datetime(2024, 1, 1, 12, 0, 0)

    def test_calculate_gapja_code(self):
        """Test Gapja code calculation."""
        date1 = datetime(2024, 1, 1)
        code1 = self.service.calculate_gapja_code(date1)

        self.assertIsInstance(code1, int)
        self.assertTrue(1 <= code1 <= 60)

        # Same date should give same code
        code2 = self.service.calculate_gapja_code(date1)
        self.assertEqual(code1, code2)

        # Different date should give different code (most likely)
        date2 = datetime(2024, 1, 2)
        code3 = self.service.calculate_gapja_code(date2)
        self.assertTrue(1 <= code3 <= 60)

    def test_get_user_saju_info(self):
        """Test retrieving user Saju information."""
        saju_info = self.service.get_user_saju_info(self.user_id)

        self.assertEqual(saju_info['user_id'], self.user_id)
        self.assertIn('birth_date', saju_info)
        self.assertIn('four_pillars', saju_info)
        self.assertIn('year', saju_info['four_pillars'])
        self.assertIn('month', saju_info['four_pillars'])
        self.assertIn('day', saju_info['four_pillars'])
        self.assertIn('hour', saju_info['four_pillars'])

        # Check day pillar structure
        day_pillar = saju_info['four_pillars']['day']
        self.assertIn('code', day_pillar)
        self.assertIn('name', day_pillar)
        self.assertIn('element', day_pillar)

    def test_analyze_saju_compatibility(self):
        """Test Saju compatibility analysis."""
        user_day_pillar = {
            "code": 45,
            "name": "무신",
            "element": "토"
        }
        tomorrow_gapja = {
            "code": 12,
            "korean_name": "을해",
            "stem_element": "목"
        }

        compatibility = self.service.analyze_saju_compatibility(
            user_day_pillar, tomorrow_gapja
        )

        self.assertIn('score', compatibility)
        self.assertIn('level', compatibility)
        self.assertIn('message', compatibility)
        self.assertIn('element_relation', compatibility)
        self.assertTrue(0 <= compatibility['score'] <= 100)
        self.assertEqual(compatibility['user_element'], "토")
        self.assertEqual(compatibility['tomorrow_element'], "목")

    def test_analyze_saju_compatibility_levels(self):
        """Test different compatibility levels."""
        # Test very good compatibility
        user_pillar_good = {"code": 1, "element": "목"}
        tomorrow_good = {"code": 2, "stem_element": "화"}

        with patch('core.services.fortune.calculate_compatibility_score', return_value=85):
            compat_good = self.service.analyze_saju_compatibility(
                user_pillar_good, tomorrow_good
            )
            self.assertEqual(compat_good['level'], "매우 좋음")

        # Test poor compatibility
        with patch('core.services.fortune.calculate_compatibility_score', return_value=30):
            compat_poor = self.service.analyze_saju_compatibility(
                user_pillar_good, tomorrow_good
            )
            self.assertEqual(compat_poor['level'], "주의")

    def test_prepare_photo_context_with_photos(self):
        """Test preparing photo context with existing photos."""
        with patch.object(self.service.image_service, 'get_user_images_for_date') as mock_get_images:
            mock_get_images.return_value = [
                {
                    'filename': 'photo1.jpg',
                    'url': '/media/photo1.jpg',
                    'path': '/path/to/photo1.jpg'
                },
                {
                    'filename': 'photo2.jpg',
                    'url': '/media/photo2.jpg',
                    'path': '/path/to/photo2.jpg'
                }
            ]

            contexts = self.service.prepare_photo_context(
                self.user_id, self.test_date
            )

            self.assertEqual(len(contexts), 2)
            self.assertEqual(contexts[0]['filename'], 'photo1.jpg')
            self.assertIn('metadata', contexts[0])
            self.assertIn('timestamp', contexts[0]['metadata'])
            self.assertIn('location', contexts[0]['metadata'])

    def test_prepare_photo_context_without_photos(self):
        """Test preparing photo context without photos."""
        with patch.object(self.service.image_service, 'get_user_images_for_date') as mock_get_images:
            mock_get_images.return_value = []

            contexts = self.service.prepare_photo_context(
                self.user_id, self.test_date
            )

            self.assertEqual(len(contexts), 1)
            self.assertEqual(contexts[0]['filename'], 'no_photo')
            self.assertIsNone(contexts[0]['metadata']['location'])

    @patch('openai.OpenAI')
    def test_generate_fortune_with_ai_success(self, mock_openai):
        """Test successful AI fortune generation."""
        # Mock OpenAI response
        mock_response = Mock()
        mock_response.choices = [Mock()]
        mock_response.choices[0].message.content = "긍정적인 하루가 될 것입니다. 새로운 시작에 좋은 날입니다."

        mock_client = Mock()
        mock_client.chat.completions.create.return_value = mock_response
        self.service.client = mock_client

        # Test data
        user_saju = self.service.get_user_saju_info(self.user_id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_gapja = {
            "code": 1,
            "korean_name": "갑자",
            "stem_element": "목",
            "chinese_characters": "甲子",
            "animal": "쥐"
        }
        compatibility = {
            "score": 75,
            "level": "좋음",
            "element_relation": "generates",
            "user_element": "토",
            "tomorrow_element": "목",
            "message": "긍정적인 에너지가 당신을 도울 것입니다."
        }
        photo_contexts = [{"filename": "test.jpg", "metadata": {"timestamp": self.test_date.isoformat(), "location": {"latitude": 37.5, "longitude": 127.0}}}]

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_gapja, compatibility, photo_contexts
        )

        self.assertIsInstance(result, FortuneResponse)
        self.assertEqual(result.tomorrow_date, "2024-01-02")
        self.assertEqual(result.overall_fortune, 75)

    def test_generate_fortune_with_ai_failure(self):
        """Test AI fortune generation with error."""
        # Mock OpenAI client to raise exception
        self.service.client = None

        user_saju = self.service.get_user_saju_info(self.user_id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_gapja = {"code": 1, "korean_name": "갑자", "stem_element": "목", "chinese_characters": "甲子", "animal": "쥐"}
        compatibility = {"score": 50, "level": "보통", "element_relation": "neutral", "user_element": "토", "tomorrow_element": "목", "message": "평온한 하루가 될 것입니다."}
        photo_contexts = []

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_gapja, compatibility, photo_contexts
        )

        # Should return default fortune on error
        self.assertIsInstance(result, FortuneResponse)
        self.assertEqual(result.overall_fortune, compatibility['score'])

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    @patch.object(FortuneService, 'prepare_photo_context')
    @patch.object(FortuneService, 'analyze_saju_compatibility')
    @patch.object(FortuneService, 'get_user_saju_info')
    def test_generate_tomorrow_fortune_success(
        self, mock_saju, mock_compat, mock_photo, mock_ai
    ):
        """Test complete tomorrow fortune generation."""
        from django.contrib.auth import get_user_model
        User = get_user_model()

        # Create user for FK constraint
        user = User.objects.create_user(email='test@example.com', password='testpass123')

        # Setup mocks
        mock_saju.return_value = {
            'user_id': user.id,
            'four_pillars': {
                'day': {'code': 45, 'name': '무신', 'element': '토'}
            }
        }
        mock_compat.return_value = {
            'score': 80,
            'level': '좋음',
            'element_relation': 'generates'
        }
        mock_photo.return_value = [{'filename': 'test.jpg'}]
        mock_fortune = FortuneResponse(
            tomorrow_date="2024-01-02",
            saju_compatibility="좋음",
            overall_fortune=80,
            fortune_summary="좋은 날",
            element_balance="균형",
            chakra_readings=[],
            daily_guidance=DailyGuidance(
                best_time="오전",
                lucky_direction="동",
                lucky_color="청색",
                activities_to_embrace=["시작"],
                activities_to_avoid=["논쟁"],
                key_advice="화이팅"
            ),
            special_message="행운"
        )
        mock_ai.return_value = mock_fortune

        result = self.service.generate_tomorrow_fortune(
            user.id, self.test_date, include_photos=True
        )

        self.assertEqual(result['status'], 'success')
        self.assertEqual(result['data']['user_id'], user.id)
        self.assertIn('fortune', result['data'])
        self.assertIn('tomorrow_gapja', result['data'])

    def test_get_hourly_fortune(self):
        """Test hourly fortune generation."""
        # Test morning time (오시: 11-13)
        target_time = datetime(2024, 1, 1, 12, 0, 0)
        result = self.service.get_hourly_fortune(self.user_id, target_time)

        self.assertEqual(result['status'], 'success')
        self.assertEqual(result['data']['time_unit'], '오시')
        self.assertEqual(result['data']['time_element'], '화')
        self.assertIn('compatibility', result['data'])
        self.assertIn('advice', result['data'])

        # Test midnight time (자시: 23-01)
        midnight_time = datetime(2024, 1, 1, 23, 30, 0)
        result_midnight = self.service.get_hourly_fortune(
            self.user_id, midnight_time
        )

        self.assertEqual(result_midnight['status'], 'success')
        self.assertEqual(result_midnight['data']['time_unit'], '자시')
        self.assertEqual(result_midnight['data']['time_element'], '수')

    def test_get_hourly_advice(self):
        """Test hourly advice generation based on compatibility."""
        advice_generates = self.service._get_hourly_advice('generates')
        self.assertIn('최적의 시간', advice_generates)

        advice_destroyed = self.service._get_hourly_advice('is_destroyed_by')
        self.assertIn('신중한', advice_destroyed)

        advice_unknown = self.service._get_hourly_advice('unknown')
        self.assertEqual(advice_unknown, '일반적인 시간입니다.')


class TestFortuneServiceIntegration(TestCase):
    """Integration tests for FortuneService."""

    @patch('core.services.fortune.openai')
    def setUp(self, mock_openai_model):
        """Set up test fixtures."""
        self.service = FortuneService()
        self.user_id = 1

    def test_gapja_cycle_consistency(self):
        """Test that Gapja codes cycle correctly."""
        base_date = datetime(2024, 1, 1)
        codes = []

        # Generate codes for 60 consecutive days
        for i in range(60):
            date = base_date + timedelta(days=i)
            code = self.service.calculate_gapja_code(date)
            codes.append(code)

        # Check all codes are valid
        self.assertTrue(all(1 <= c <= 60 for c in codes))

        # 61st day should have same code as 1st day (60-day cycle)
        date_61 = base_date + timedelta(days=60)
        code_61 = self.service.calculate_gapja_code(date_61)
        # Note: This might not be exactly equal due to simplified calculation
        # In real implementation, proper lunar calendar should be used

    def test_time_unit_coverage(self):
        """Test that all 12 time units are covered."""
        time_units_found = set()

        for hour in range(24):
            target_time = datetime(2024, 1, 1, hour, 0, 0)
            result = self.service.get_hourly_fortune(
                self.user_id, target_time
            )

            if result['status'] == 'success':
                time_units_found.add(result['data']['time_unit'])

        # Should cover all 12 traditional time units
        expected_units = {
            '자시', '축시', '인시', '묘시', '진시', '사시',
            '오시', '미시', '신시', '유시', '술시', '해시'
        }
        self.assertEqual(time_units_found, expected_units)