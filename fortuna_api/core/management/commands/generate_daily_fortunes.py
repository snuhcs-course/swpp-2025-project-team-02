"""
Management command to pre-generate fortunes for all active users.

Usage:
    python manage.py generate_daily_fortunes [--workers N] [--dry-run] [--date YYYY-MM-DD]

This command should be run daily (e.g., at 10 PM) via external cron job.
Receives a base date and generates fortunes for the NEXT day.
"""

from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed
from django.core.management.base import BaseCommand
from loguru import logger
from user.models import User
from core.services.fortune import FortuneService
from core.services.image import ImageService


class Command(BaseCommand):
    help = 'Pre-generate fortunes for all active users for tomorrow'

    def add_arguments(self, parser):
        parser.add_argument(
            '--workers',
            type=int,
            default=5,
            help='Number of parallel workers (default: 5)'
        )
        parser.add_argument(
            '--dry-run',
            action='store_true',
            help='Run without actually generating fortunes'
        )
        parser.add_argument(
            '--user-id',
            type=int,
            help='Generate fortune for specific user only (for testing)'
        )
        parser.add_argument(
            '--date',
            type=str,
            help='Base date (YYYY-MM-DD). Will generate fortune for the NEXT day. Defaults to today.'
        )

    def handle(self, *args, **options):
        workers = options['workers']
        dry_run = options['dry_run']
        specific_user_id = options.get('user_id')
        date_str = options.get('date')

        # Parse base date (default: today)
        if date_str:
            try:
                base_date = datetime.strptime(date_str, '%Y-%m-%d').date()
            except ValueError:
                self.stdout.write(self.style.ERROR(
                    f'Invalid date format: {date_str}. Use YYYY-MM-DD.'
                ))
                return
        else:
            base_date = datetime.now().date()

        # Calculate target date (tomorrow from base_date)
        target_date = base_date + timedelta(days=1)

        self.stdout.write(self.style.SUCCESS(
            f'\n{"="*60}\n'
            f'Starting daily fortune generation\n'
            f'Time: {datetime.now().isoformat()}\n'
            f'Base date: {base_date.isoformat()}\n'
            f'Target date (generating for): {target_date.isoformat()}\n'
            f'Workers: {workers}\n'
            f'Dry run: {dry_run}\n'
            f'{"="*60}\n'
        ))

        # Get all active users
        if specific_user_id:
            users = User.objects.filter(id=specific_user_id, is_active=True)
            self.stdout.write(self.style.WARNING(
                f'Generating fortune for specific user: {specific_user_id}'
            ))
        else:
            users = User.objects.filter(is_active=True).exclude(
                birth_date_solar__isnull=True
            )

        total_users = users.count()

        if total_users == 0:
            self.stdout.write(self.style.WARNING('No active users found'))
            return

        self.stdout.write(f'Found {total_users} active users to process\n')

        if dry_run:
            self.stdout.write(self.style.WARNING(
                'DRY RUN MODE - No fortunes will be generated\n'
            ))
            for user in users[:10]:  # Show first 10
                self.stdout.write(f'  - Would generate for: {user.email} (ID: {user.id})')
            if total_users > 10:
                self.stdout.write(f'  ... and {total_users - 10} more users')
            return

        # Process users in parallel
        success_count = 0
        error_count = 0
        skipped_count = 0

        with ThreadPoolExecutor(max_workers=workers) as executor:
            # Submit all tasks
            future_to_user = {
                executor.submit(self._generate_fortune_for_user, user, base_date): user
                for user in users
            }

            # Process completed tasks
            for future in as_completed(future_to_user):
                user = future_to_user[future]
                try:
                    result = future.result()
                    if result['status'] == 'success':
                        success_count += 1
                        self.stdout.write(
                            self.style.SUCCESS(
                                f'✓ [{success_count + error_count + skipped_count}/{total_users}] '
                                f'{user.email} (ID: {user.id})'
                            )
                        )
                    elif result['status'] == 'skipped':
                        skipped_count += 1
                        self.stdout.write(
                            self.style.WARNING(
                                f'⊘ [{success_count + error_count + skipped_count}/{total_users}] '
                                f'{user.email} - {result["message"]}'
                            )
                        )
                    else:
                        error_count += 1
                        self.stdout.write(
                            self.style.ERROR(
                                f'✗ [{success_count + error_count + skipped_count}/{total_users}] '
                                f'{user.email} - {result["message"]}'
                            )
                        )
                except Exception as e:
                    error_count += 1
                    self.stdout.write(
                        self.style.ERROR(
                            f'✗ [{success_count + error_count + skipped_count}/{total_users}] '
                            f'{user.email} - Exception: {str(e)}'
                        )
                    )
                    logger.error(f'Failed to generate fortune for user {user.id}: {e}')

        # Summary
        self.stdout.write(
            self.style.SUCCESS(
                f'\n{"="*60}\n'
                f'Fortune generation completed\n'
                f'{"="*60}\n'
                f'Total users: {total_users}\n'
                f'Success: {success_count}\n'
                f'Skipped: {skipped_count}\n'
                f'Errors: {error_count}\n'
                f'{"="*60}\n'
            )
        )

        if error_count > 0:
            self.stdout.write(
                self.style.WARNING(
                    f'\n⚠ {error_count} users had errors. Check logs for details.\n'
                )
            )

    def _generate_fortune_for_user(self, user: User, base_date) -> dict:
        """
        Generate fortune for a single user for the day after base_date.

        Calls FortuneService.generate_fortune() which schedules background AI generation.

        Args:
            user: User object
            base_date: date object (base date, will generate for next day)

        Returns:
            dict with 'status' ('success', 'error', 'skipped') and optional 'message'
        """
        try:
            # Check if user has required saju data
            if not user.birth_date_solar:
                return {
                    'status': 'skipped',
                    'message': 'No birth date'
                }

            # Initialize services
            image_service = ImageService()
            fortune_service = FortuneService(image_service)

            # Generate fortune for tomorrow (base_date + 1)
            # Convert date to datetime for service call
            base_datetime = datetime.combine(base_date, datetime.min.time())

            # This will schedule background AI generation via tasks.py
            # Images are skipped in batch to improve performance
            result = fortune_service.generate_fortune(
                user=user,
                date=base_datetime,
                generate_image=False  # Skip image generation in batch
            )

            if result.status == 'success':
                return {'status': 'success'}
            else:
                error_msg = result.error.message if result.error else 'Unknown error'
                return {
                    'status': 'error',
                    'message': error_msg
                }

        except Exception as e:
            logger.error(f'Error generating fortune for user {user.id}: {e}', exc_info=True)
            return {
                'status': 'error',
                'message': str(e)
            }
