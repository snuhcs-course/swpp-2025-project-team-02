# Fortuna API 프론트엔드 인증 가이드

## 개요
Fortuna API는 JWT 기반의 인증 시스템을 사용하며, Google OAuth 2.0을 통한 소셜 로그인을 지원합니다.

## 기본 설정

### Base URL
```
개발환경: http://localhost:8000/api
운영환경: https://your-domain.com/api
```

### 공통 헤더
```javascript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${accessToken}` // 인증이 필요한 API만
}
```

## 1. Google OAuth 로그인

### 1.1 Google 로그인 버튼 구현
```javascript
// Google OAuth ID Token을 받아오는 예시 (google-auth-library 사용)
import { GoogleAuth } from 'google-auth-library';

const googleAuth = new GoogleAuth({
  clientId: 'YOUR_GOOGLE_CLIENT_ID'
});

// 구글 로그인 처리
async function handleGoogleLogin() {
  try {
    const response = await window.gapi.auth2.getAuthInstance().signIn();
    const idToken = response.getAuthResponse().id_token;

    // 서버로 ID 토큰 전송
    await loginWithGoogle(idToken);
  } catch (error) {
    console.error('Google login failed:', error);
  }
}
```

### 1.2 서버 인증 요청
```javascript
async function loginWithGoogle(idToken) {
  const response = await fetch('/api/user/auth/google/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      id_token: idToken
    })
  });

  if (response.ok) {
    const data = await response.json();

    // 토큰 저장
    localStorage.setItem('access_token', data.access_token);
    localStorage.setItem('refresh_token', data.refresh_token);

    // 사용자 정보 저장
    const userInfo = {
      user_id: data.user_id,
      email: data.email,
      name: data.name,
      profile_image: data.profile_image,
      is_new_user: data.is_new_user,
      needs_additional_info: data.needs_additional_info
    };

    localStorage.setItem('user_info', JSON.stringify(userInfo));

    // 새 사용자이거나 추가 정보가 필요한 경우 프로필 설정 페이지로 이동
    if (data.is_new_user || data.needs_additional_info) {
      window.location.href = '/profile-setup';
    } else {
      window.location.href = '/dashboard';
    }
  } else {
    const error = await response.json();
    console.error('Login failed:', error.message);
  }
}
```

## 2. 토큰 관리

### 2.1 Access Token 자동 갱신
```javascript
class TokenManager {
  constructor() {
    this.accessToken = localStorage.getItem('access_token');
    this.refreshToken = localStorage.getItem('refresh_token');
  }

  async refreshAccessToken() {
    try {
      const response = await fetch('/api/user/auth/refresh/', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          refresh: this.refreshToken
        })
      });

      if (response.ok) {
        const data = await response.json();
        this.accessToken = data.access;
        localStorage.setItem('access_token', data.access);
        return data.access;
      } else {
        // Refresh token도 만료된 경우 재로그인 필요
        this.logout();
        return null;
      }
    } catch (error) {
      console.error('Token refresh failed:', error);
      this.logout();
      return null;
    }
  }

  async getValidAccessToken() {
    // 토큰 만료 체크 (JWT 디코딩)
    if (this.isTokenExpired(this.accessToken)) {
      return await this.refreshAccessToken();
    }
    return this.accessToken;
  }

  isTokenExpired(token) {
    if (!token) return true;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp < currentTime;
    } catch (error) {
      return true;
    }
  }

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_info');
    window.location.href = '/login';
  }
}

const tokenManager = new TokenManager();
```

### 2.2 API 요청 인터셉터
```javascript
class ApiClient {
  constructor() {
    this.baseURL = '/api';
    this.tokenManager = new TokenManager();
  }

  async request(endpoint, options = {}) {
    const token = await this.tokenManager.getValidAccessToken();

    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
      },
      ...options
    };

    const response = await fetch(`${this.baseURL}${endpoint}`, config);

    // 401 에러 시 토큰 갱신 시도
    if (response.status === 401) {
      const newToken = await this.tokenManager.refreshAccessToken();
      if (newToken) {
        config.headers['Authorization'] = `Bearer ${newToken}`;
        return fetch(`${this.baseURL}${endpoint}`, config);
      }
    }

    return response;
  }

  async get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  async post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async patch(endpoint, data) {
    return this.request(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(data)
    });
  }
}

