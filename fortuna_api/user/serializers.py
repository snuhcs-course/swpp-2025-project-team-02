from rest_framework import serializers
from django.contrib.auth import get_user_model

User = get_user_model()

class GoogleLoginSerializer(serializers.Serializer):
    """Google 로그인 요청 시리얼라이저"""
    id_token = serializers.CharField()



class UserProfileUpdateSerializer(serializers.ModelSerializer):
    """프로필 업데이트 시리얼라이저"""
    input_birth_date = serializers.DateField(write_only=True, required=False)
    input_calendar_type = serializers.ChoiceField(
        choices=['solar', 'lunar'],
        write_only=True,
        required=False
    )

    class Meta:
        model = User
        fields = [
            'nickname',
            'input_birth_date',
            'input_calendar_type',
            'birth_time_units',
            'gender'
        ]

    def validate_nickname(self, value):
        """
        닉네임 유효성 검증

        길이 검증: 2-6자
        """
        # 닉네임 길이 검증
        if len(value) < 2 or len(value) > 6:
            raise serializers.ValidationError("닉네임은 2-6자 사이여야 합니다.")

        return value

    def update(self, instance, validated_data):
        """
        사용자 프로필 업데이트

        1. 기본 필드(닉네임, 성별 등) 업데이트
        2. 생년월일이 있으면 양력/음력 변환 및 사주팔자 계산
        3. 프로필 완성도 재계산
        """
        # 생년월일 정보 추출 (write_only 필드이므로 pop 필요)
        input_birth_date = validated_data.pop('input_birth_date', None)
        input_calendar_type = validated_data.pop('input_calendar_type', None)
        input_birth_time_units = validated_data.pop('birth_time_units', None)

        # 일반 필드들 업데이트 (닉네임, 성별 등)
        for attribute_name, attribute_value in validated_data.items():
            setattr(instance, attribute_name, attribute_value)

        # 생년월일 정보가 있으면 양력/음력 변환 및 사주 계산
        if input_birth_date and input_calendar_type:
            instance.set_birth_date_and_calculate_saju(
                input_birth_date,
                input_calendar_type,
                input_birth_time_units
            )

        # 프로필 완성도 재계산
        instance.update_profile_completeness_status()

        instance.save()

        return instance
