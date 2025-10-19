# Daily Fortune Batch Generation

매일 오후 10시에 모든 활성 사용자에 대해 다음 날 운세를 미리 생성하는 배치 작업 가이드입니다.

## 개요

- **목적**: 사용자가 `/fortune/today` API를 호출할 때 즉시 응답하기 위해 미리 운세를 생성
- **실행 시간**: 매일 오후 10시 (22:00 KST = 13:00 UTC)
- **처리 방식**: 병렬 처리 (기본 5 workers, Railway에서는 10 workers 권장)
- **대상**: `is_active=True`이고 `birth_date_solar`이 있는 모든 사용자
- **아키텍처**: Worker Service (별도 서비스로 분리하여 API 서버 부하 방지)

## Architecture: Worker Service vs API Server

배치 작업은 **별도의 Worker Service**에서 실행됩니다. 이렇게 하면:

- ✅ API 서버가 배치 작업의 무거운 부하로부터 보호됨
- ✅ 배치 작업 실패 시 API 서버는 영향 없음
- ✅ 각 서비스를 독립적으로 스케일링 가능
- ✅ 리소스 사용을 명확하게 분리 가능

**Railway 배포 구조**:
- **Web Service**: Django API 서버 (Procfile의 `web` 사용)
- **Worker Service**: 배치 작업 전용 (Procfile의 `worker` 사용, `railway.toml`로 cron 설정)

## Method 1: Management Command (로컬 개발/테스트)

### 날짜 로직 이해하기 (중요!)

이 커맨드는 **base date** (기준 날짜)를 받아서 **그 다음 날** 운세를 생성합니다:

- `--date` 인자 없음 → base_date = 오늘, target_date = 내일
- `--date 2025-10-25` → base_date = 2025-10-25, target_date = 2025-10-26

**예시**: 2025년 10월 25일 오후 10시에 실행하면:
- Base date: 2025-10-25
- Target date: 2025-10-26 (10월 26일 운세 생성)

### 기본 사용법

```bash
# 기본 실행 (오늘 날짜 기준 → 내일 운세 생성)
python manage.py generate_daily_fortunes

# workers 수 지정
python manage.py generate_daily_fortunes --workers 10

# Dry run (테스트)
python manage.py generate_daily_fortunes --dry-run

# 특정 날짜 지정 (해당 날짜의 다음 날 운세 생성)
python manage.py generate_daily_fortunes --date 2025-10-25

# 특정 사용자만 (테스트용)
python manage.py generate_daily_fortunes --user-id 1

# 모든 옵션 조합
python manage.py generate_daily_fortunes --date 2025-10-25 --user-id 1 --workers 1 --dry-run
```

### 서버에서 직접 Cron 설정 (비권장)

서버에 직접 SSH 접근이 가능한 경우에만 사용:

```bash
# crontab -e
0 22 * * * cd /path/to/fortuna_api && /path/to/.venv/bin/python manage.py generate_daily_fortunes --workers 10 >> /var/log/fortuna/batch.log 2>&1
```

**참고**: Railway에서는 아래 Method 2를 사용하는 것이 더 권장됩니다.

## Method 2: Railway Worker Service (프로덕션 권장)

### 1. Procfile 설정

프로젝트 루트에 `Procfile` 생성:

```procfile
# Web Service: Django API Server
web: gunicorn fortuna_api.wsgi:application --bind 0.0.0.0:$PORT --workers 4 --timeout 120

# Worker Service: Background task handler
worker: echo "Worker service ready. Waiting for cron jobs..."
```

### 2. railway.toml 설정

프로젝트 루트에 `railway.toml` 생성:

```toml
[build]
builder = "NIXPACKS"

[deploy]
startCommand = "echo 'Worker service ready for cron jobs'"
restartPolicyType = "never"

# Daily Fortune Batch Generation
# Runs every day at 10 PM KST (1 PM UTC)
# Generates fortunes for TOMORROW (base_date = today, target_date = tomorrow)
[[cron]]
schedule = "0 13 * * *"
command = "python manage.py generate_daily_fortunes --workers 10"
```

**날짜 로직 설명**:
- Cron이 2025-10-25 오후 10시에 실행되면
- Base date: 2025-10-25 (오늘)
- Target date: 2025-10-26 (내일 운세 생성)
- 사용자가 다음 날 `/fortune/today` 호출 시 즉시 응답!

### 3. Railway에서 서비스 배포

