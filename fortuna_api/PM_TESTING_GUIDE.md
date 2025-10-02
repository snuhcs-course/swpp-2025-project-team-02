# 🎯 PM을 위한 Fortuna API 테스팅 가이드

## 📋 목차
1. [환경 설정](#1-환경-설정)
2. [서버 실행 방법](#2-서버-실행-방법)
3. [6개 API 테스트 가이드](#3-6개-api-테스트-가이드)
4. [안드로이드 앱 연동 가이드](#4-안드로이드-앱-연동-가이드)
5. [FAQ & 트러블슈팅](#5-faq--트러블슈팅)

---

## 1. 환경 설정

### 1.1 필수 프로그램 설치
```bash
# 1. Python 3.8+ 설치 확인
python --version

# 2. 프로젝트 클론
git clone [repository-url]
cd fortuna_api

# 3. Poetry를 사용한 의존성 설치
poetry install

# 4. 가상환경 활성화 (선택사항)
poetry shell
# 또는 poetry run 명령어로 실행
```

### 1.2 환경 변수 설정 (중요!)

#### 📚 .env 파일이란?
**.env 파일**은 환경변수를 저장하는 파일로, **민감한 정보**(비밀번호, API 키 등)를 코드와 분리해서 관리하는 방법입니다.

**왜 필요한가요?**
- 🔒 **보안**: 비밀번호나 API 키를 코드에 직접 쓰지 않음
- 🌍 **환경별 설정**: 개발환경과 배포환경에서 다른 값 사용 가능
- 👥 **팀 협업**: 개발자마다 다른 설정값 사용 가능

#### 🛠️ .env 파일 생성 방법
```bash
# 1. .env.example 파일을 복사해서 .env 파일 생성
cp .env.example .env

# 2. .env 파일을 텍스트 에디터로 열어서 수정
# (VS Code, 메모장, nano 등 아무거나 사용)
```

#### 📝 개발환경용 .env 파일 설정
**.env 파일에 다음 내용을 입력하세요:**

```env
# ===== Django 기본 설정 =====
SECRET_KEY=dev-secret-key-for-fortuna-api-2024
DEBUG=True
ALLOWED_HOSTS=localhost,127.0.0.1,10.0.2.2

# ===== 데이터베이스 설정 (개발환경) =====
# SQLite 사용 (간단한 개발용)
USE_SQLITE=True

# PostgreSQL 사용하려면 아래 주석 해제하고 USE_SQLITE=False로 변경
# DB_NAME=fortuna_db_dev
# DB_USER=postgres
# DB_PASSWORD=your-dev-password
# DB_HOST=localhost
# DB_PORT=5432

# ===== Google OAuth 설정 (⚠️ 반드시 설정 필요) =====
GOOGLE_OAUTH2_CLIENT_ID=your-google-client-id
GOOGLE_OAUTH2_CLIENT_SECRET=your-google-client-secret

# ===== OpenAI API 설정 =====
OPENAI_API_KEY=your-openai-api-key

# ===== Redis 설정 (개발환경에서는 선택사항) =====
REDIS_URL=redis://localhost:6379/0
```

#### 🚀 배포환경용 .env 파일 설정
**배포 서버(프로덕션)에서는 다음과 같이 설정:**

```env
# ===== Django 기본 설정 (배포환경) =====
SECRET_KEY=super-secure-production-secret-key-2024
DEBUG=False
ALLOWED_HOSTS=yourdomain.com,api.yourdomain.com

# ===== 데이터베이스 설정 (배포환경) =====
# PostgreSQL 필수 사용
USE_SQLITE=False
DB_NAME=fortuna_db_prod
DB_USER=fortuna_user
DB_PASSWORD=super-secure-db-password
DB_HOST=your-db-server.amazonaws.com
DB_PORT=5432

# ===== Google OAuth 설정 (배포환경용) =====
GOOGLE_OAUTH2_CLIENT_ID=production-google-client-id
GOOGLE_OAUTH2_CLIENT_SECRET=production-google-client-secret

# ===== OpenAI API 설정 =====
OPENAI_API_KEY=production-openai-api-key

# ===== Redis 설정 (배포환경에서는 필수) =====
REDIS_URL=redis://production-redis-server.com:6379/0
```

#### ⚠️ 중요 주의사항
1. **절대 .env 파일을 Git에 올리지 마세요!** (이미 .gitignore에 포함됨)
2. **실제 값은 PM이 직접 입력해야 합니다**
3. **개발환경과 배포환경의 .env 파일은 서로 다릅니다**

### 1.3 데이터베이스 설정

#### 🗄️ 데이터베이스가 뭐고 왜 필요한가요?
데이터베이스는 우리 앱의 **모든 데이터를 저장하는 곳**입니다.
- 사용자 정보 (이메일, 이름, 생년월일 등)
- 사주 계산 결과
- 운세 분석 데이터
- Google 로그인 정보 등

#### 📱 개발환경 (로컬 컴퓨터) 데이터베이스 설정

**옵션 1: SQLite 사용 (추천 - 간단함) 🟢**
```bash
# .env 파일에서 USE_SQLITE=True로 설정했다면
# 별도 설치 없이 바로 사용 가능!

# 마이그레이션 실행 (테이블 생성)
poetry run python manage.py migrate

# 슈퍼유저 생성 (관리자 계정, 선택사항)
poetry run python manage.py createsuperuser
# 이메일, 비밀번호 입력하면 관리자 계정 생성됨
```

**옵션 2: PostgreSQL 사용 (실제 서버와 동일 환경) 🟡**
```bash
# 1. PostgreSQL 설치
# macOS:
brew install postgresql

# Windows:
# https://www.postgresql.org/download/windows/ 에서 다운로드

# Ubuntu/Linux:
sudo apt-get install postgresql postgresql-contrib

# 2. PostgreSQL 시작
brew services start postgresql  # macOS
# Windows는 서비스에서 자동 시작됨

# 3. 데이터베이스 생성
createdb fortuna_db_dev

# 4. .env 파일에서 다음 값 변경:
# USE_SQLITE=False
# DB_NAME=fortuna_db_dev
# DB_USER=postgres
# DB_PASSWORD=your-password
# DB_HOST=localhost
# DB_PORT=5432

# 5. 마이그레이션 실행
poetry run python manage.py migrate

# 6. 슈퍼유저 생성 (선택사항)
poetry run python manage.py createsuperuser
# 이메일, 비밀번호 입력하면 관리자 계정 생성됨
```

#### 🚀 배포환경 (실제 서버) 데이터베이스 설정

**PostgreSQL 필수 사용 (SQLite는 배포환경에서 부적합)**

```bash
# 1. 클라우드 PostgreSQL 서비스 사용 (추천)
# - AWS RDS PostgreSQL
# - Google Cloud SQL PostgreSQL
# - Azure Database for PostgreSQL
# - Supabase (무료 옵션)

# 2. .env 파일 설정 (배포서버에서)
USE_SQLITE=False
DB_NAME=fortuna_db_prod
DB_USER=fortuna_user
DB_PASSWORD=super-secure-production-password
DB_HOST=your-cloud-db-server.com
DB_PORT=5432

# 3. 배포 서버에서 마이그레이션 실행
python manage.py migrate

# 4. 슈퍼유저 생성
python manage.py createsuperuser
# 이메일, 비밀번호 입력하면 관리자 계정 생성됨
```

#### 🔍 데이터베이스 선택 가이드

| 환경 | 데이터베이스 | 장점 | 단점 |
|------|------------|-----|-----|
| **개발환경** | SQLite | ✅ 설치 불필요<br>✅ 설정 간단<br>✅ 파일 하나로 관리 | ❌ 성능 제한<br>❌ 동시 접속 제한 |
| **개발환경** | PostgreSQL | ✅ 실제 서버와 동일<br>✅ 고성능<br>✅ 모든 기능 지원 | ❌ 설치/설정 필요<br>❌ 메모리 사용량 높음 |
| **배포환경** | PostgreSQL | ✅ 고성능<br>✅ 안정성<br>✅ 확장성 | ❌ 비용 발생<br>❌ 관리 복잡 |

#### 💡 PM을 위한 추천 방법
1. **처음 시작할 때**: SQLite 사용 (USE_SQLITE=True)
2. **테스트가 잘 되면**: PostgreSQL로 변경해서 실제 환경 테스트
3. **배포할 때**: 반드시 PostgreSQL 사용

---

## 2. 서버 실행 방법

### 2.1 개발 서버 시작
```bash
# Poetry를 사용한 서버 실행
poetry run python manage.py runserver

# 또는 가상환경 활성화 후 실행
poetry shell
python manage.py runserver

# 성공 메시지 확인
# Starting development server at http://127.0.0.1:8000/
```

### 2.2 API 문서 확인
브라우저에서 다음 URL 접속:
- **Swagger UI**: http://127.0.0.1:8000/api/docs/
- **API Schema**: http://127.0.0.1:8000/api/schema/

---

## 3. 6개 API 테스트 가이드

### 📱 API 1: Google 로그인
**⚠️ PM이 반드시 알아야 할 인증 정보:**

```http
POST http://127.0.0.1:8000/api/user/auth/google/
Content-Type: application/json

{
    "id_token": "테스트용_가짜_토큰"
}
```

**🔧 테스트 방법 (실제 Google 계정 불필요):**
1. **Postman**에서 위 요청 보내기
2. **예상 응답**: 400 Bad Request (정상 - 가짜 토큰이므로)
3. **실제 테스트를 위해서는**:
   - Google OAuth 2.0 Playground 사용
   - 또는 안드로이드 앱에서 실제 로그인

**✅ 성공 응답 예시:**
```json
{
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "user": {
        "email": "user@gmail.com",
        "name": "사용자이름",
        "is_new_user": true
    }
}
```

### 📱 API 2: 프로필 조회
```http
GET http://127.0.0.1:8000/api/user/profile/
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**🔑 인증 토큰 사용법:**
1. API 1에서 받은 `access_token` 복사
2. Postman Headers에 추가: `Authorization: Bearer 토큰값`

### 📱 API 3: 프로필 업데이트 (생년월일 & 사주 계산)
```http
PATCH http://127.0.0.1:8000/api/user/profile/
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
    "nickname": "테스트닉네임",
    "birth_date": "2001-09-21",
    "solar_or_lunar": "solar",
    "birth_time_units": "오시",
    "gender": "F"
}
```

**📊 응답에서 확인할 사주 정보:**
```json
{
    "birth_date_solar": "2001-09-21",
    "birth_date_lunar": "2001-08-05",
    "yearly_ganji": "신사",
    "monthly_ganji": "정미",
    "daily_ganji": "경자",
    "hourly_ganji": "임오"
}
```

### 📱 API 4: 토큰 갱신
```http
POST http://127.0.0.1:8000/api/user/auth/refresh/
Content-Type: application/json

{
    "refresh": "YOUR_REFRESH_TOKEN"
}
```

### 📱 API 5: 토큰 검증
```http
POST http://127.0.0.1:8000/api/user/auth/verify/
Content-Type: application/json

{
    "token": "YOUR_ACCESS_TOKEN"
}
```

**✅ 성공 응답 (토큰 유효 - 200):**
```json
{
    "message": "Token is valid",
    "valid": true
}
```

**✅ 성공 응답 (토큰 무효 - 200):**
```json
{
    "message": "Token is invalid or expired",
    "valid": false
}
```

**💡 참고**: 토큰이 무효하더라도 200 응답이 반환됩니다. `valid` 필드로 유효성을 판단하세요.

### 📱 API 6: 로그아웃
```http
POST http://127.0.0.1:8000/api/user/auth/logout/
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
    "refresh": "YOUR_REFRESH_TOKEN"
}
```

---

## 4. 안드로이드 앱 연동 가이드

### 4.1 Android Studio 설정

**1. 프로젝트 열기:**
```
File → Open → [안드로이드 프로젝트 폴더 선택]
```

**2. 필수 의존성 확인 (app/build.gradle):**
```kotlin
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
}
```

**3. 네트워크 보안 설정 (AndroidManifest.xml):**
```xml
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

### 4.2 API 클라이언트 설정

**ApiClient.kt 예시:**
```kotlin
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/api/"  // 에뮬레이터용
    // private const val BASE_URL = "http://192.168.1.100:8000/api/"  // 실제 기기용

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

### 4.3 Google OAuth 설정

**1. Google Cloud Console 설정:**
- 프로젝트 생성 → OAuth 2.0 클라이언트 ID 생성
- Android 앱용 클라이언트 ID 추가
- SHA-1 지문 등록

**2. google-services.json 파일:**
```
app/ 폴더에 google-services.json 파일 추가
```

### 4.4 테스트 시나리오

**시나리오 1: 새 사용자 가입**
1. Google 로그인 버튼 클릭
2. Google 계정 선택
3. 생년월일 입력 화면에서 "2001-09-21" 입력
4. 사주 정보 자동 계산 확인

**시나리오 2: 기존 사용자 로그인**
1. 동일한 Google 계정으로 재로그인
2. 기존 프로필 정보 표시 확인

---

## 5. FAQ & 트러블슈팅

### Q1: "제가 인증 API 테스트하려면 어떻게 해야 하나요?"
**A:** 두 가지 방법이 있습니다:

**방법 1 - Postman으로 기본 테스트:**
```json
// 가짜 토큰으로 에러 응답 확인
{
    "id_token": "fake_token_for_testing"
}
// → 400 Bad Request 받으면 API 구조는 정상
```

**방법 2 - 실제 Google 토큰 사용:**
1. [Google OAuth 2.0 Playground](https://developers.google.com/oauthplayground/) 접속
2. `https://www.googleapis.com/auth/userinfo.profile` 스코프 선택
3. 생성된 `id_token` 사용

### Q2: "제 실제 구글 아이디 입력해야 하나요?"
**A:** 네, 실제 테스트를 위해서는 필요합니다:
- **개발/테스트**: 본인 Google 계정 사용 권장
- **데모**: 테스트용 Google 계정 생성 권장

### Q3: "특별한 값 세팅이 필요한가요?"
**A:** 네, 다음 설정이 필요합니다:
```env
# .env 파일에 반드시 설정
GOOGLE_OAUTH2_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
GOOGLE_OAUTH2_CLIENT_SECRET=GOCSPX-abcdef123456
```

### Q4: 서버 연결 안 될 때
**확인사항:**
1. `python manage.py runserver` 실행 중인지 확인
2. 방화벽 설정 확인
3. 안드로이드 에뮬레이터: `10.0.2.2:8000` 사용
4. 실제 기기: PC의 IP 주소 사용 (예: `192.168.1.100:8000`)

### Q5: 사주 계산 결과 검증
**예상 결과:**
- 양력 2001-09-21 → 음력 2001-08-05
- 사주: 신사(년) 정미(월) 경자(일) 임오(시)

### Q6: 데이터베이스 초기화
```bash
# 기존 데이터 삭제 후 재시작
poetry run python manage.py flush
poetry run python manage.py migrate
```

### Q7: "개발환경과 배포환경의 차이점이 뭔가요?"
**A:** 환경별 주요 차이점:

| 구분 | 개발환경 (로컬) | 배포환경 (서버) |
|------|-------------|-------------|
| **목적** | 개발 및 테스트 | 실제 사용자 서비스 |
| **데이터베이스** | SQLite (선택) 또는 PostgreSQL | PostgreSQL 필수 |
| **보안** | DEBUG=True (에러 정보 표시) | DEBUG=False (보안 강화) |
| **도메인** | localhost:8000 | yourdomain.com |
| **성능** | 속도보다 편의성 중심 | 최적화된 성능 |
| **비용** | 무료 (로컬 컴퓨터) | 유료 (서버, DB 등) |

### Q8: "PostgreSQL 설치가 어려워요!"
**A:** 단계별 해결 방법:

**쉬운 방법:**
1. 우선 SQLite로 개발 시작 (.env에서 USE_SQLITE=True)
2. 기능 테스트가 완료되면 PostgreSQL 도입

**PostgreSQL 설치 도움:**
- macOS: `brew install postgresql`
- Windows: PostgreSQL 공식 홈페이지에서 installer 다운로드
- 설치 후 서비스 시작: `brew services start postgresql`

### Q9: ".env 파일 값을 어떻게 얻나요?"
**A:** 각 서비스별 설정 방법:

**Google OAuth:**
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 → APIs & Services → Credentials
3. OAuth 2.0 클라이언트 ID 생성
4. 웹 애플리케이션 선택
5. 승인된 리디렉션 URI 추가: `http://localhost:8000/accounts/google/login/callback/`

**OpenAI API:**
1. [OpenAI Platform](https://platform.openai.com/) 접속
2. API Keys 섹션에서 새 키 생성
3. 생성된 키를 .env 파일에 입력

### Q10: "슈퍼유저(관리자 계정)가 뭔가요?"
**A:** 슈퍼유저는 **Django 관리자 패널에 접속할 수 있는 최고 권한 계정**입니다.

**슈퍼유저로 할 수 있는 것:**
- 🔧 Django Admin 패널 접속 (http://127.0.0.1:8000/admin/)
- 👥 모든 사용자 데이터 조회/수정/삭제
- 📊 데이터베이스의 모든 테이블 내용 확인
- 🛠️ 서버 설정 및 디버깅

**슈퍼유저 생성 방법:**
```bash
poetry run python manage.py createsuperuser

# 입력해야 하는 정보:
# Email: admin@example.com (원하는 이메일)
# Password: ******** (8자 이상, 복잡한 비밀번호)
# Password (again): ******** (확인용)
```

**슈퍼유저 사용 시나리오:**
- **개발 중**: 사용자 데이터 확인, 테스트 데이터 수정
- **디버깅**: API에서 생성된 데이터가 제대로 저장되었는지 확인
- **PM 업무**: 실제 사용자 데이터 현황 파악

**⚠️ 주의사항:**
- 배포환경에서는 매우 강력한 비밀번호 사용
- 관리자 계정 정보는 절대 공유하지 말 것

---

## 🎯 PM 체크리스트

### 🔧 개발환경 설정 완료 체크
- [ ] Python 3.8+ 설치 및 Poetry 설치
- [ ] 프로젝트 클론 및 `poetry install` 성공
- [ ] .env 파일 생성 및 기본값 설정
- [ ] 데이터베이스 선택 (SQLite 추천)
- [ ] `poetry run python manage.py migrate` 성공
- [ ] `poetry run python manage.py runserver` 성공
- [ ] Google OAuth 클라이언트 ID/Secret 설정

### 📱 API 테스트 완료 체크 (개발환경)
- [ ] Swagger UI 접속 (http://127.0.0.1:8000/api/docs/)
- [ ] Google 로그인 API 구조 확인 (400 에러도 정상)
- [ ] 프로필 조회/업데이트 테스트 (실제 Google 토큰 사용)
- [ ] 토큰 갱신/검증/로그아웃 API 테스트
- [ ] 사주 계산 기능 테스트 (생년월일 입력)

### 🚀 배포환경 설정 완료 체크
- [ ] 클라우드 서버 준비 (AWS, GCP, Azure 등)
- [ ] PostgreSQL 데이터베이스 서비스 설정
- [ ] 배포환경용 .env 파일 설정 (DEBUG=False)
- [ ] Google OAuth 배포용 클라이언트 ID/Secret 설정
- [ ] 도메인 및 SSL 인증서 설정
- [ ] 서버에서 마이그레이션 및 배포 테스트

### 📱 안드로이드 연동 체크
- [ ] Android Studio 프로젝트 빌드 성공
- [ ] Google OAuth 안드로이드용 설정 완료
- [ ] 개발 서버와 안드로이드 앱 통신 확인 (10.0.2.2:8000)
- [ ] 실제 기기에서 API 통신 확인 (PC IP 주소 사용)
- [ ] 사주 계산 결과 검증 및 UI 표시 확인

---

**🔗 추가 도움이 필요하시면:**
- 개발팀 Slack: #fortuna-api-support
- 이슈 제보: GitHub Issues
- 긴급 연락: [개발자 연락처]

---
*이 문서는 비개발자 PM을 위해 작성되었습니다. 기술적 용어보다는 실용적인 가이드에 중점을 두었습니다.*