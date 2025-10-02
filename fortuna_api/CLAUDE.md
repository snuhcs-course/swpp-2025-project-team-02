# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Development Server
```bash
python manage.py runserver
```

### Database Management
```bash
# Create migrations
python manage.py makemigrations

# Apply migrations
python manage.py migrate

# Create superuser
python manage.py createsuperuser

# Django shell with extensions
python manage.py shell_plus
```

### Testing
```bash
# Run all tests
python manage.py test

# Run specific test file
python manage.py test core.tests.test_fortune_service
```

### Code Quality
```bash
# Format code
black .

# Sort imports
isort .

# Lint code
flake8 .

# Type checking
mypy .
```

### Dependencies
```bash
# Install in development mode
pip install -e .[dev]
```

## Architecture

This is a Django REST API for Korean traditional fortune telling (Saju) with modern features:

### App Structure
- **core/**: Main business logic for fortune telling, image processing, and Saju calculations
- **user/**: Custom user model with Google OAuth integration and saju-related fields
- **fortuna_api/**: Project settings with multiple environment configurations

### Key Services
- **FortuneService** (`core/services/fortune.py`): AI-powered fortune generation using OpenAI and traditional Korean Saju system
- **ImageService** (`core/services/image.py`): Photo upload and processing for "chakra" readings
- **Saju Concepts** (`core/utils/concept.py`): Traditional Korean 60 Gapja system and Five Elements calculations

### Authentication Architecture
- JWT tokens for mobile app authentication
- Google OAuth2 via django-allauth
- Custom User model extending AbstractUser with birth date and saju fields
- Session-based auth for admin and web

### Database Models
- **User**: Custom user model with Korean fortune telling fields (birth_date, birth_time, birth_location)
- **PhotoEvent**: User photo uploads with context for fortune analysis
- **FortuneReading**: Generated fortune readings based on traditional saju
- **NotificationSettings**: User preference management
- **UserDevice**: Security tracking for user devices

### Settings Configuration
- **base.py**: Shared settings with PostgreSQL, JWT, OAuth, and API documentation
- **development.py**: Development-specific overrides
- **testing.py**: Test environment settings
- Uses environment variables via python-decouple

### API Endpoints Structure
- `/api/auth/`: Authentication (login, logout, Google OAuth, registration)
- `/api/core/`: Fortune telling endpoints (daily readings, photo uploads, AR fields)
- `/api/user/`: User management and profile
- `/api/docs/`: Swagger UI documentation
- `/api/schema/`: OpenAPI schema

### Traditional Korean Fortune Telling Integration
- 60 Gapja system for daily energy calculation
- Five Elements (오행: 목화토금수) compatibility analysis
- Day Pillar (일간) based fortune readings
- Real-time compatibility scoring between user's birth chart and current dates

The codebase follows Korean traditional fortune telling principles while providing modern REST API interfaces for mobile applications.