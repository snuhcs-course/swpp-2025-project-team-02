# TTS API 키 보안 가이드

## 현재 상태

현재 OpenAI API 키는 `local.properties`에 저장되고 BuildConfig를 통해 앱에 포함됩니다.

**보안 수준: ⚠️ 중간**
- ✅ Git에는 안전 (local.properties가 .gitignore에 포함)
- ❌ APK 디컴파일 시 노출 가능

---

## 프로덕션 배포 전 필수 조치

### 1. 백엔드 프록시 구현 (강력 추천)

**아키텍처:**
```
[Android App] → [Your Backend API] → [OpenAI Realtime API]
                 (API 키 보관)
```

**백엔드 엔드포인트 예시:**
```python
# FastAPI 예시
@app.post("/api/tts/speak")
async def tts_speak(text: str, user_id: str):
    # 사용자 인증 확인
    # 사용량 제한 체크
    # OpenAI API 호출
    # WebSocket 프록시
    pass
```

**장점:**
- API 키 완전 보호
- 사용량 모니터링 및 제어
- 사용자별 쿼터 설정
- 비용 최적화 (캐싱, rate limiting)

---

### 2. 임시 해결책: API 키 제한

OpenAI 대시보드에서 API 키 제한 설정:

1. **Usage limits**: 월 $10 같은 한도 설정
2. **Allowed models**: gpt-4o-realtime-preview만 허용
3. **Rate limiting**: 분당 요청 수 제한
4. **Monitor usage**: 일일 사용량 모니터링

---

## 개발 중 (현재)

**개발/테스트 단계에서는 현재 방식 사용 가능:**

1. `local.properties`에 개발용 API 키 저장
   ```properties
   OPENAI_API_KEY=sk-proj-dev-key-here
   ```

2. **주의사항:**
   - 팀원에게 APK 공유 금지
   - 개발용 API 키에 사용 한도 설정 ($5/month 등)
   - Firebase App Distribution이나 Play Store 내부 테스트 트랙 사용 시 주의

3. **Git 안전성 재확인:**
   ```bash
   git status local.properties
   # fatal: pathspec 'local.properties' did not match any files
   # → 추적되지 않음 = 안전
   ```

---

## 배포 체크리스트

프로덕션 배포 전 확인:

- [ ] 백엔드 TTS 프록시 API 구현
- [ ] 앱에서 백엔드 API 호출로 변경
- [ ] BuildConfig에서 OPENAI_API_KEY 제거
- [ ] API 키 사용량 모니터링 설정
- [ ] 사용자 인증 및 rate limiting 구현

---

## 대안: Android TTS 사용

비용과 보안이 걱정된다면 Android native TTS를 계속 사용하는 것도 좋은 선택:

```properties
# local.properties
OPENAI_API_KEY=  # 빈 값 = Android TTS 사용
```

**장점:**
- 완전 무료
- 오프라인 동작
- 보안 걱정 없음
- 레이턴시 0ms

**단점:**
- 음질이 OpenAI보다 떨어짐
