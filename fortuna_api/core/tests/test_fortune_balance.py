"""
Tests for fortune balance calculation (five elements entropy).
"""

from datetime import date, datetime
from django.test import TestCase
from core.services.fortune import FortuneService
from user.models import User


class TestFortuneBalance(TestCase):
    """Test five elements balance calculation."""

    def setUp(self):
        """Set up test user with complete saju data."""
        self.service = FortuneService()

        self.user = User.objects.create_user(
            email="balance_test@example.com",
            password="testpass123",
            gender="F",
        )

        # Set birth data and calculate saju
        self.user.set_birth_date_and_calculate_saju(
            birth_date=date(1995, 7, 15),
            calendar_type="solar",
            time_units="오시"  # 11:30-13:30
        )
        self.user.save()

    def test_calculate_fortune_balance_structure(self):
        """Test that balance calculation returns proper structure."""
        test_date = datetime(2024, 12, 25, 12, 0)

        balance = self.service.calculate_fortune_balance(self.user, test_date)

        # Check that balance is FortuneScore Pydantic object
        self.assertIsNotNone(balance)
        self.assertTrue(hasattr(balance, 'entropy_score'))
        self.assertTrue(hasattr(balance, 'elements'))
        self.assertTrue(hasattr(balance, 'element_distribution'))
        self.assertTrue(hasattr(balance, 'interpretation'))

        # Check entropy score range
        self.assertIsInstance(balance.entropy_score, float)
        self.assertGreaterEqual(balance.entropy_score, 0)
        self.assertLessEqual(balance.entropy_score, 100)

        # Check elements structure (should have 8 keys for 8 pillars)
        elements = balance.elements
        self.assertIn("대운", elements)
        self.assertIn("세운", elements)
        self.assertIn("월운", elements)
        self.assertIn("일운", elements)
        self.assertIn("년주", elements)
        self.assertIn("월주", elements)
        self.assertIn("일주", elements)
        self.assertIn("시주", elements)

    def test_calculate_fortune_balance_element_distribution(self):
        """Test that element distribution sums to total elements."""
        test_date = datetime(2024, 12, 25, 12, 0)

        balance = self.service.calculate_fortune_balance(self.user, test_date)

        # Get element distribution
        distribution = balance.element_distribution

        # Should have all 5 elements
        self.assertEqual(len(distribution), 5)

        # Check that counts exist
        total_count = sum(elem.count for elem in distribution.values())
        self.assertGreater(total_count, 0)

        # Check percentages sum to ~100
        total_percentage = sum(elem.percentage for elem in distribution.values())
        self.assertAlmostEqual(total_percentage, 100.0, places=0)

    def test_calculate_fortune_balance_with_daewoon(self):
        """Test balance calculation when user has daewoon."""
        # Use older birthdate to ensure daewoon exists
        older_user = User.objects.create_user(
            email="older_test@example.com",
            password="testpass123",
            gender="M",
        )

        older_user.set_birth_date_and_calculate_saju(
            birth_date=date(1980, 3, 10),
            calendar_type="solar",
            time_units="진시"
        )
        older_user.save()

        test_date = datetime(2024, 12, 25, 12, 0)
        balance = self.service.calculate_fortune_balance(older_user, test_date)

        # Should have elements field with all 8 pillars
        elements = balance.elements

        # Check that daewoon exists (not None) for older user
        self.assertIsNotNone(elements["대운"])

        # Count total elements from distribution
        distribution = balance.element_distribution
        total_count = sum(elem.count for elem in distribution.values())

        # Should have 16 elements (8 pillars * 2) when daewoon exists
        self.assertEqual(total_count, 16)

    def test_five_element_entropy_score_perfect_balance(self):
        """Test entropy score with perfectly balanced distribution."""
        # Equal distribution: [3, 3, 3, 3, 4] (as balanced as possible for 16 items)
        counts = [3, 3, 3, 3, 4]
        score = self.service._five_element_entropy_score(counts)

        # Should be very high (close to 100)
        self.assertGreater(score, 95)

    def test_five_element_entropy_score_unbalanced(self):
        """Test entropy score with very unbalanced distribution."""
        # Very unbalanced: all in one element
        counts = [16, 0, 0, 0, 0]
        score = self.service._five_element_entropy_score(counts)

        # Should be 0 (no entropy)
        self.assertEqual(score, 0.0)

    def test_five_element_entropy_score_moderately_unbalanced(self):
        """Test entropy score with moderately unbalanced distribution."""
        # Moderately unbalanced
        counts = [8, 4, 2, 1, 1]
        score = self.service._five_element_entropy_score(counts)

        # Should be moderate (between 40-85)
        self.assertGreater(score, 40)
        self.assertLess(score, 85)

    def test_interpret_balance_score(self):
        """Test interpretation messages for different scores."""
        # Very high score
        msg_high = self.service._interpret_balance_score(95)
        self.assertIn("매우 균형", msg_high)

        # Medium score
        msg_medium = self.service._interpret_balance_score(65)
        self.assertIn("균형", msg_medium)

        # Low score
        msg_low = self.service._interpret_balance_score(30)
        self.assertIn("편중", msg_low)

    def test_generate_fortune_includes_balance(self):
        """Test that generate_fortune includes fortune_score."""
        test_date = datetime(2024, 12, 25, 12, 0)

        result = self.service.generate_fortune(self.user, test_date)

        # Check structure (Response[FortuneResponse] object)
        self.assertEqual(result.status, "success")
        self.assertIsNotNone(result.data)
        self.assertIsNotNone(result.data.fortune_score)

        # Check fortune_score structure (FortuneScore Pydantic object)
        fortune_score = result.data.fortune_score
        self.assertTrue(hasattr(fortune_score, 'entropy_score'))
        self.assertTrue(hasattr(fortune_score, 'element_distribution'))
