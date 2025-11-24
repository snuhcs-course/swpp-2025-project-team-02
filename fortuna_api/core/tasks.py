"""
Background tasks for fortune generation using dedicated worker thread pool.
Uses ThreadPoolExecutor to avoid blocking the main application.
"""

import logging
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor

logger = logging.getLogger(__name__)

# Create dedicated thread pool for fortune generation tasks
# Max workers can be adjusted based on server resources
FORTUNE_WORKER_POOL = ThreadPoolExecutor(
    max_workers=5,  # Limit concurrent AI generations to avoid resource exhaustion
    thread_name_prefix='fortune_worker'
)


def update_fortune_sync(user_id: int, image_date_str: str) -> None:
    """
    Update fortune based on newly uploaded image.
    Runs in worker thread pool to avoid blocking main application.

    Args:
        user_id: ID of the user who uploaded the image
        image_date_str: Date string in YYYY-MM-DD format for the image date
    """
    try:
        from .models import ChakraImage, FortuneResult
        from core.views import fortune_service

        # Parse the image date
        image_date = datetime.strptime(image_date_str, '%Y-%m-%d')
        tomorrow = image_date + timedelta(days=1)

        logger.info(
            f"Starting fortune update for user {user_id}, "
            f"image_date={image_date_str}, for_date={tomorrow.date()}"
        )

        # Check if images exist
        images_count = ChakraImage.objects.filter(
            user_id=user_id,
            date=image_date.date()
        ).count()

        if images_count == 0:
            logger.warning(
                f"No images found for user {user_id} on {image_date_str}"
            )
            return

        # Get user object
        from user.models import User
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            logger.error(f"User {user_id} not found")
            return

        # Update FortuneResult status to processing
        try:
            fortune_result = FortuneResult.objects.get(
                user_id=user_id,
                for_date=tomorrow.date()
            )
            if hasattr(fortune_result, 'status'):
                fortune_result.status = 'processing'
                fortune_result.save(update_fields=['status'])
        except FortuneResult.DoesNotExist:
            logger.warning(
                f"FortuneResult not found for user {user_id}, "
                f"date {tomorrow.date()}"
            )
            return

        # Generate/update the fortune
        result = fortune_service.generate_fortune(
            user=user,
            date=image_date
        )

        # Update status based on result
        try:
            fortune_result = FortuneResult.objects.get(
                user_id=user_id,
                for_date=tomorrow.date()
            )

            if result.status == 'success':
                if hasattr(fortune_result, 'status'):
                    fortune_result.status = 'completed'
                    fortune_result.save(update_fields=['status'])

                logger.info(
                    f"Successfully updated fortune for user {user_id}, "
                    f"for_date={tomorrow.date()}, images_used={images_count}"
                )
            else:
                error_message = result.error.message if result.error else 'Unknown error'
                logger.error(
                    f"Failed to generate fortune: {error_message}"
                )
                # Mark as pending so it can be retried
                if hasattr(fortune_result, 'status'):
                    fortune_result.status = 'pending'
                    fortune_result.save(update_fields=['status'])

        except FortuneResult.DoesNotExist:
            logger.error(
                f"FortuneResult disappeared during update for user {user_id}"
            )

    except Exception as e:
        logger.error(
            f"Error in fortune update for user {user_id}: {e}",
            exc_info=True
        )


def schedule_fortune_update(user_id: int, image_date_str: str) -> None:
    """
    Schedule a fortune update task to run in worker thread pool.

    Args:
        user_id: ID of the user who uploaded the image
        image_date_str: Date string in YYYY-MM-DD format for the image date
    """
    FORTUNE_WORKER_POOL.submit(update_fortune_sync, user_id, image_date_str)
    logger.debug(
        f"Scheduled fortune update for user {user_id}, "
        f"image_date={image_date_str}"
    )


def generate_fortune_sync(user_id: int, date_str: str) -> None:
    """
    Generate fortune with AI in worker thread.
    Runs synchronously in dedicated worker thread pool.

    Args:
        user_id: ID of the user
        date_str: Date string in YYYY-MM-DD format for the fortune date
    """
    try:
        from .models import FortuneResult
        from user.models import User
        from django.core.files.base import ContentFile
        from core.views import fortune_service

        # Parse the date
        date = datetime.strptime(date_str, '%Y-%m-%d')

        logger.info(
            f"Starting fortune generation for user {user_id}, date={date_str}"
        )

        # Get user object
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            logger.error(f"User {user_id} not found")
            return

        # Get FortuneResult
        try:
            fortune_result = FortuneResult.objects.get(
                user_id=user_id,
                for_date=date.date()
            )
        except FortuneResult.DoesNotExist:
            logger.error(f"FortuneResult not found for user {user_id}, date {date.date()}")
            return

        # Skip if already completed
        if fortune_result.status == 'completed':
            logger.info(f"Fortune already completed for user {user_id}, date {date.date()}")
            return

        # Generate fortune with AI (all sync operations in worker thread)
        user_saju = fortune_service.get_user_saju_info(user_id)
        tomorrow_day_ganji = fortune_service.calculate_day_ganji(date)
        fortune_score = fortune_service.calculate_fortune_balance(user, date)

        compatibility = fortune_service.analyze_saju_compatibility(
            user_saju.daily,
            tomorrow_day_ganji
        )

        # Generate fortune with AI
        fortune = fortune_service.generate_fortune_with_ai(
            user_saju,
            date,
            tomorrow_day_ganji,
            compatibility,
            fortune_score
        )

        # Generate fortune image with AI
        image_bytes = fortune_service.generate_fortune_image_with_ai(
            fortune,
            user_saju,
            date,
            tomorrow_day_ganji,
            fortune_score
        )

        # Update with completed fortune
        fortune_result.fortune_data = fortune.model_dump()
        fortune_result.status = 'completed'
        fortune_result.save(update_fields=['fortune_data', 'status'])

        # Save image if generated successfully
        if image_bytes:
            image_filename = f"fortune_{user_id}_{date.strftime('%Y%m%d')}.png"
            fortune_result.fortune_image.save(
                image_filename,
                ContentFile(image_bytes),
                save=True
            )
            logger.info(f"Fortune image saved for user {user_id} on {date}")
        else:
            logger.warning(f"No image generated for user {user_id} on {date}")

        logger.info(f"Successfully generated fortune for user {user_id}, date={date_str}")

    except Exception as e:
        logger.error(
            f"Error in fortune generation for user {user_id}: {e}",
            exc_info=True
        )
        # Mark as pending so it can be retried
        try:
            from .models import FortuneResult
            fortune_result = FortuneResult.objects.get(
                user_id=user_id,
                for_date=datetime.strptime(date_str, '%Y-%m-%d').date()
            )
            fortune_result.status = 'pending'
            fortune_result.save(update_fields=['status'])
        except Exception:
            pass


def schedule_fortune_generation(user_id: int, date_str: str) -> None:
    """
    Schedule a fortune generation task to run in worker thread pool.

    Args:
        user_id: ID of the user
        date_str: Date string in YYYY-MM-DD format for the fortune date
    """
    FORTUNE_WORKER_POOL.submit(generate_fortune_sync, user_id, date_str)
    logger.info(
        f"Scheduled fortune generation for user {user_id}, date={date_str}"
    )
