from django.db import models
from django.conf import settings


class ChakraImage(models.Model):
    """Stores uploaded chakra images with metadata."""
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    image = models.ImageField(upload_to='chakras/%Y/%m/%d/')
    chakra_type = models.CharField(max_length=50, default='default')
    date = models.DateField(db_index=True)

    # EXIF metadata
    timestamp = models.DateTimeField()
    latitude = models.FloatField(null=True, blank=True)
    longitude = models.FloatField(null=True, blank=True)
    device_make = models.CharField(max_length=100, null=True, blank=True)
    device_model = models.CharField(max_length=100, null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-timestamp']
        indexes = [
            models.Index(fields=['user', 'date']),
            models.Index(fields=['user', 'timestamp']),
        ]


class FortuneResult(models.Model):
    """Stores generated fortune results."""
    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('processing', 'Processing'),
        ('completed', 'Completed'),
    ]

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    for_date = models.DateField()

    # Gapja info
    gapja_code = models.IntegerField(default=0)
    gapja_name = models.CharField(max_length=10, default='')
    gapja_element = models.CharField(max_length=5, default='')

    # Fortune data (JSON)
    fortune_data = models.JSONField(default=dict)

    # Fortune score with entropy-based balance (JSON)
    fortune_score = models.JSONField(default=dict, null=True, blank=True)

    # Status tracking for incremental updates
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='pending',
        help_text='Current status of fortune generation'
    )

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ['user', 'for_date']
        ordering = ['-for_date']
        indexes = [
            models.Index(fields=['user', 'for_date']),
        ]
