# Presigned URL 클라이언트 통합 가이드

## 📋 목차
1. [브라우저/React 통합](#브라우저react-통합)
2. [React Native 통합](#react-native-통합)
3. [CORS 설정](#cors-설정)
4. [테스트 방법](#테스트-방법)
5. [트러블슈팅](#트러블슈팅)

---

## 브라우저/React 통합

### 1. Presigned URL 받기
```javascript
// API에서 presigned URL 요청
const getUploadUrl = async (chakraType = 'fire') => {
  const response = await fetch(
    `${API_URL}/api/core/chakra/upload-url/?chakra_type=${chakraType}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );

  const data = await response.json();

  if (data.status === 'success') {
    return {
      uploadUrl: data.data.upload_url,
      key: data.data.key,
      fileId: data.data.file_id,
      expiresIn: data.data.expires_in
    };
  }

  throw new Error(data.message);
};
```

### 2. S3에 직접 업로드
```javascript
const uploadToS3 = async (presignedUrl, file) => {
  const response = await fetch(presignedUrl, {
    method: 'PUT',
    body: file,
    headers: {
      'Content-Type': file.type || 'image/jpeg'
    }
  });

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.status}`);
  }

  return response;
};
```

### 3. React 컴포넌트 예제
```jsx
import React, { useState } from 'react';

function ImageUpload() {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleUpload = async (file) => {
    setUploading(true);
    setProgress(0);

    try {
      // Step 1: Get presigned URL
      setProgress(25);
      const { uploadUrl, key } = await getUploadUrl('fire');

      // Step 2: Upload to S3
      setProgress(50);
      await uploadToS3(uploadUrl, file);

      // Step 3: Success!
      setProgress(100);
      console.log('Upload success! S3 key:', key);

      // Optional: 서버에 메타데이터 등록
      await registerImageMetadata(key);

    } catch (error) {
      console.error('Upload failed:', error);
      alert(`업로드 실패: ${error.message}`);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <input
        type="file"
        accept="image/*"
        onChange={(e) => handleUpload(e.target.files[0])}
        disabled={uploading}
      />
      {uploading && (
        <div>
          <progress value={progress} max="100" />
          <span>{progress}%</span>
        </div>
      )}
    </div>
  );
}
```

### 4. 진행률 표시 (XMLHttpRequest 사용)
```javascript
const uploadToS3WithProgress = (presignedUrl, file, onProgress) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        const percentComplete = (e.loaded / e.total) * 100;
        onProgress(percentComplete);
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        resolve(xhr.response);
      } else {
        reject(new Error(`Upload failed: ${xhr.status}`));
      }
    });

    xhr.addEventListener('error', () => {
      reject(new Error('Network error'));
    });

    xhr.open('PUT', presignedUrl);
    xhr.setRequestHeader('Content-Type', file.type || 'image/jpeg');
    xhr.send(file);
  });
};

// 사용 예시
await uploadToS3WithProgress(uploadUrl, file, (progress) => {
  console.log(`Upload progress: ${progress}%`);
  setProgress(progress);
});
```

---

## React Native 통합

### 1. 기본 업로드
```javascript
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system';

const uploadImage = async () => {
  // Step 1: 이미지 선택
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Images,
    quality: 0.8,
  });

  if (result.canceled) return;

  const uri = result.assets[0].uri;
  const fileType = uri.split('.').pop();

  // Step 2: Presigned URL 받기
  const { uploadUrl, key } = await getUploadUrl('fire');

  // Step 3: 업로드
  const uploadResponse = await FileSystem.uploadAsync(uploadUrl, uri, {
    httpMethod: 'PUT',
    headers: {
      'Content-Type': `image/${fileType}`,
    },
  });

  if (uploadResponse.status === 200) {
    console.log('Upload success!', key);
  }
};
```

### 2. 카메라 촬영 + 업로드
```javascript
const takePhotoAndUpload = async () => {
  // 카메라 권한 확인
  const { status } = await ImagePicker.requestCameraPermissionsAsync();
  if (status !== 'granted') {
    alert('카메라 권한이 필요합니다');
    return;
  }

  // 사진 촬영
  const result = await ImagePicker.launchCameraAsync({
    quality: 0.8,
    exif: true, // EXIF 데이터 포함
  });

  if (!result.canceled) {
    const uri = result.assets[0].uri;

    // 업로드
    await uploadImage(uri);
  }
};
```

### 3. 진행률 표시
```javascript
import { ProgressBar } from 'react-native-paper';

const [uploadProgress, setUploadProgress] = useState(0);

const uploadWithProgress = async (uri) => {
  const { uploadUrl } = await getUploadUrl();

  const uploadResumable = FileSystem.createUploadTask(
    uploadUrl,
    uri,
    {
      httpMethod: 'PUT',
      headers: { 'Content-Type': 'image/jpeg' },
    },
    (data) => {
      const progress = data.totalBytesSent / data.totalBytesExpectedToSend;
      setUploadProgress(progress);
    }
  );

  const result = await uploadResumable.uploadAsync();
  return result;
};

// UI
<ProgressBar progress={uploadProgress} color="#007bff" />
```

---

## CORS 설정

### S3/MinIO CORS 설정
```bash
# 1. Python 스크립트 실행
cd fortuna_api
python scripts/setup_s3_cors.py

# 2. 또는 AWS CLI 사용
aws s3api put-bucket-cors \
  --bucket your-bucket-name \
  --cors-configuration file://cors.json
```

### cors.json 예시
```json
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedOrigins": [
        "http://localhost:3000",
        "http://localhost:19006",
        "https://yourdomain.com"
      ],
      "ExposeHeaders": ["ETag", "Content-Length"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

### Django CORS 설정 (백엔드 API)
```python
# settings.py에 이미 설정됨
CORS_ALLOWED_ORIGINS = [
    "http://localhost:3000",
    "http://localhost:19006",
]

# 또는 개발 환경에서
CORS_ALLOW_ALL_ORIGINS = True  # 개발 전용!
```

---

## 테스트 방법

### 1. 브라우저 테스트
```bash
# Django 서버 실행
cd fortuna_api
python manage.py runserver

# 브라우저에서 열기
open test_client.html
# 또는
open http://localhost:8000/test_client.html
```

### 2. cURL로 테스트
```bash
# Step 1: Presigned URL 받기
curl -X GET "http://localhost:8000/api/core/chakra/upload-url/?chakra_type=fire" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq .

# Step 2: S3에 업로드
PRESIGNED_URL="https://..."
curl -X PUT "$PRESIGNED_URL" \
  -H "Content-Type: image/jpeg" \
  --data-binary @test.jpg

# Step 3: 확인
curl -I "$PRESIGNED_URL"  # HEAD 요청으로 확인
```

### 3. Postman/Insomnia 테스트
1. **GET** `/api/core/chakra/upload-url/`
   - Header: `Authorization: Bearer {token}`
   - Response에서 `upload_url` 복사

2. **PUT** `{upload_url}`
   - Body: Binary → 이미지 파일 선택
   - Header: `Content-Type: image/jpeg`

3. 성공 시 200 OK 응답

---

## 트러블슈팅

### ❌ CORS 오류
```
Access to fetch at '...' from origin 'http://localhost:3000'
has been blocked by CORS policy
```

**해결책:**
1. S3 CORS 설정 확인: `python scripts/setup_s3_cors.py`
2. Django CORS 설정 확인
3. 브라우저 개발자 도구에서 preflight 요청 확인

### ❌ 403 Forbidden
```
<Error><Code>SignatureDoesNotMatch</Code></Error>
```

**해결책:**
1. Presigned URL이 만료되었는지 확인 (5분 유효)
2. Content-Type이 일치하는지 확인
3. URL을 수정하지 않았는지 확인

### ❌ 업로드 후 파일이 없음
```
NoSuchKey: The specified key does not exist
```

**해결책:**
1. 업로드 응답이 200 OK인지 확인
2. S3 버킷과 키가 올바른지 확인
3. S3 콘솔에서 직접 확인

### ❌ React Native에서 업로드 실패
```
Network request failed
```

**해결책:**
1. HTTP가 아닌 HTTPS 사용 (production)
2. `info.plist` (iOS) 또는 `AndroidManifest.xml` (Android)에서 네트워크 권한 확인
3. 에뮬레이터가 아닌 실제 기기에서 테스트

---

## 성능 최적화

### 1. 이미지 압축
```javascript
import imageCompression from 'browser-image-compression';

const compressImage = async (file) => {
  const options = {
    maxSizeMB: 1,
    maxWidthOrHeight: 1920,
    useWebWorker: true
  };

  return await imageCompression(file, options);
};

// 사용
const compressed = await compressImage(originalFile);
await uploadToS3(uploadUrl, compressed);
```

### 2. 병렬 업로드
```javascript
const uploadMultiple = async (files) => {
  const uploads = files.map(async (file) => {
    const { uploadUrl, key } = await getUploadUrl();
    await uploadToS3(uploadUrl, file);
    return key;
  });

  return await Promise.all(uploads);
};
```

### 3. 재시도 로직
```javascript
const uploadWithRetry = async (uploadUrl, file, maxRetries = 3) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await uploadToS3(uploadUrl, file);
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      console.log(`Retry ${i + 1}/${maxRetries}`);
      await new Promise(r => setTimeout(r, 1000 * (i + 1)));
    }
  }
};
```

---

## 보안 체크리스트

- [ ] Presigned URL은 HTTPS 사용
- [ ] JWT 토큰은 안전하게 저장 (httpOnly cookie 또는 secure storage)
- [ ] Presigned URL은 짧은 만료 시간 설정 (5-15분)
- [ ] 파일 타입 검증 (클라이언트 + 서버)
- [ ] 파일 크기 제한 (클라이언트 + 서버)
- [ ] Rate limiting 적용
- [ ] S3 버킷은 private 설정
- [ ] CloudFront 사용 시 signed URL/cookie 활용

---

## 참고 자료

- [AWS S3 Presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
- [Django Storages](https://django-storages.readthedocs.io/)
- [React Native FileSystem](https://docs.expo.dev/versions/latest/sdk/filesystem/)
