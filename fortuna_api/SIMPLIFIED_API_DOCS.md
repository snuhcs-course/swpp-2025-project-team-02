# Fortuna User API 명세서 (간소화 버전)

이 문서는 Fortuna 앱의 핵심 사용자 인증 및 프로필 관리 API를 정의합니다.

## Base URL
```
http://localhost:8000/api/user/
```

## 인증 방식
- JWT Bearer Token 인증
- Google OAuth2 로그인

---

## API 엔드포인트

### 1. Google 로그인/회원가입 API

**POST /auth/google/**

Google ID Token으로 로그인하거나 새 계정을 생성합니다.

**Request:**
```json
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjdkYzc..."
}
```

**Response (성공 - 200):**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "user_id": 123,
  "email": "user@gmail.com",
  "name": "홍길동",
  "profile_image": "https://lh3.googleusercontent.com/...",
  "is_new_user": true,
  "needs_additional_info": true
}
```

**Response (실패 - 400):**
```json
{
  "error": "Invalid token",
  "message": "Google ID token verification failed"
}
```

---

### 2. 토큰 갱신 API

**POST /auth/refresh/**

Access Token 만료 시 새 토큰을 발급합니다.

**Request:**
```json
{
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Response (성공 - 200):**
```json
{
  "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Response (실패 - 401):**
```json
{
  "detail": "Token is invalid or expired",
  "code": "token_not_valid"
}
```

---

### 3. 토큰 검증 API

**POST /auth/verify/**

JWT 토큰의 유효성을 검증합니다.

**Request:**
```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Response (토큰 유효 - 200):**
```json
{
  "message": "Token is valid",
  "valid": true
}
```

**Response (토큰 무효 - 200):**
```json
{
  "message": "Token is invalid or expired",
  "valid": false
}
```

**Response (토큰 누락 - 400):**
```json
{
  "error": "Missing token",
  "message": "Token field is required"
}
```

**Response (실패 - 401):**
```json
{
  "detail": "Token is invalid or expired",
  "code": "token_not_valid"
}
```

---

### 4. 사용자 프로필 조회 API

**GET /profile/**

현재 로그인한 사용자의 프로필 정보를 조회합니다.

**Headers:**
```
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

**Response (성공 - 200):**
```json
{
  "user_id": 123,
  "email": "user@gmail.com",
  "name": "홍길동",
  "profile_image": "https://lh3.googleusercontent.com/...",
  "nickname": "운세왕",
  "birth_date_solar": "1995-03-15",
  "birth_date_lunar": "1995-02-14",
  "solar_or_lunar": "solar",
  "birth_time_units": "오시",
  "gender": "Male",
  "yearly_ganji": "을해",
  "monthly_ganji": "기묘",
  "daily_ganji": "무신",
  "hourly_ganji": "정오",
  "created_at": "2024-10-15T10:30:00Z",
  "last_login": "2024-10-20T09:15:00Z"
}
```

**Response (실패 - 401):**
```json
{
  "detail": "Authentication credentials were not provided."
}
```

---

### 5. 사용자 프로필 업데이트 API

**PATCH /profile/**

사용자의 추가 정보를 업데이트합니다 (닉네임, 생년월일, 시진, 성별).

**Headers:**
```
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

**Request:**
```json
{
  "nickname": "운세마스터",
  "birth_date": "1995-03-15",
  "solar_or_lunar": "solar",
  "birth_time_units": "오시",
  "gender": "M"
}
```

**시진 (birth_time_units) 옵션:**
- "자시", "축시", "인시", "묘시", "진시", "사시", "오시", "미시", "신시", "유시", "술시", "해시"

**성별 (gender) 옵션:**
- "M": 남자
- "F": 여자

**Response (성공 - 200):**
```json
{
  "message": "Profile updated successfully",
  "user": {
    "user_id": 123,
    "email": "user@gmail.com",
    "name": "홍길동",
    "nickname": "운세마스터",
    "birth_date_solar": "1995-03-15",
    "birth_date_lunar": "1995-02-14",
    "solar_or_lunar": "solar",
    "birth_time_units": "오시",
    "gender": "Male",
    "yearly_ganji": "을해",
    "monthly_ganji": "기묘",
    "daily_ganji": "무신",
    "hourly_ganji": "정오"
  }
}
```

**Response (실패 - 401):**
```json
{
  "detail": "Authentication credentials were not provided."
}
```

---

### 6. 로그아웃 API

**POST /auth/logout/**

로그아웃하고 Refresh Token을 무효화합니다 (토큰 블랙리스트 처리).

**Request:**
```json
{
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Response (성공 - 200):**
```json
{
  "message": "Successfully logged out"
}
```

> **참고**: `refresh_token` 필드는 선택사항입니다. 제공하지 않아도 로그아웃은 성공으로 처리됩니다.

---

## 데이터 모델

### User 모델

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✓ | 이메일 주소 (고유) |
| google_id | String |  | Google 계정 ID (고유) |
| profile_image | String |  | 프로필 이미지 URL |
| nickname | String |  | 닉네임 (고유, 2-20자) |
| birth_date_solar | Date |  | 양력 생년월일 |
| birth_date_lunar | Date |  | 음력 생년월일 |
| solar_or_lunar | String |  | 달력 타입 (solar/lunar) |
| birth_time_units | String |  | 시진 (자시~해시) |
| gender | String |  | 성별 (M: 남자, F: 여자) |
| yearly_ganji | String |  | 년주 (계산됨) |
| monthly_ganji | String |  | 월주 (계산됨) |
| daily_ganji | String |  | 일주 (계산됨) |
| hourly_ganji | String |  | 시주 (계산됨) |
| is_profile_complete | Boolean | ✓ | 프로필 완성 여부 |
| created_at | DateTime | ✓ | 계정 생성일 |
| last_login | DateTime |  | 마지막 로그인 |

### 달력 타입 (solar_or_lunar)
- `solar`: 양력
- `lunar`: 음력

### 성별 (gender)
- `M`: 남자
- `F`: 여자

### 시진 (birth_time_units)
- `자시`, `축시`, `인시`, `묘시`, `진시`, `사시`, `오시`, `미시`, `신시`, `유시`, `술시`, `해시`

### 사주 계산
- 사용자가 생년월일 정보를 입력하면 자동으로 사주 간지(yearly_ganji, monthly_ganji, daily_ganji, hourly_ganji)가 계산됩니다.
- 양력/음력 변환도 자동으로 수행되어 두 날짜가 모두 저장됩니다.

---

## 개발 가이드

### 설치 및 실행

```bash
# 1. 의존성 설치
pip install -e .[dev]

# 2. 환경 설정
cp .env.example .env
# .env 파일에 다음 설정 추가:
# - GOOGLE_OAUTH_CLIENT_ID
# - SECRET_KEY
# - DATABASE_URL (선택사항, 기본값: SQLite)

# 3. 데이터베이스 마이그레이션
python manage.py makemigrations
python manage.py migrate

# 4. 개발 서버 실행
python manage.py runserver
```

### 테스트

```bash
# 전체 테스트 실행
python manage.py test

# 특정 앱 테스트 실행
python manage.py test user.tests
python manage.py test core.tests
```

### API 문서 확인

- Swagger UI: http://localhost:8000/api/docs/
- ReDoc: http://localhost:8000/api/redoc/

### 개발 모드 특별 기능

개발 환경에서는 `user_id` 파라미터로 인증 없이 특정 사용자 프로필을 조회할 수 있습니다:

```
GET /api/user/profile/?user_id=1
```

---

## 보안 고려사항

1. **HTTPS 필수**: 프로덕션 환경에서는 모든 토큰 통신에 암호화된 연결 사용
2. **토큰 안전 저장**: 클라이언트에서 안전한 저장소 사용 (예: iOS Keychain, Android KeyStore)
3. **토큰 만료 처리**: Refresh Token을 이용한 자동 갱신 로직 구현
4. **입력 검증**: 모든 사용자 입력에 대한 서버 측 검증
5. **토큰 블랙리스트**: 로그아웃 시 Refresh Token 블랙리스트 처리

---

## 주요 변경 사항 및 특징

### 인증 시스템
- Google OAuth2 기반 소셜 로그인
- JWT (Access Token + Refresh Token) 방식
- 토큰 블랙리스트 지원 (로그아웃 시)
- 커스텀 토큰 검증 엔드포인트

### 사용자 프로필
- 생년월일 양력/음력 자동 변환
- 전통 사주 간지 자동 계산 (년주, 월주, 일주, 시주)
- 닉네임 중복 검사 및 길이 제한 (2-20자)
- 프로필 완성도 자동 체크

### 개발 편의 기능
- 개발 모드에서 `user_id` 파라미터로 인증 우회 가능
- drf-spectacular 기반 자동 API 문서 생성
- 상세한 에러 메시지

---

## 문의 및 지원

프로젝트 관련 문의사항은 개발팀에 문의해주세요.