"""
Async tasks for incremental fortune updates.
Uses threading for fire-and-forget task execution.
Can be migrated to Celery for production use.
"""

import threading
import logging
from datetime import datetime, timedelta
from typing import Optional

logger = logging.getLogger(__name__)


def update_fortune_async(user_id: int, image_date_str: str) -> None:
    """
    Fire-and-forget async task to update fortune based on newly uploaded image.

    This function triggers an incremental update of the fortune for the day
    after the image was taken. It runs in a separate thread to avoid blocking
    the image upload response.

    Args:
        user_id: ID of the user who uploaded the image
        image_date_str: Date string in YYYY-MM-DD format for the image date

    Note:
        This uses threading for simplicity. For production, consider using
        Celery or Django Q for more robust task management.
    """
    def _update_task():
        """Internal function that performs the actual fortune update."""
        try:
            from .models import ChakraImage, FortuneResult
            from .services.fortune import FortuneService

            # Parse the image date
            image_date = datetime.strptime(image_date_str, '%Y-%m-%d')
            tomorrow = image_date + timedelta(days=1)

            logger.info(
                f"Starting async fortune update for user {user_id}, "
                f"image_date={image_date_str}, for_date={tomorrow.date()}"
            )

            # Check if there are any images for this date
            images_count = ChakraImage.objects.filter(
                user_id=user_id,
                date=image_date.date()
            ).count()

            if images_count == 0:
                logger.warning(
                    f"No images found for user {user_id} on {image_date_str}"
                )
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
            fortune_service = FortuneService()
            result = fortune_service.generate_tomorrow_fortune(
                user_id=user_id,
                date=image_date,
                include_photos=True
            )

            # Update status to completed
            if result['status'] == 'success':
                try:
                    fortune_result = FortuneResult.objects.get(
                        user_id=user_id,
                        for_date=tomorrow.date()
                    )
                    if hasattr(fortune_result, 'status'):
                        fortune_result.status = 'completed'
                        fortune_result.save(update_fields=['status'])

                    logger.info(
                        f"Successfully updated fortune for user {user_id}, "
                        f"for_date={tomorrow.date()}, images_used={images_count}"
                    )
                except FortuneResult.DoesNotExist:
                    logger.error(
                        f"FortuneResult disappeared during update for user {user_id}"
                    )
            else:
                logger.error(
                    f"Failed to generate fortune: {result.get('message', 'Unknown error')}"
                )
                # Mark as pending so it can be retried
                try:
                    fortune_result = FortuneResult.objects.get(
                        user_id=user_id,
                        for_date=tomorrow.date()
                    )
                    if hasattr(fortune_result, 'status'):
                        fortune_result.status = 'pending'
                        fortune_result.save(update_fields=['status'])
                except FortuneResult.DoesNotExist:
                    pass

        except Exception as e:
            logger.error(
                f"Error in async fortune update for user {user_id}: {e}",
                exc_info=True
            )

    # Start the task in a daemon thread (fire-and-forget)
    thread = threading.Thread(target=_update_task, daemon=True)
    thread.start()

    logger.debug(
        f"Triggered async fortune update for user {user_id}, "
        f"image_date={image_date_str}"
    )
