"""
Unit tests for database persistence (ChakraImage and FortuneResult models).
"""

from datetime import datetime
from django.test import TestCase
from django.contrib.auth import get_user_model
from django.utils import timezone
from core.models import ChakraImage, FortuneResult
from core.services.fortune import FortuneService
from unittest.mock import patch

User = get_user_model()


class TestChakraImagePersistence(TestCase):
    """Test ChakraImage model persistence."""

    def setUp(self):
        """Set up test fixtures."""
        self.user = User.objects.create_user(
            email='test@example.com',
            password='testpass123'
        )

    def test_create_chakra_image(self):
        """Test creating a ChakraImage record."""
        now = timezone.now()
        chakra = ChakraImage.objects.create(
            user=self.user,
            image='test.jpg',
            chakra_type='fire',
            date=now.date(),
            timestamp=now,
            latitude=37.5665,
            longitude=126.9780,
            device_make='Apple',
            device_model='iPhone 14'
        )

        self.assertEqual(chakra.user, self.user)
        self.assertEqual(chakra.chakra_type, 'fire')
        self.assertEqual(chakra.latitude, 37.5665)
        self.assertEqual(chakra.longitude, 126.9780)

    def test_chakra_image_without_location(self):
        """Test creating ChakraImage without GPS data."""
        now = timezone.now()
        chakra = ChakraImage.objects.create(
            user=self.user,
            image='test.jpg',
            chakra_type='water',
            date=now.date(),
            timestamp=now
        )

        self.assertIsNone(chakra.latitude)
        self.assertIsNone(chakra.longitude)

    def test_query_images_by_date(self):
        """Test querying images by date."""
        date1 = timezone.make_aware(datetime(2024, 1, 1, 10, 0, 0))
        date2 = timezone.make_aware(datetime(2024, 1, 2, 10, 0, 0))

        ChakraImage.objects.create(
            user=self.user,
            image='test1.jpg',
            chakra_type='fire',
            date=date1.date(),
            timestamp=date1
        )
        ChakraImage.objects.create(
            user=self.user,
            image='test2.jpg',
            chakra_type='water',
            date=date2.date(),
            timestamp=date2
        )

        # Query by date
        images_date1 = ChakraImage.objects.filter(
            user=self.user,
            date=date1.date()
        )
        self.assertEqual(images_date1.count(), 1)
        self.assertEqual(images_date1.first().chakra_type, 'fire')


class TestFortuneResultPersistence(TestCase):
    """Test FortuneResult model persistence."""

    def setUp(self):
        """Set up test fixtures."""
        self.user = User.objects.create_user(
            email='fortune@example.com',
            password='testpass123'
        )

    def test_create_fortune_result(self):
        """Test creating a FortuneResult record."""
        fortune = FortuneResult.objects.create(
            user=self.user,
            for_date=datetime(2024, 1, 2).date(),
            gapja_code=12,
            gapja_name='을해',
            gapja_element='목',
            fortune_data={
                'overall_fortune': 85,
                'fortune_summary': 'Good day ahead!'
            }
        )

        self.assertEqual(fortune.user, self.user)
        self.assertEqual(fortune.gapja_code, 12)
        self.assertEqual(fortune.fortune_data['overall_fortune'], 85)

    def test_fortune_unique_constraint(self):
        """Test unique constraint on user and date."""
        date = datetime(2024, 1, 2).date()

        FortuneResult.objects.create(
            user=self.user,
            for_date=date,
            gapja_code=12,
            gapja_name='을해',
            gapja_element='목',
            fortune_data={}
        )

        # Try to create duplicate - should be prevented by unique_together
        from django.db import IntegrityError
        with self.assertRaises(IntegrityError):
            FortuneResult.objects.create(
                user=self.user,
                for_date=date,
                gapja_code=13,
                gapja_name='병자',
                gapja_element='화',
                fortune_data={}
            )

    def test_update_or_create_fortune(self):
        """Test update_or_create for fortune results."""
        date = datetime(2024, 1, 2).date()

        # Create first fortune
        fortune1, created1 = FortuneResult.objects.update_or_create(
            user=self.user,
            for_date=date,
            defaults={
                'gapja_code': 12,
                'gapja_name': '을해',
                'gapja_element': '목',
                'fortune_data': {'score': 80}
            }
        )
        self.assertTrue(created1)
        self.assertEqual(fortune1.fortune_data['score'], 80)

        # Update same fortune
        fortune2, created2 = FortuneResult.objects.update_or_create(
            user=self.user,
            for_date=date,
            defaults={
                'gapja_code': 12,
                'gapja_name': '을해',
                'gapja_element': '목',
                'fortune_data': {'score': 90}
            }
        )
        self.assertFalse(created2)
        self.assertEqual(fortune2.id, fortune1.id)
        self.assertEqual(fortune2.fortune_data['score'], 90)


