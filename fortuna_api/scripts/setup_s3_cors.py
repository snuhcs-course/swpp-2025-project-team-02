#!/usr/bin/env python
"""
S3/MinIO CORS 설정 스크립트
Presigned URL을 클라이언트에서 사용하기 위해 필요한 CORS 설정
"""

import boto3
import os
from decouple import config

# S3 클라이언트 설정
s3_client = boto3.client(
    's3',
    aws_access_key_id=config('AWS_ACCESS_KEY_ID'),
    aws_secret_access_key=config('AWS_SECRET_ACCESS_KEY'),
    endpoint_url=config('AWS_S3_ENDPOINT_URL'),
    region_name=config('AWS_S3_REGION_NAME', default='us-east-1'),
)

bucket_name = config('AWS_STORAGE_BUCKET_NAME')

# CORS 규칙 정의
cors_configuration = {
    'CORSRules': [
        {
            'AllowedHeaders': ['*'],
            'AllowedMethods': ['GET', 'PUT', 'POST', 'DELETE', 'HEAD'],
            'AllowedOrigins': [
                'http://localhost:3000',      # React 개발 서버
                'http://localhost:5173',      # Vite 개발 서버
                'http://localhost:19006',     # Expo 개발 서버
                'https://yourdomain.com',     # 프로덕션 도메인
            ],
            'ExposeHeaders': [
                'ETag',
                'Content-Length',
                'Content-Type',
            ],
            'MaxAgeSeconds': 3600  # 1시간
        }
    ]
}

try:
    # CORS 설정 적용
    s3_client.put_bucket_cors(
        Bucket=bucket_name,
        CORSConfiguration=cors_configuration
    )
    print(f"✅ CORS 설정 성공: {bucket_name}")
    print("\n허용된 Origin:")
    for rule in cors_configuration['CORSRules']:
        for origin in rule['AllowedOrigins']:
            print(f"  - {origin}")

    # 현재 CORS 설정 확인
    current_cors = s3_client.get_bucket_cors(Bucket=bucket_name)
    print("\n✅ 현재 CORS 설정:")
    print(current_cors['CORSRules'])

except Exception as e:
    print(f"❌ CORS 설정 실패: {e}")
    print("\n해결 방법:")
    print("1. AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY 확인")
    print("2. 버킷 이름 확인")
    print("3. IAM 권한 확인 (s3:PutBucketCors)")
