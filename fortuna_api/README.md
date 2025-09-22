# Fortuna API

A Django REST API for fortune telling with Google authentication, photo uploads, and AR field generation.

## Features

- **Google OAuth2 Authentication** with django-allauth
- **JWT Token Authentication** for mobile apps
- **Custom User Model** with fortune telling fields
- **Traditional Korean Saju Calculation** using 60 Gapja system
- **Photo Upload** with fortune analysis
- **AR Field Generation** with five elements (목화토금수)
- **Real-time Fortune Readings** based on traditional Korean fortune telling
- **Admin Interface** for managing data

## Technology Stack

- Django 5.0+
- Django REST Framework
- PostgreSQL
- JWT Authentication
- Google OAuth2
- Traditional Korean Saju (사주) calculations

## Quick Start

### 1. Install Dependencies

```bash
cd fortuna_api
pip install -e .[dev]
```

### 2. Environment Setup

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```env
SECRET_KEY=your-secret-key
DEBUG=True
DATABASE_URL=postgresql://user:password@localhost:5432/fortuna_db
GOOGLE_OAUTH2_CLIENT_ID=your-google-client-id
GOOGLE_OAUTH2_CLIENT_SECRET=your-google-client-secret
```

### 3. Database Setup

```bash
python manage.py makemigrations
python manage.py migrate
python manage.py createsuperuser
```

### 4. Run Development Server

```bash
python manage.py runserver
```

## API Endpoints

### Authentication

- `POST /api/auth/login/` - Login with email/password
- `POST /api/auth/logout/` - Logout
- `POST /api/auth/google/` - Google OAuth login
- `POST /api/auth/registration/` - Register new user

### User Management

- `GET /api/profile/` - Get user profile
- `PUT /api/profile/` - Update user profile (including saju code)

### Fortune Telling

- `GET /api/daily/` - Get daily fortune reading
- `POST /api/photo/upload/` - Upload photo with context
- `GET /api/photos/` - Get user's photo history
- `GET /api/ar/field/` - Generate AR field objects
- `GET /api/saju-codes/` - Get available 60 Gapja codes

### Health Check

- `GET /api/health/` - Simple health check endpoint

### API Documentation

- Swagger UI: `http://localhost:8000/api/docs/`
- ReDoc: `http://localhost:8000/api/redoc/`
- OpenAPI Schema: `http://localhost:8000/api/schema/`

## Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
   - `http://localhost:8000/accounts/google/login/callback/` (development)
   - `https://yourdomain.com/accounts/google/login/callback/` (production)

6. Add the credentials to Django admin:
   - Go to `/admin/socialaccount/socialapp/`
   - Create new Social Application for Google
   - Add Client ID and Secret
   - Select your site

## Models

### User Model
- Email-based authentication
- Google OAuth integration
- Saju code field for fortune telling

### PhotoEvent
- User photo uploads with context
- Real-time fortune analysis
- Linked to user's saju data

### FortuneReading
- Generated fortune readings based on traditional saju
- Daily fortune calculations
- Five elements analysis (목화토금수)

## Development Commands

```bash
# Run tests
python manage.py test

# Code quality checks
black .
isort .
flake8 .
mypy .

# Generate migrations
python manage.py makemigrations

# Shell with Django context
python manage.py shell_plus
```

## Production Deployment

1. Set environment variables
2. Configure PostgreSQL database
3. Configure static file serving
4. Set up SSL certificates
5. Configure Google OAuth for production domain

## Architecture

**Ultra-simplified single app structure:**

```
fortuna_api/
├── fortuna_api/           # Project settings
├── fortuna/               # Single app with everything
│   ├── models.py         # User, PhotoEvent, FortuneReading
│   ├── views.py          # All API endpoints
│   ├── serializers.py    # DRF serializers
│   ├── admin.py          # Admin interface
│   ├── saju_concepts.py  # Traditional Korean saju logic
│   ├── fortune_calculator.py # Fortune calculations
│   └── urls.py           # URL configuration
├── static/               # Static files
├── media/                # User uploads
└── templates/            # Django templates
```

## API Flow Examples

### Mobile Authentication Flow

1. Android app gets Google ID token
2. Send ID token to `/api/auth/google/`
3. Server validates token and returns JWT
4. Use JWT for subsequent API calls

### Fortune Reading Flow

1. User sets saju code via `PUT /api/profile/`
2. User uploads photo via `POST /api/photo/upload/`
3. Server analyzes using traditional saju calculation
4. User can view AR field via `GET /api/ar/field/`
5. Daily fortune available at `GET /api/daily/`

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new features
4. Run code quality checks
5. Submit pull request

## License

MIT License
