"""
Async tasks for incremental fortune updates.
Uses asyncio coroutines for efficient non-blocking task execution.
Can be migrated to Celery for production use.
"""

import asyncio
import logging
from datetime import datetime, timedelta
from typing import Optional
from asgiref.sync import sync_to_async
from core.views import fortune_service
from django.db import transaction

logger = logging.getLogger(__name__)


async def update_fortune_async(user_id: int, image_date_str: str) -> None:
    """
    Fire-and-forget async task to update fortune based on newly uploaded image.

    This function triggers an incremental update of the fortune for the day
    after the image was taken. It runs as a coroutine to avoid blocking
    the image upload response while being more efficient than threading.

    Args:
        user_id: ID of the user who uploaded the image
        image_date_str: Date string in YYYY-MM-DD format for the image date

    Note:
        This uses asyncio coroutines for efficiency. For production, consider using
        Celery or Django Q for more robust task management with persistence.
    """
    try:
        from .models import ChakraImage, FortuneResult

        # Parse the image date
        image_date = datetime.strptime(image_date_str, '%Y-%m-%d')
        tomorrow = image_date + timedelta(days=1)

        logger.info(
            f"Starting async fortune update for user {user_id}, "
            f"image_date={image_date_str}, for_date={tomorrow.date()}"
        )

        # Convert Django ORM operations to async
        images_count = await sync_to_async(
            ChakraImage.objects.filter(
                user_id=user_id,
                date=image_date.date()
            ).count
        )()

        if images_count == 0:
            logger.warning(
                f"No images found for user {user_id} on {image_date_str}"
            )
            return

        # Update FortuneResult status to processing
        try:
            fortune_result = await sync_to_async(FortuneResult.objects.get)(
                user_id=user_id,
                for_date=tomorrow.date()
            )
            if hasattr(fortune_result, 'status'):
                fortune_result.status = 'processing'
                await sync_to_async(fortune_result.save)(update_fields=['status'])
        except FortuneResult.DoesNotExist:
            logger.warning(
                f"FortuneResult not found for user {user_id}, "
                f"date {tomorrow.date()}"
            )
            return

        # Generate/update the fortune (this might involve AI calls, so keep it sync for now)
        result = await sync_to_async(fortune_service.generate_tomorrow_fortune)(
            user_id=user_id,
            date=image_date,
            include_photos=True
        )

        # Update status based on result
        try:
            fortune_result = await sync_to_async(FortuneResult.objects.get)(
                user_id=user_id,
                for_date=tomorrow.date()
            )
            
            if result['status'] == 'success':
                if hasattr(fortune_result, 'status'):
                    fortune_result.status = 'completed' # FIXME : 하루에 여러 번 올리는 걸 쪼개 받는 식으로 나눠 구현해야 함.
                    await sync_to_async(fortune_result.save)(update_fields=['status'])

                logger.info(
                    f"Successfully updated fortune for user {user_id}, "
                    f"for_date={tomorrow.date()}, images_used={images_count}"
                )
            else:
                logger.error(
                    f"Failed to generate fortune: {result.get('message', 'Unknown error')}"
                )
                # Mark as pending so it can be retried
                if hasattr(fortune_result, 'status'):
                    fortune_result.status = 'pending'
                    await sync_to_async(fortune_result.save)(update_fields=['status'])
                    
        except FortuneResult.DoesNotExist:
            logger.error(
                f"FortuneResult disappeared during update for user {user_id}"
            )

    except Exception as e:
        logger.error(
            f"Error in async fortune update for user {user_id}: {e}",
            exc_info=True
        )


def schedule_fortune_update(user_id: int, image_date_str: str) -> None:
    """
    Schedule a fortune update task to run in the background.
    
    This function creates and schedules the coroutine without blocking
    the calling thread. It's designed to be called from sync contexts.
    
    Args:
        user_id: ID of the user who uploaded the image
        image_date_str: Date string in YYYY-MM-DD format for the image date
    """
    try:
        # Get the current event loop or create a new one
        loop = asyncio.get_event_loop()
        if loop.is_running():
            # If we're already in an async context, schedule the task
            asyncio.create_task(update_fortune_async(user_id, image_date_str))
        else:
            # If we're in a sync context, run the task in the background
            asyncio.run_coroutine_threadsafe(
                update_fortune_async(user_id, image_date_str), 
                loop
            )
    except RuntimeError:
        # No event loop exists, create a new thread with its own loop
        import threading
        
        def run_in_thread():
            asyncio.run(update_fortune_async(user_id, image_date_str))
        
        thread = threading.Thread(target=run_in_thread, daemon=True)
        thread.start()
    
    logger.debug(
        f"Scheduled async fortune update for user {user_id}, "
        f"image_date={image_date_str}"
    )
