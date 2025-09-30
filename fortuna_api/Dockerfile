# Simple Dockerfile for Fortuna Django API
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    POETRY_VERSION=1.8.3

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libjpeg62-turbo \
    zlib1g \
    libpng16-16 \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Install Poetry
RUN pip install --no-cache-dir "poetry==${POETRY_VERSION}"

WORKDIR /app

# Copy dependency files
COPY pyproject.toml poetry.lock ./

# Install dependencies
RUN poetry config virtualenvs.create false \
    && poetry install --with production --without dev

# Copy Django project
COPY . ./

# Create directories and set permissions
RUN mkdir -p /app/media /app/logs

EXPOSE 8000

# Run server
CMD python manage.py migrate --noinput && \
    uvicorn fortuna_api.asgi:application \
    --host 0.0.0.0 \
    --port 8000 \
    --workers 2