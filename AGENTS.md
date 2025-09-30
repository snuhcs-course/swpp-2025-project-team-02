# Repository Guidelines

## Project Structure & Module Organization
- `fortuna_api/` — Django REST API (settings in `fortuna_api/fortuna_api/settings/`, app code in `fortuna_api/core/`, tests in `fortuna_api/core/tests/`).
- `fortuna_android/` — Android app (Kotlin). Source in `app/src/main/`, unit tests in `app/src/test/`, instrumented tests in `app/src/androidTest/`.
- Root configs: `pyproject.toml` (Python deps, Black/Ruff/Mypy/Pytest), `poetry.lock`, `.env.example`.

## Build, Test, and Development Commands
- Backend (Python/Django)
  - `cd fortuna_api && poetry install --with dev` — install dependencies.
  - `cp ../.env.example .env` — local env (fill secrets).
  - `python manage.py migrate && python manage.py runserver` — run API.
  - `pytest -q` — run tests; `pytest --cov=fortuna_api` for coverage.
- Android
  - `cd fortuna_android && ./gradlew assembleDebug` — build debug APK.
  - `./gradlew test` — JVM unit tests; `./gradlew connectedAndroidTest` — instrumented tests (device/emulator).

## Coding Style & Naming Conventions
- Python: 4‑space indent, Black (line length 88). Lint with Ruff (`ruff check . --fix`). Type‑check with Mypy (`mypy .`).
- Imports: follow Ruff isort rules (see `tool.ruff.lint.isort`).
- Naming: snake_case (functions/vars), PascalCase (classes), kebab-case for branch names.
- Kotlin: follow official Android Kotlin style (UpperCamelCase classes, lowerCamelCase members).

## Testing Guidelines
- Python: Pytest + pytest-django. Place tests under `fortuna_api/core/tests/` with `test_*.py`.
- Prefer small, focused tests around services in `core/services/` and API tests hitting `core/urls.py` routes.
- Android: put unit tests in `app/src/test/...` and instrumented tests in `app/src/androidTest/...`.

## Commit & Pull Request Guidelines
- Commits: use Conventional Commits (e.g., `feat:`, `fix:`, `chore:`, `test:`). Keep messages imperative and scoped.
- PRs: include clear description, linked issue (e.g., `Closes #123`), test coverage notes, and screenshots for Android UI changes. Ensure `pytest` and `./gradlew test` pass and code is formatted/linted.

## Security & Configuration
- Never commit secrets. Use `.env` (backend) and local Gradle properties (Android). Django settings profiles live in `fortuna_api/fortuna_api/settings/{development,testing,production}.py`.
- Default DB is PostgreSQL; configure via env vars (`DB_HOST`, `DB_NAME`, etc.).

## Agent-Specific Notes
- Respect existing structure; update or add tests with any behavior change. Run Black, Ruff, and Mypy before opening a PR.
