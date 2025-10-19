"""
Unit tests for FortuneService.
"""

from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock
from django.test import TestCase
import json
from ..services.fortune import (
    FortuneService,
    FortuneAIResponse,
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

    def test_calculate_day_ganji(self):
        """Test day pillar (GanJi) calculation."""
        from core.utils.saju_concepts import GanJi

        date1 = datetime(2024, 1, 1)
        ganji1 = self.service.calculate_day_ganji(date1)

        self.assertIsInstance(ganji1, GanJi)
        self.assertIsNotNone(ganji1.stem)
        self.assertIsNotNone(ganji1.branch)
        self.assertEqual(len(ganji1.two_letters), 2)

        # Same date should give same ganji
        ganji2 = self.service.calculate_day_ganji(date1)
        self.assertEqual(ganji1.two_letters, ganji2.two_letters)

        # Different date should give different ganji (most likely)
        date2 = datetime(2024, 1, 2)
        ganji3 = self.service.calculate_day_ganji(date2)
        self.assertIsInstance(ganji3, GanJi)

    def test_get_user_saju_info(self):
        """Test retrieving user Saju information."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import Saju

        User = get_user_model()

        # Create a user with complete saju data
        user = User.objects.create_user(
            email='testuser@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        saju = self.service.get_user_saju_info(user.id)

        self.assertIsInstance(saju, Saju)
        self.assertIsNotNone(saju.yearly)
        self.assertIsNotNone(saju.monthly)
        self.assertIsNotNone(saju.daily)
        self.assertIsNotNone(saju.hourly)
        self.assertEqual(saju.yearly.two_letters, '갑자')
        self.assertEqual(saju.daily.two_letters, '무신')

    def test_analyze_saju_compatibility(self):
        """Test Saju compatibility analysis."""
        from core.utils.saju_concepts import GanJi

        # 무신 (土) and 을해 (木)
        user_day_ganji = GanJi.find_by_name("무신")
        tomorrow_day_ganji = GanJi.find_by_name("을해")

        compatibility = self.service.analyze_saju_compatibility(
            user_day_ganji, tomorrow_day_ganji
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
        from core.utils.saju_concepts import GanJi

        # Test empowers (상생) - 목생화 (Wood empowers Fire)
        # 갑인 (木) empowers 병오 (火) → high score
        user_ganji_empowers = GanJi.find_by_name("갑인")  # 木
        tomorrow_ganji_empowers = GanJi.find_by_name("병오")  # 火

        compat_empowers = self.service.analyze_saju_compatibility(
            user_ganji_empowers, tomorrow_ganji_empowers
        )
        self.assertGreater(compat_empowers['score'], 65)  # Should have bonus
        self.assertEqual(compat_empowers['element_relation'], "상생 (相生)")

        # Test weakens (상극) - 목극토 (Wood weakens Earth)
        # 갑인 (木) weakens 무진 (土) → low score
        user_ganji_weakens = GanJi.find_by_name("갑인")  # 木
        tomorrow_ganji_weakens = GanJi.find_by_name("무진")  # 土

        compat_weakens = self.service.analyze_saju_compatibility(
            user_ganji_weakens, tomorrow_ganji_weakens
        )
        self.assertLess(compat_weakens['score'], 45)  # Should have penalty
        self.assertEqual(compat_weakens['element_relation'], "상극 (相剋)")

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
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import Saju, GanJi

        User = get_user_model()

        # Create a user with complete saju data
        user = User.objects.create_user(
            email='aitest@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Mock OpenAI response
        mock_response = Mock()
        mock_response.choices = [Mock()]
        mock_response.choices[0].message.content = "긍정적인 하루가 될 것입니다. 새로운 시작에 좋은 날입니다."

        mock_client = Mock()
        mock_client.chat.completions.create.return_value = mock_response
        self.service.client = mock_client

        # Test data
        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")

        compatibility = {
            "score": 75,
            "level": "좋음",
            "element_relation": "상생 (相生)",
            "relation_detail": "목이 화를 도와줍니다",
            "user_element": "토",
            "user_element_color": "노란",
            "tomorrow_element": "목",
            "tomorrow_element_color": "푸른",
            "user_ganji": "무신",
            "tomorrow_ganji": "갑자",
            "message": "긍정적인 에너지가 당신을 도울 것입니다."
        }
        photo_contexts = [{"filename": "test.jpg", "metadata": {"timestamp": self.test_date.isoformat(), "location": {"latitude": 37.5, "longitude": 127.0}}}]

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_day_ganji, compatibility, photo_contexts
        )

        self.assertIsInstance(result, FortuneAIResponse)
        self.assertEqual(result.tomorrow_date, "2024-01-02")
        self.assertEqual(result.overall_fortune, 75)

    def test_generate_fortune_with_ai_failure(self):
        """Test AI fortune generation with error."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import GanJi

        User = get_user_model()

        # Create a user with complete saju data
        user = User.objects.create_user(
            email='aifailtest@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Mock OpenAI client to raise exception
        self.service.client = None

        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")
        compatibility = {
            "score": 50,
            "level": "보통",
            "element_relation": "중립",
            "relation_detail": "중립적인 관계",
            "user_element": "토",
            "user_element_color": "노란",
            "tomorrow_element": "목",
            "tomorrow_element_color": "푸른",
            "user_ganji": "무신",
            "tomorrow_ganji": "갑자",
            "message": "평온한 하루가 될 것입니다."
        }
        photo_contexts = []

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_day_ganji, compatibility, photo_contexts
        )

        # Should return default fortune on error
        self.assertIsInstance(result, FortuneAIResponse)
        self.assertEqual(result.overall_fortune, compatibility['score'])

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    @patch.object(FortuneService, 'prepare_photo_context')
    @patch.object(FortuneService, 'analyze_saju_compatibility')
    def test_generate_tomorrow_fortune_success(
        self, mock_compat, mock_photo, mock_ai
    ):
        """Test complete tomorrow fortune generation."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import Saju

        User = get_user_model()

        # Create user for FK constraint with complete saju data
        user = User.objects.create_user(
            email='fortunetest@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Setup mocks
        mock_compat.return_value = {
            'score': 80,
            'level': '좋음',
            'element_relation': '상생 (相生)',
            'relation_detail': '목이 화를 도와줍니다',
            'user_element': '토',
            'user_element_color': '노란',
            'tomorrow_element': '목',
            'tomorrow_element_color': '푸른',
            'user_ganji': '무신',
            'tomorrow_ganji': '갑자'
        }
        mock_photo.return_value = [{'filename': 'test.jpg'}]
        mock_fortune = FortuneAIResponse(
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

class TestFortuneServiceIntegration(TestCase):
    """Integration tests for FortuneService."""

    @patch('core.services.fortune.openai')
    def setUp(self, mock_openai_model):
        """Set up test fixtures."""
        self.service = FortuneService()
        self.user_id = 1

    def test_ganji_cycle_consistency(self):
        """Test that day pillar (GanJi) cycles correctly."""
        from core.utils.saju_concepts import GanJi

        base_date = datetime(2024, 1, 1)
        ganjis = []

        # Generate ganjis for 60 consecutive days
        for i in range(60):
            date = base_date + timedelta(days=i)
            ganji = self.service.calculate_day_ganji(date)
            ganjis.append(ganji.two_letters)

        # Check all ganjis are valid (60 unique combinations in cycle)
        self.assertEqual(len(set(ganjis)), 60)

        # 61st day should have same ganji as 1st day (60-day cycle)
        date_61 = base_date + timedelta(days=60)
        ganji_61 = self.service.calculate_day_ganji(date_61)
        ganji_1 = self.service.calculate_day_ganji(base_date)
        self.assertEqual(ganji_61.two_letters, ganji_1.two_letters)