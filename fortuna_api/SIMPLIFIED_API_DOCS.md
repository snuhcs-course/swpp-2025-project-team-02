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

**GET /auth/verify/**

JWT 토큰의 유효성을 검증합니다.

**Headers:**
```
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
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
  "birth_date_solar": "1995-03-15",
  "birth_date_lunar": "1995-02-14",
  "solar_or_lunar": "solar",
  "birth_time_units": "오시",
  "gender": "남자",
  "yearly_ganji": "을해",
  "monthly_ganji": "기묘",
  "daily_ganji": "무신",
  "hourly_ganji": "정오",
  "nickname": "운세왕",
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
- "자시", "축시", "인시", "묘시", "진시", "사시", "오시", "미시", "신시", "유시", "술시", "해시", "모름"

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
    "gender": "남자",
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

로그아웃하고 Refresh Token을 무효화합니다.

**Headers:**
```
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

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

---

## 데이터 모델

### User 모델

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✓ | 이메일 주소 (고유) |
| google_id | String |  | Google 계정 ID |
| profile_image | String |  | 프로필 이미지 URL |
| nickname | String |  | 닉네임 (고유, 2-20자) |
| birth_date | Date |  | 생년월일 |
| birth_time | Time |  | 출생시간 |
| gender | String |  | 성별 (M: 남자, F: 여자) |
| is_profile_complete | Boolean | ✓ | 프로필 완성 여부 |

### 성별 선택지
- `M`: 남자
- `F`: 여자

---

## 개발 가이드

### 설치 및 실행

```bash
# 1. 의존성 설치
pip install -e .[dev]

# 2. 환경 설정
cp .env.example .env
# .env 파일에 Google OAuth2 설정 추가

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

# 특정 테스트 실행
python manage.py test user.tests
```

### API 문서 확인

- Swagger UI: http://localhost:8000/api/docs/
- ReDoc: http://localhost:8000/api/redoc/

---

## 보안 고려사항

1. **HTTPS 필수**: 모든 토큰 통신은 암호화된 연결 사용
2. **토큰 안전 저장**: 클라이언트에서 안전한 저장소 사용
3. **토큰 만료 처리**: Refresh Token을 이용한 자동 갱신
4. **입력 검증**: 모든 사용자 입력에 대한 서버 측 검증

---

**핵심 기능들:**
- Google OAuth2 로그인
- JWT 토큰 인증
- 프로필 관리 (이름, 생년월일, 출생시간, 성별)
- 토큰 갱신 및 로그아웃