class TestFortuneServicePersistence(TestCase):
    """Test FortuneService database persistence."""

    def setUp(self):
        """Set up test fixtures."""
        from datetime import date
        self.user = User.objects.create_user(
            email='service@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )
        # Set required birth info separately
        self.user.birth_date_solar = date(1990, 1, 1)
        self.user.birth_time_units = '자시'
        self.user.save()

        with patch('core.services.fortune.openai'):
            self.service = FortuneService()

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_fortune_generation_saves_to_db(self, mock_ai):
        """Test that fortune generation saves to database."""
        from core.services.fortune import FortuneAIResponse
        mock_ai.return_value = FortuneAIResponse(
            today_fortune_summary="오늘은 조화로운 날! 균형을 유지하며 차분히 시작해보세요.",
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 균형잡힌 좋은 날입니다.",
            today_daily_guidance="동쪽으로의 활동이 좋으며, 침착함을 유지하세요. 일에 집중하기 좋은 시간입니다."
        )

        result = self.service.generate_fortune(
            user=self.user,
            date=datetime(2024, 1, 1)
        )

        self.assertEqual(result.status, 'success')
        self.assertIsNotNone(result.data)

        # Verify database record exists for tomorrow
        tomorrow = datetime(2024, 1, 2).date()
        fortune = FortuneResult.objects.get(user=self.user, for_date=tomorrow)
        self.assertEqual(fortune.user_id, self.user.id)
        self.assertIsNotNone(fortune.fortune_data)

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_fortune_regeneration_returns_cached(self, mock_ai):
        """Test that regenerating fortune returns cached record (race condition protection)."""
        from core.services.fortune import FortuneAIResponse
        mock_ai.return_value = FortuneAIResponse(
            today_fortune_summary="오늘은 좋은 날! 첫 번째 운세로 하루를 시작해보세요.",
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 첫 번째 운세입니다.",
            today_daily_guidance="동쪽으로의 활동이 좋으며, 침착함을 유지하세요."
        )

        # Generate first fortune
        self.service.generate_fortune(
            user=self.user,
            date=datetime(2024, 1, 1)
        )
        tomorrow = datetime(2024, 1, 2).date()
        fortune1 = FortuneResult.objects.get(user=self.user, for_date=tomorrow)
        fortune_id1 = fortune1.id
        first_summary = fortune1.fortune_data['today_fortune_summary']

        # Generate again for same date - should return cached version (race condition protection)
        mock_ai.return_value = FortuneAIResponse(
            today_fortune_summary="오늘은 새로운 날! 업데이트된 운세로 다시 시작해보세요.",
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 업데이트된 운세입니다.",
            today_daily_guidance="남쪽으로의 활동이 좋으며, 긍정적인 마음을 유지하세요."
        )
        self.service.generate_fortune(
            user=self.user,
            date=datetime(2024, 1, 1)
        )
        fortune2 = FortuneResult.objects.get(user=self.user, for_date=tomorrow)
        fortune_id2 = fortune2.id

        # Should return same record WITHOUT regenerating (cached)
        self.assertEqual(fortune_id1, fortune_id2)
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 1)
        # Fortune data should NOT be updated (cached version returned)
        self.assertEqual(fortune2.fortune_data['today_fortune_summary'], first_summary)
        self.assertNotIn('업데이트된 운세', fortune2.fortune_data['today_fortune_summary'])

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_race_condition_protection_with_processing_status(self, mock_ai):
        """Test that concurrent requests return placeholder when fortune is being generated."""
        from core.models import FortuneResult

        tomorrow = datetime(2024, 1, 2).date()

        # Simulate first request creating record with 'processing' status
        FortuneResult.objects.create(
            user=self.user,
            for_date=tomorrow,
            status='processing',
            gapja_code=1,
            gapja_name='갑자',
            gapja_element='목',
            fortune_score={
                'entropy_score': 75.0,
                'elements': {},
                'element_distribution': {},
                'interpretation': 'Test',
                'needed_element': '수'
            },
            fortune_data={
                'today_fortune_summary': '운세를 생성하고 있습니다... 잠시만 기다려주세요!',
                'today_element_balance_description': 'AI가 당신의 사주와 오늘의 기운을 분석하고 있습니다.',
                'today_daily_guidance': '곧 맞춤형 조언을 제공해드리겠습니다.'
            }
        )

        # Second request should return placeholder without calling AI
        result = self.service.generate_fortune(
            user=self.user,
            date=datetime(2024, 1, 1)
        )

        # Verify placeholder is returned
        self.assertEqual(result.status, 'success')
        self.assertIn('운세를 생성하고 있습니다', result.data.fortune.today_fortune_summary)

        # Verify AI was NOT called (because status is 'processing')
        mock_ai.assert_not_called()

        # Verify only one record exists
        self.assertEqual(FortuneResult.objects.filter(user=self.user, for_date=tomorrow).count(), 1)

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_race_condition_protection_with_completed_status(self, mock_ai):
        """Test that completed fortune is returned from cache without regenerating."""
        from core.models import FortuneResult

        tomorrow = datetime(2024, 1, 2).date()

        # Create completed fortune result
        FortuneResult.objects.create(
            user=self.user,
            for_date=tomorrow,
            status='completed',
            gapja_code=1,
            gapja_name='갑자',
            gapja_element='목',
            fortune_score={
                'entropy_score': 85.0,
                'elements': {},
                'element_distribution': {},
                'interpretation': 'Test',
                'needed_element': '화'
            },
            fortune_data={
                'today_fortune_summary': '완성된 운세입니다!',
                'today_element_balance_description': '완성된 오행 분석입니다.',
                'today_daily_guidance': '완성된 일상 가이드입니다.'
            }
        )

        # Request fortune - should return cached version
        result = self.service.generate_fortune(
            user=self.user,
            date=datetime(2024, 1, 1)
        )

        # Verify cached data is returned
        self.assertEqual(result.status, 'success')
        self.assertEqual(result.data.fortune.today_fortune_summary, '완성된 운세입니다!')

        # Verify AI was NOT called (cached)
        mock_ai.assert_not_called()

        # Verify only one record exists
        self.assertEqual(FortuneResult.objects.filter(user=self.user, for_date=tomorrow).count(), 1)
