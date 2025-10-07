"""
Async utilities for Django ORM operations.
Provides safe wrappers for common database operations in async contexts.
"""

import asyncio
import logging
from typing import Any, Callable, Optional, TypeVar, Union
from asgiref.sync import sync_to_async
from django.db import transaction
from django.db.models import Model, QuerySet

logger = logging.getLogger(__name__)

T = TypeVar('T')


async def async_get_or_create(
    model_class: type[Model], 
    defaults: Optional[dict] = None, 
    **kwargs
) -> tuple[Model, bool]:
    """
    Async version of get_or_create.
    
    Args:
        model_class: The Django model class
        defaults: Default values for creation
        **kwargs: Lookup parameters
        
    Returns:
        Tuple of (instance, created)
    """
    return await sync_to_async(model_class.objects.get_or_create)(
        defaults=defaults, **kwargs
    )


async def async_get(model_class: type[Model], **kwargs) -> Model:
    """
    Async version of get().
    
    Args:
        model_class: The Django model class
        **kwargs: Lookup parameters
        
    Returns:
        Model instance
        
    Raises:
        DoesNotExist: If no object matches the query
        MultipleObjectsReturned: If multiple objects match
    """
    return await sync_to_async(model_class.objects.get)(**kwargs)


async def async_filter_count(model_class: type[Model], **kwargs) -> int:
    """
    Async version of filter().count().
    
    Args:
        model_class: The Django model class
        **kwargs: Filter parameters
        
    Returns:
        Count of matching objects
    """
    return await sync_to_async(
        model_class.objects.filter(**kwargs).count
    )()


async def async_save(instance: Model, **kwargs) -> None:
    """
    Async version of save().
    
    Args:
        instance: Model instance to save
        **kwargs: Additional save parameters
    """
    await sync_to_async(instance.save)(**kwargs)


async def async_delete(instance: Model) -> tuple[int, dict[str, int]]:
    """
    Async version of delete().
    
    Args:
        instance: Model instance to delete
        
    Returns:
        Tuple of (number_deleted, deletion_details)
    """
    return await sync_to_async(instance.delete)()


async def async_bulk_create(
    model_class: type[Model], 
    objs: list[Model], 
    **kwargs
) -> list[Model]:
    """
    Async version of bulk_create().
    
    Args:
        model_class: The Django model class
        objs: List of model instances to create
        **kwargs: Additional bulk_create parameters
        
    Returns:
        List of created instances
    """
    return await sync_to_async(model_class.objects.bulk_create)(objs, **kwargs)


@sync_to_async
def run_in_transaction(func: Callable[[], T]) -> T:
    """
    Run a function within a database transaction asynchronously.
    
    Args:
        func: Function to run in transaction
        
    Returns:
        Result of the function
    """
    with transaction.atomic():
        return func()


class AsyncModelManager:
    """
    Async wrapper for Django model operations.
    Provides a cleaner interface for async database operations.
    """
    
    def __init__(self, model_class: type[Model]):
        self.model_class = model_class
    
    async def get(self, **kwargs) -> Model:
        """Get a single object."""
        return await async_get(self.model_class, **kwargs)
    
    async def get_or_create(
        self, 
        defaults: Optional[dict] = None, 
        **kwargs
    ) -> tuple[Model, bool]:
        """Get or create an object."""
        return await async_get_or_create(self.model_class, defaults, **kwargs)
    
    async def filter_count(self, **kwargs) -> int:
        """Count filtered objects."""
        return await async_filter_count(self.model_class, **kwargs)
    
    async def create(self, **kwargs) -> Model:
        """Create a new object."""
        instance = self.model_class(**kwargs)
        await async_save(instance)
        return instance
    
    async def bulk_create(self, objs: list[Model], **kwargs) -> list[Model]:
        """Bulk create objects."""
        return await async_bulk_create(self.model_class, objs, **kwargs)


def get_async_manager(model_class: type[Model]) -> AsyncModelManager:
    """
    Get an async manager for a model class.
    
    Args:
        model_class: The Django model class
        
    Returns:
        AsyncModelManager instance
    """
    return AsyncModelManager(model_class)


async def safe_async_call(
    func: Callable, 
    *args, 
    timeout: Optional[float] = None,
    **kwargs
) -> Any:
    """
    Safely call an async function with timeout and error handling.
    
    Args:
        func: Async function to call
        *args: Positional arguments
        timeout: Timeout in seconds
        **kwargs: Keyword arguments
        
    Returns:
        Function result or None if error/timeout
    """
    try:
        if timeout:
            return await asyncio.wait_for(func(*args, **kwargs), timeout=timeout)
        else:
            return await func(*args, **kwargs)
    except asyncio.TimeoutError:
        logger.warning(f"Async call to {func.__name__} timed out after {timeout}s")
        return None
    except Exception as e:
        logger.error(f"Error in async call to {func.__name__}: {e}", exc_info=True)
        return None
