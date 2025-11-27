"""
Unit tests for FortuneService.
"""

from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock
from django.test import TestCase
import json
import base64
from ..services.fortune import (
    FortuneService,
    FortuneAIResponse
)


class TestFortuneService(TestCase):
    """Test cases for FortuneService."""

    def setUp(self):
        """Set up test fixtures."""
        # Patch both OpenAI and Gemini modules
        with patch('core.services.fortune.openai'), \
             patch('core.services.fortune.GEMINI_AVAILABLE', True), \
             patch('core.services.fortune.genai'), \
             patch('core.services.fortune.Image'):
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

        # Mock OpenAI response with parsed structure
        mock_parsed = FortuneAIResponse(
            today_fortune_summary="오늘은 조화로운 날! 수의 기운을 모아 균형을 찾아보세요.",
            today_element_balance_description="당신의 토행과 오늘의 목행이 만나 조화를 이룹니다. 긍정적인 하루가 될 것입니다.",
            today_daily_guidance="새로운 시작에 좋은 날입니다. 창의적인 활동을 시도해보세요."
        )
        mock_response = Mock()
        mock_response.choices = [Mock()]
        mock_response.choices[0].message.parsed = mock_parsed

        mock_client = Mock()
        mock_client.chat.completions.parse.return_value = mock_response
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

        # Create fortune_score mock
        from core.services.fortune import FortuneScore, ElementDistribution
        fortune_score = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=4, percentage=26.7),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=2, percentage=13.3)
            },
            interpretation="Test interpretation",
            needed_element="수"
        )

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_day_ganji, compatibility, fortune_score
        )

        self.assertIsInstance(result, FortuneAIResponse)
        self.assertIsNotNone(result.today_element_balance_description)
        self.assertIsNotNone(result.today_daily_guidance)
        self.assertIn("토행", result.today_element_balance_description)
        self.assertIn("목행", result.today_element_balance_description)

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

        # Create fortune_score mock
        from core.services.fortune import FortuneScore, ElementDistribution
        fortune_score = FortuneScore(
            entropy_score=50.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=3, percentage=20.0)
            },
            interpretation="Test interpretation",
            needed_element="목"
        )

        result = self.service.generate_fortune_with_ai(
            user_saju, tomorrow_date, tomorrow_day_ganji, compatibility, fortune_score
        )

        # Should return default fortune on error
        self.assertIsInstance(result, FortuneAIResponse)
        self.assertIsNotNone(result.today_element_balance_description)
        self.assertIsNotNone(result.today_daily_guidance)
        self.assertIn(compatibility['user_element'], result.today_element_balance_description)
        self.assertIn(compatibility['tomorrow_element'], result.today_element_balance_description)

    def test_get_element_color(self):
        """Test _get_element_color helper method."""
        self.assertEqual(self.service._get_element_color("목"), "초록색, 청색")
        self.assertEqual(self.service._get_element_color("화"), "빨간색, 주황색")
        self.assertEqual(self.service._get_element_color("토"), "노란색, 갈색")
        self.assertEqual(self.service._get_element_color("금"), "흰색, 회색")
        self.assertEqual(self.service._get_element_color("수"), "검은색, 파란색")
        # Invalid element should return default
        self.assertEqual(self.service._get_element_color("invalid"), "무지개색")

    @patch('core.services.fortune.Image')
    @patch('core.services.fortune.genai')
    def test_generate_fortune_image_with_ai_success(self, mock_genai, mock_image_module):
        """Test successful fortune image generation with AI using Gemini."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import GanJi
        from core.services.fortune import FortuneScore, ElementDistribution

        User = get_user_model()

        # Create a user with complete saju data
        user = User.objects.create_user(
            email='imagetest@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Create a mock image bytes (PNG format)
        mock_image_bytes = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\rIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82'

        # Mock PIL Image
        mock_pil_image = Mock()
        mock_image_module.open.return_value = mock_pil_image

        # Mock Gemini API response structure
        mock_inline_data = Mock()
        mock_inline_data.data = mock_image_bytes

        mock_part = Mock()
        mock_part.inline_data = mock_inline_data

        mock_content = Mock()
        mock_content.parts = [mock_part]

        mock_candidate = Mock()
        mock_candidate.content = mock_content

        mock_response = Mock()
        mock_response.candidates = [mock_candidate]

        # Mock Gemini client
        mock_gemini_client = Mock()
        mock_gemini_client.models.generate_content.return_value = mock_response
        self.service.gemini_client = mock_gemini_client

        # Create test data
        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")

        fortune_response = FortuneAIResponse(
            today_fortune_summary="오늘은 재물운이 좋은 날! 수의 기운을 모아보세요.",
            today_element_balance_description="오행 균형 설명",
            today_daily_guidance="일상 가이드"
        )

        fortune_score = FortuneScore(
            entropy_score=75.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=4, percentage=26.7),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=2, percentage=13.3)
            },
            interpretation="Test interpretation",
            needed_element="수"
        )

        # Call the method
        result = self.service.generate_fortune_image_with_ai(
            fortune_response,
            user_saju,
            tomorrow_date,
            tomorrow_day_ganji,
            fortune_score
        )

        # Verify result
        self.assertIsNotNone(result)
        self.assertIsInstance(result, bytes)
        self.assertGreater(len(result), 0)

        # Verify Gemini API was called
        self.assertTrue(mock_gemini_client.models.generate_content.called)

        # Get the call args
        call_args = mock_gemini_client.models.generate_content.call_args

        # Verify model name
        self.assertEqual(call_args[1]['model'], 'gemini-2.5-flash-image')

        # Verify prompt is in contents
        self.assertIn('contents', call_args[1])
        contents = call_args[1]['contents']

        # First element should be the prompt string
        prompt_text = contents[0] if isinstance(contents[0], str) else str(contents[0])

        # Verify prompt contains key elements
        self.assertIn("물 (Water)", prompt_text)  # needed_element_desc
        self.assertIn("오행 균형 설명", prompt_text)  # today_element_balance_description content

    def test_generate_fortune_image_with_ai_no_client(self):
        """Test fortune image generation when Gemini client is not initialized."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import GanJi
        from core.services.fortune import FortuneScore, ElementDistribution

        User = get_user_model()

        user = User.objects.create_user(
            email='imagenoclient@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Set gemini_client to None
        self.service.gemini_client = None

        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")

        fortune_response = FortuneAIResponse(
            today_fortune_summary="테스트 요약",
            today_element_balance_description="균형 설명",
            today_daily_guidance="일상 가이드"
        )

        fortune_score = FortuneScore(
            entropy_score=50.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=3, percentage=20.0)
            },
            interpretation="Test",
            needed_element="목"
        )

        result = self.service.generate_fortune_image_with_ai(
            fortune_response,
            user_saju,
            tomorrow_date,
            tomorrow_day_ganji,
            fortune_score
        )

        # Should return None on error
        self.assertIsNone(result)

    @patch('core.services.fortune.Image')
    @patch('core.services.fortune.genai')
    def test_generate_fortune_image_with_ai_no_image_data(self, mock_genai, mock_image_module):
        """Test fortune image generation when Gemini API returns no image data."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import GanJi
        from core.services.fortune import FortuneScore, ElementDistribution

        User = get_user_model()

        user = User.objects.create_user(
            email='imagenodata@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Mock PIL Image
        mock_pil_image = Mock()
        mock_image_module.open.return_value = mock_pil_image

        # Mock Gemini API response with empty candidates
        mock_response = Mock()
        mock_response.candidates = []  # No candidates

        mock_gemini_client = Mock()
        mock_gemini_client.models.generate_content.return_value = mock_response
        self.service.gemini_client = mock_gemini_client

        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")

        fortune_response = FortuneAIResponse(
            today_fortune_summary="테스트 요약",
            today_element_balance_description="균형 설명",
            today_daily_guidance="일상 가이드"
        )

        fortune_score = FortuneScore(
            entropy_score=50.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=3, percentage=20.0)
            },
            interpretation="Test",
            needed_element="목"
        )

        result = self.service.generate_fortune_image_with_ai(
            fortune_response,
            user_saju,
            tomorrow_date,
            tomorrow_day_ganji,
            fortune_score
        )

        # Should return None when no image data
        self.assertIsNone(result)

    @patch('core.services.fortune.Image')
    @patch('core.services.fortune.genai')
    def test_generate_fortune_image_with_ai_api_exception(self, mock_genai, mock_image_module):
        """Test fortune image generation when Gemini API raises exception."""
        from django.contrib.auth import get_user_model
        from core.utils.saju_concepts import GanJi
        from core.services.fortune import FortuneScore, ElementDistribution

        User = get_user_model()

        user = User.objects.create_user(
            email='imageexception@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )

        # Mock PIL Image
        mock_pil_image = Mock()
        mock_image_module.open.return_value = mock_pil_image

        # Mock Gemini API to raise exception
        mock_gemini_client = Mock()
        mock_gemini_client.models.generate_content.side_effect = Exception("Gemini API Error")
        self.service.gemini_client = mock_gemini_client

        user_saju = self.service.get_user_saju_info(user.id)
        tomorrow_date = self.test_date + timedelta(days=1)
        tomorrow_day_ganji = GanJi.find_by_name("갑자")

        fortune_response = FortuneAIResponse(
            today_fortune_summary="테스트 요약",
            today_element_balance_description="균형 설명",
            today_daily_guidance="일상 가이드"
        )

        fortune_score = FortuneScore(
            entropy_score=50.0,
            elements={
                "대운": None,
                "세운": {"two_letters": "갑자"},
                "월운": {"two_letters": "병인"},
                "일운": {"two_letters": "무신"},
                "년주": None,
                "월주": None,
                "일주": None,
                "시주": None,
            },
            element_distribution={
                "목": ElementDistribution(count=3, percentage=20.0),
                "화": ElementDistribution(count=3, percentage=20.0),
                "토": ElementDistribution(count=3, percentage=20.0),
                "금": ElementDistribution(count=3, percentage=20.0),
                "수": ElementDistribution(count=3, percentage=20.0)
            },
            interpretation="Test",
            needed_element="목"
        )

        result = self.service.generate_fortune_image_with_ai(
            fortune_response,
            user_saju,
            tomorrow_date,
            tomorrow_day_ganji,
            fortune_score
        )

        # Should return None on exception
        self.assertIsNone(result)

class TestFortuneServiceIntegration(TestCase):
    """Integration tests for FortuneService."""

    @patch('core.services.fortune.openai')
    @patch('core.services.fortune.GEMINI_AVAILABLE', True)
    @patch('core.services.fortune.genai')
    @patch('core.services.fortune.Image')
    def setUp(self, mock_image, mock_genai, mock_openai_model):
        """Set up test fixtures."""
        self.service = FortuneService()
        self.user_id = 1

    @patch('core.services.fortune.Image')
    @patch('core.services.fortune.genai')
    @patch('openai.OpenAI')
    def test_generate_fortune_with_image(self, mock_openai, mock_genai, mock_image_module):
        """Test generate_fortune includes image generation and saves it to DB."""
        from django.contrib.auth import get_user_model
        from core.models import FortuneResult
        from core.services.fortune import FortuneAIResponse

        User = get_user_model()

        # Create a user with complete saju data
        from datetime import date
        user = User.objects.create_user(
            email='fortuneimage@example.com',
            password='testpass123'
        )
        user.birth_date_solar = date(1990, 1, 1)
        user.birth_time_units = '23'
        user.yearly_ganji = '갑자'
        user.monthly_ganji = '병인'
        user.daily_ganji = '무신'
        user.hourly_ganji = '임오'
        user.save()

        # Mock AI text response
        mock_parsed = FortuneAIResponse(
            today_fortune_summary="오늘은 좋은 날! 수의 기운을 모아보세요.",
            today_element_balance_description="균형 설명입니다.",
            today_daily_guidance="일상 가이드입니다."
        )
        mock_text_response = Mock()
        mock_text_response.choices = [Mock()]
        mock_text_response.choices[0].message.parsed = mock_parsed

        # Mock Gemini image response
        mock_image_bytes = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\rIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82'

        # Mock PIL Image
        mock_pil_image = Mock()
        mock_image_module.open.return_value = mock_pil_image

        # Mock Gemini response structure
        mock_inline_data = Mock()
        mock_inline_data.data = mock_image_bytes
        mock_part = Mock()
        mock_part.inline_data = mock_inline_data
        mock_content = Mock()
        mock_content.parts = [mock_part]
        mock_candidate = Mock()
        mock_candidate.content = mock_content
        mock_gemini_response = Mock()
        mock_gemini_response.candidates = [mock_candidate]

        # Set up mock clients
        mock_client = Mock()
        mock_client.chat.completions.parse.return_value = mock_text_response
        self.service.client = mock_client

        mock_gemini_client = Mock()
        mock_gemini_client.models.generate_content.return_value = mock_gemini_response
        self.service.gemini_client = mock_gemini_client

        # Generate fortune
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        result = self.service.generate_fortune(user, test_date)

        # Verify result status
        self.assertEqual(result.status, 'success')
        self.assertIsNotNone(result.data)

        # Verify FortuneResult was saved to DB
        tomorrow_date = (test_date + timedelta(days=1)).date()
        fortune_result = FortuneResult.objects.get(
            user=user,
            for_date=tomorrow_date
        )

        # Verify fortune data (placeholder initially)
        self.assertIsNotNone(fortune_result.fortune_data)
        self.assertIsNotNone(fortune_result.fortune_score)
        self.assertIn('today_fortune_summary', fortune_result.fortune_data)

        # Verify status is processing (image generated in background)
        self.assertEqual(fortune_result.status, 'processing')

        # Image is not saved yet (generated in background)
        self.assertFalse(fortune_result.fortune_image)

        # Verify placeholder message
        self.assertIn('운세를 생성하고 있습니다', fortune_result.fortune_data['today_fortune_summary'])

    @patch('core.services.fortune.Image')
    @patch('core.services.fortune.genai')
    @patch('openai.OpenAI')
    def test_generate_fortune_without_image_on_failure(self, mock_openai, mock_genai, mock_image_module):
        """Test generate_fortune saves fortune even if image generation fails."""
        from django.contrib.auth import get_user_model
        from core.models import FortuneResult
        from core.services.fortune import FortuneAIResponse

        User = get_user_model()

        # Create a user with complete saju data
        from datetime import date
        user = User.objects.create_user(
            email='fortunenoimageonfail@example.com',
            password='testpass123'
        )
        user.birth_date_solar = date(1990, 1, 1)
        user.birth_time_units = '23'
        user.yearly_ganji = '갑자'
        user.monthly_ganji = '병인'
        user.daily_ganji = '무신'
        user.hourly_ganji = '임오'
        user.save()

        # Mock AI text response (success)
        mock_parsed = FortuneAIResponse(
            today_fortune_summary="오늘은 좋은 날! 수의 기운을 모아보세요.",
            today_element_balance_description="균형 설명입니다.",
            today_daily_guidance="일상 가이드입니다."
        )
        mock_text_response = Mock()
        mock_text_response.choices = [Mock()]
        mock_text_response.choices[0].message.parsed = mock_parsed

        # Mock PIL Image
        mock_pil_image = Mock()
        mock_image_module.open.return_value = mock_pil_image

        # Set up mock client with image generation failure
        mock_client = Mock()
        mock_client.chat.completions.parse.return_value = mock_text_response
        self.service.client = mock_client

        # Mock Gemini to raise exception
        mock_gemini_client = Mock()
        mock_gemini_client.models.generate_content.side_effect = Exception("Gemini API Error")
        self.service.gemini_client = mock_gemini_client

        # Generate fortune
        test_date = datetime(2024, 1, 1, 12, 0, 0)
        result = self.service.generate_fortune(user, test_date)

        # Verify result status is still success
        self.assertEqual(result.status, 'success')
        self.assertIsNotNone(result.data)

        # Verify FortuneResult was saved to DB
        tomorrow_date = (test_date + timedelta(days=1)).date()
        fortune_result = FortuneResult.objects.get(
            user=user,
            for_date=tomorrow_date
        )

        # Verify fortune data exists
        self.assertIsNotNone(fortune_result.fortune_data)
        self.assertIsNotNone(fortune_result.fortune_score)

        # Verify image is NOT saved (but fortune is still created)
        self.assertFalse(fortune_result.fortune_image)

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