#### Web Service (API Server)
1. Railway 프로젝트 생성
2. GitHub 레포지토리 연결
3. Service Type: **Web**
4. Procfile의 `web` 커맨드가 자동 실행됨
5. 환경 변수 설정:
   - `DATABASE_URL`
   - `SECRET_KEY`
   - `OPENAI_API_KEY`
   - `USE_S3=true`
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - 등...

#### Worker Service (Batch Jobs)
1. 같은 Railway 프로젝트에서 **New Service** 클릭
2. 같은 GitHub 레포지토리 선택
3. Service Type: **Worker**
4. `railway.toml` 설정을 자동으로 인식
5. Cron 스케줄이 자동으로 활성화됨
6. **중요**: Web Service와 동일한 환경 변수 설정 필요

### 4. 환경 변수 동기화

Worker Service는 Web Service와 같은 Django 설정을 사용하므로, 모든 환경 변수를 동일하게 설정해야 합니다:

```bash
# Railway UI에서 또는 CLI로 설정
railway variables set OPENAI_API_KEY="sk-..."
railway variables set DATABASE_URL="postgresql://..."
# ... 모든 환경 변수 복사
```

### 5. 배포 확인

```bash
# Railway CLI로 로그 확인
railway logs --service worker

# 수동으로 테스트 (Railway console에서)
python manage.py generate_daily_fortunes --dry-run --user-id 1
```

## 모니터링

### 로그 확인

```bash
# Django 로그
tail -f logs/django.log | grep "fortune"

# Batch command 로그 (cron에서 리다이렉트한 경우)
tail -f /var/log/fortuna/batch.log
```

### DB에서 확인

```python
from core.models import FortuneResult
from datetime import date, timedelta

tomorrow = date.today() + timedelta(days=1)
count = FortuneResult.objects.filter(for_date=tomorrow).count()
print(f"Generated fortunes for tomorrow: {count}")
```

## 성능

- **5 workers**: ~30명/초 (150명 약 5초)
- **10 workers**: ~50명/초 (150명 약 3초)
- **20 workers**: ~80명/초 (150명 약 2초)

실제 성능은 OpenAI API 응답 속도에 따라 다를 수 있습니다.

## 트러블슈팅

### 1. Worker Service가 실행되지 않음

- Railway UI에서 Worker Service의 로그를 확인
- `railway.toml` 파일이 프로젝트 루트에 있는지 확인
- Service Type이 "Worker"로 설정되어 있는지 확인

### 2. Cron이 실행되지 않음

- Railway Deployments 탭에서 Cron 스케줄 확인
- 타임존 확인: Railway는 UTC 사용 (22:00 KST = 13:00 UTC)
- `railway.toml`의 cron 설정이 올바른지 확인

### 3. 일부 사용자 실패

로그를 확인하여 구체적인 에러를 파악하세요. 주로 발생하는 원인:
- birth_date_solar가 없는 사용자
- OpenAI API 호출 실패
- DB 연결 문제

### 4. 메모리 부족 / OOM 에러

- Worker Service의 메모리 사용량을 Railway UI에서 확인
- workers 수를 줄이세요 (railway.toml에서 `--workers 5` 또는 `--workers 3`으로 변경)
- Railway의 메모리 제한을 확인하고 필요시 플랜 업그레이드

### 5. 환경 변수가 없다는 에러

Worker Service에 Web Service와 동일한 모든 환경 변수를 설정했는지 확인:
- `DATABASE_URL`
- `OPENAI_API_KEY`
- `SECRET_KEY`
- S3 관련 변수들

## 주의사항

1. **환경 변수 동기화**: Worker Service와 Web Service의 환경 변수가 동일해야 함 (특히 DATABASE_URL, OPENAI_API_KEY)
2. **API 제한**: OpenAI API rate limit에 주의 (workers 수 조절로 완화 가능)
3. **DB 부하**: 대량 사용자의 경우 workers 수를 조절하여 DB 부하 관리
4. **타임존**: Railway는 UTC를 사용하므로 KST 기준 22:00 = UTC 13:00
5. **중복 실행 방지**: Railway의 cron은 이전 작업이 완료되기 전에 새 작업을 시작하지 않음 (자동 방지)

## 향후 개선사항

- [x] Worker Service로 API 서버와 분리 (완료)
- [x] 병렬 처리로 성능 최적화 (완료)
- [ ] 실패한 사용자 재시도 로직 (exponential backoff)
- [ ] Slack/Discord 알림 연동 (성공/실패 통계)
- [ ] Railway Dashboard로 진행률 모니터링
- [ ] Celery로 마이그레이션 (장기적, 더 복잡한 워크플로우가 필요할 때)
