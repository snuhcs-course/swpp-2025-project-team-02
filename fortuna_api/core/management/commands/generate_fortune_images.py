"""
Management command to generate fortune images for existing FortuneResult records.

Usage:
    python manage.py generate_fortune_images [--workers N] [--dry-run] [--date YYYY-MM-DD] [--user-id ID]

This command reads existing FortuneResult records (with fortune_data already generated)
and generates only the AI images, saving them back to the fortune_image field.
"""

from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed
from django.core.management.base import BaseCommand
from django.core.files.base import ContentFile
from loguru import logger
from user.models import User
from core.models import FortuneResult
from core.services.fortune import FortuneService, FortuneAIResponse, FortuneScore
from core.services.image import ImageService
from core.utils.saju_concepts import Saju, GanJi


class Command(BaseCommand):
    help = 'Generate fortune images for existing FortuneResult records'

    def add_arguments(self, parser):
        parser.add_argument(
            '--workers',
            type=int,
            default=3,
            help='Number of parallel workers (default: 3)'
        )
        parser.add_argument(
            '--dry-run',
            action='store_true',
            help='Run without actually generating images'
        )
        parser.add_argument(
            '--user-id',
            type=int,
            help='Generate image for specific user only (for testing)'
        )
        parser.add_argument(
            '--date',
            type=str,
            help='Target date (YYYY-MM-DD). Generate images for this specific date only.'
        )
        parser.add_argument(
            '--skip-existing',
            action='store_true',
            help='Skip records that already have fortune_image'
        )

    def handle(self, *args, **options):
        workers = options['workers']
        dry_run = options['dry_run']
        specific_user_id = options.get('user_id')
        date_str = options.get('date')
        skip_existing = options.get('skip_existing', False)

        # Parse target date if provided
        target_date = None
        if date_str:
            try:
                target_date = datetime.strptime(date_str, '%Y-%m-%d').date()
            except ValueError:
                self.stdout.write(self.style.ERROR(
                    f'Invalid date format: {date_str}. Use YYYY-MM-DD.'
                ))
                return

        self.stdout.write(self.style.SUCCESS(
            f'\n{"="*60}\n'
            f'Starting fortune image generation\n'
            f'Time: {datetime.now().isoformat()}\n'
            f'Target date: {target_date.isoformat() if target_date else "All dates"}\n'
            f'Workers: {workers}\n'
            f'Dry run: {dry_run}\n'
            f'Skip existing: {skip_existing}\n'
            f'{"="*60}\n'
        ))

        # Query FortuneResult records
        queryset = FortuneResult.objects.filter()

        if specific_user_id:
            queryset = queryset.filter(user_id=specific_user_id)
            self.stdout.write(self.style.WARNING(
                f'Generating images for specific user: {specific_user_id}'
            ))

        if target_date:
            queryset = queryset.filter(for_date=target_date)
            self.stdout.write(self.style.WARNING(
                f'Generating images for specific date: {target_date}'
            ))

        if skip_existing:
            queryset = queryset.filter(fortune_image='')
            self.stdout.write(self.style.WARNING(
                'Skipping records with existing images'
            ))

        total_records = queryset.count()

        if total_records == 0:
            self.stdout.write(self.style.WARNING('No FortuneResult records found'))
            return

        self.stdout.write(f'Found {total_records} records to process\n')

        if dry_run:
            self.stdout.write(self.style.WARNING(
                'DRY RUN MODE - No images will be generated\n'
            ))
            for record in queryset[:10]:  # Show first 10
                self.stdout.write(
                    f'  - Would generate image for: User {record.user_id}, '
                    f'Date {record.for_date}'
                )
            if total_records > 10:
                self.stdout.write(f'  ... and {total_records - 10} more records')
            return

        # Process records in parallel
        success_count = 0
        error_count = 0
        skipped_count = 0

        with ThreadPoolExecutor(max_workers=workers) as executor:
            # Submit all tasks
            future_to_record = {
                executor.submit(self._generate_image_for_record, record): record
                for record in queryset
            }

            # Process completed tasks
            for future in as_completed(future_to_record):
                record = future_to_record[future]
                try:
                    result = future.result()
                    if result['status'] == 'success':
                        success_count += 1
                        self.stdout.write(
                            self.style.SUCCESS(
                                f'✓ [{success_count + error_count + skipped_count}/{total_records}] '
                                f'User {record.user_id}, Date {record.for_date}'
                            )
                        )
                    elif result['status'] == 'skipped':
                        skipped_count += 1
                        self.stdout.write(
                            self.style.WARNING(
                                f'⊘ [{success_count + error_count + skipped_count}/{total_records}] '
                                f'User {record.user_id} - {result["message"]}'
                            )
                        )
                    else:
                        error_count += 1
                        self.stdout.write(
                            self.style.ERROR(
                                f'✗ [{success_count + error_count + skipped_count}/{total_records}] '
                                f'User {record.user_id} - {result["message"]}'
                            )
                        )
                except Exception as e:
                    error_count += 1
                    self.stdout.write(
                        self.style.ERROR(
                            f'✗ [{success_count + error_count + skipped_count}/{total_records}] '
                            f'User {record.user_id} - Exception: {str(e)}'
                        )
                    )
                    logger.error(f'Failed to generate image for record {record.id}: {e}')

        # Summary
        self.stdout.write(
            self.style.SUCCESS(
                f'\n{"="*60}\n'
                f'Image generation completed\n'
                f'{"="*60}\n'
                f'Total records: {total_records}\n'
                f'Success: {success_count}\n'
                f'Skipped: {skipped_count}\n'
                f'Errors: {error_count}\n'
                f'{"="*60}\n'
            )
        )

        if error_count > 0:
            self.stdout.write(
                self.style.WARNING(
                    f'\n⚠ {error_count} records had errors. Check logs for details.\n'
                )
            )

    def _generate_image_for_record(self, record: FortuneResult) -> dict:
        """
        Generate fortune image for a single FortuneResult record.

        Args:
            record: FortuneResult object with existing fortune_data

        Returns:
            dict with 'status' ('success', 'error', 'skipped') and optional 'message'
        """
        try:
            # Check if record has required data
            if not record.fortune_data or not record.fortune_score:
                return {
                    'status': 'skipped',
                    'message': 'Missing fortune_data or fortune_score'
                }

            # Get user
            user = User.objects.get(id=record.user_id)

            # Check if user has required saju data
            if not user.birth_date_solar:
                return {
                    'status': 'skipped',
                    'message': 'No birth date'
                }

            # Initialize services
            image_service = ImageService()
            fortune_service = FortuneService(image_service)

            # Parse existing fortune data
            fortune_response = FortuneAIResponse(**record.fortune_data)
            fortune_score = FortuneScore(**record.fortune_score)

            # Get user's saju
            user_saju = user.saju()

            # Calculate day ganji for the date
            target_datetime = datetime.combine(record.for_date, datetime.min.time())
            tomorrow_day_ganji = fortune_service.calculate_day_ganji(target_datetime)

            # Generate image using AI
            image_bytes = fortune_service.generate_fortune_image_with_ai(
                fortune_response=fortune_response,
                user_saju=user_saju,
                tomorrow_date=target_datetime,
                tomorrow_day_ganji=tomorrow_day_ganji,
                fortune_score=fortune_score
            )

            if image_bytes:
                # Save image to model
                image_filename = f'fortune_{user.id}_{record.for_date.isoformat()}.png'
                record.fortune_image.save(
                    image_filename,
                    ContentFile(image_bytes),
                    save=True
                )
                logger.info(f'Saved fortune image for user {user.id}, date {record.for_date}')
                return {'status': 'success'}
            else:
                return {
                    'status': 'error',
                    'message': 'Image generation returned None'
                }

        except User.DoesNotExist:
            return {
                'status': 'error',
                'message': f'User {record.user_id} not found'
            }
        except Exception as e:
            logger.error(f'Error generating image for record {record.id}: {e}', exc_info=True)
            return {
                'status': 'error',
                'message': str(e)
            }
