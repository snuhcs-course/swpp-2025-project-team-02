"""
Unit tests for database persistence (ChakraImage and FortuneResult models).
"""

from datetime import datetime, timedelta
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
        self.user = User.objects.create_user(
            email='service@example.com',
            password='testpass123',
            yearly_ganji='갑자',
            monthly_ganji='병인',
            daily_ganji='무신',
            hourly_ganji='임오'
        )
        with patch('core.services.fortune.openai'):
            self.service = FortuneService()

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_fortune_generation_saves_to_db(self, mock_ai):
        """Test that fortune generation saves to database."""
        from core.services.fortune import FortuneAIResponse
        mock_ai.return_value = FortuneAIResponse(
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 균형잡힌 좋은 날입니다.",
            today_daily_guidance="동쪽으로의 활동이 좋으며, 침착함을 유지하세요. 일에 집중하기 좋은 시간입니다."
        )

        result = self.service.generate_tomorrow_fortune(
            user_id=self.user.id,
            date=datetime(2024, 1, 1),
            include_photos=False
        )

        self.assertEqual(result['status'], 'success')
        self.assertIn('fortune_id', result['data'])

        # Verify database record
        fortune = FortuneResult.objects.get(id=result['data']['fortune_id'])
        self.assertEqual(fortune.user_id, self.user.id)
        self.assertEqual(fortune.gapja_name, fortune.gapja_name)
        self.assertIsNotNone(fortune.fortune_data)

    @patch.object(FortuneService, 'generate_fortune_with_ai')
    def test_fortune_regeneration_updates_existing(self, mock_ai):
        """Test that regenerating fortune updates existing record."""
        from core.services.fortune import FortuneAIResponse
        mock_ai.return_value = FortuneAIResponse(
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 첫 번째 운세입니다.",
            today_daily_guidance="동쪽으로의 활동이 좋으며, 침착함을 유지하세요."
        )

        # Generate first fortune
        result1 = self.service.generate_tomorrow_fortune(
            user_id=self.user.id,
            date=datetime(2024, 1, 1),
            include_photos=False
        )
        fortune_id1 = result1['data']['fortune_id']

        # Generate again for same date
        mock_ai.return_value = FortuneAIResponse(
            today_element_balance_description="당신의 오행과 오늘의 기운이 조화를 이룹니다. 업데이트된 운세입니다.",
            today_daily_guidance="남쪽으로의 활동이 좋으며, 긍정적인 마음을 유지하세요."
        )
        result2 = self.service.generate_tomorrow_fortune(
            user_id=self.user.id,
            date=datetime(2024, 1, 1),
            include_photos=False
        )
        fortune_id2 = result2['data']['fortune_id']

        # Should update same record
        self.assertEqual(fortune_id1, fortune_id2)
        self.assertEqual(FortuneResult.objects.filter(user=self.user).count(), 1)