const apiClient = new ApiClient();
```

## 3. 사용자 프로필 관리

### 3.1 프로필 조회
```javascript
async function getUserProfile() {
  try {
    const response = await apiClient.get('/user/profile/');

    if (response.ok) {
      const profile = await response.json();
      return profile;
    } else {
      console.error('Failed to fetch profile');
      return null;
    }
  } catch (error) {
    console.error('Profile fetch error:', error);
    return null;
  }
}
```

### 3.2 프로필 업데이트 (생년월일, 성별 등)
```javascript
async function updateUserProfile(profileData) {
  try {
    const response = await apiClient.patch('/user/profile/', {
      nickname: profileData.nickname,
      birth_date: profileData.birthDate, // YYYY-MM-DD 형식
      solar_or_lunar: profileData.calendarType, // 'solar' 또는 'lunar'
      birth_time_units: profileData.birthTimeUnits, // 0-23 (시간)
      gender: profileData.gender // 'M' 또는 'F'
    });

    if (response.ok) {
      const result = await response.json();
      console.log('Profile updated:', result.message);
      return result.user;
    } else {
      const errors = await response.json();
      console.error('Profile update failed:', errors);
      return { errors };
    }
  } catch (error) {
    console.error('Profile update error:', error);
    return { error: 'Network error' };
  }
}
```

## 4. 로그아웃

```javascript
async function logout() {
  try {
    const refreshToken = localStorage.getItem('refresh_token');

    await apiClient.post('/user/auth/logout/', {
      refresh_token: refreshToken
    });

    // 로컬 스토리지 정리
    tokenManager.logout();
  } catch (error) {
    console.error('Logout error:', error);
    // 에러가 발생해도 로컬 스토리지는 정리
    tokenManager.logout();
  }
}
```

## 5. 토큰 검증

```javascript
async function verifyToken(token) {
  try {
    const response = await fetch('/api/user/auth/verify/', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        token: token
      })
    });

    if (response.ok) {
      const data = await response.json();
      return data.valid; // true or false
    }
    return false;
  } catch (error) {
    console.error('Token verification failed:', error);
    return false;
  }
}
```

## 6. 에러 처리

### 공통 에러 응답 형식
```javascript
// 인증 실패
{
  "error": "Invalid token",
  "message": "Google ID token verification failed"
}

// 프로필 업데이트 실패
{
  "nickname": ["닉네임은 2-20자 사이여야 합니다."],
  "birth_date": ["유효한 날짜를 입력해주세요."]
}

// 토큰 검증 실패 (/verify 엔드포인트)
{
  "message": "Token is invalid or expired",
  "valid": false
}
```

### 에러 처리 예시
```javascript
function handleApiError(error, response) {
  if (response.status === 401) {
    // 인증 실패 - 재로그인 필요
    tokenManager.logout();
  } else if (response.status === 400) {
    // 유효성 검사 실패 - 사용자에게 구체적인 오류 메시지 표시
    displayValidationErrors(error);
  } else if (response.status >= 500) {
    // 서버 오류
    showErrorMessage('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
  }
}
```

## 7. 개발 환경 특별 기능

개발 환경에서는 `user_id` 파라미터를 사용해 특정 사용자의 프로필을 조회할 수 있습니다:

```javascript
// 개발 환경에서만 사용 가능
async function getDevUserProfile(userId) {
  const response = await fetch(`/api/user/profile/?user_id=${userId}`);
  return response.json();
}
```

## 8. 전체 인증 플로우 예시

```javascript
class AuthFlow {
  async initialize() {
    // 페이지 로드 시 토큰 확인
    const token = await tokenManager.getValidAccessToken();

    if (token) {
      // 유효한 토큰이 있으면 사용자 정보 로드
      const profile = await getUserProfile();
      if (profile) {
        this.setUser(profile);
        return true;
      }
    }

    // 인증되지 않은 상태
    return false;
  }

  async loginWithGoogle(idToken) {
    const result = await loginWithGoogle(idToken);
    if (result.success) {
      this.setUser(result.user);
    }
    return result;
  }

  setUser(user) {
    // 앱 상태에 사용자 정보 설정
    this.currentUser = user;

    // 추가 정보가 필요한 경우 프로필 설정 페이지로 이동
    if (user.needs_additional_info) {
      this.redirectToProfileSetup();
    }
  }

  async logout() {
    await logout();
    this.currentUser = null;
  }
}

// 앱 초기화
const auth = new AuthFlow();
auth.initialize();
```

이 가이드를 참고하여 프론트엔드에서 Fortuna API의 인증 시스템을 구현할 수 있습니다. 추가 질문이 있으면 언제든지 문의해주세요!