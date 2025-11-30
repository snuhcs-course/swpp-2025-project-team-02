package com.example.fortuna_android.util

import com.example.fortuna_android.api.UpdatedUserData
import com.example.fortuna_android.api.UserProfile

/**
 * 프로필 관련 유틸리티 클래스
 * 프로필 완성도 체크 등의 공통 로직을 제공합니다.
 */
object ProfileUtils {

    /**
     * 사용자 프로필이 완성되었는지 확인합니다.
     *
     * 완성 조건:
     * - nickname이 비어있지 않음
     * - birthDateLunar 또는 birthDateSolar 중 하나 이상 존재
     * - solarOrLunar (음력/양력 구분)이 비어있지 않음
     * - birthTimeUnits (시간)이 비어있지 않음
     * - gender (성별)이 비어있지 않음
     *
     * @param profile 확인할 사용자 프로필
     * @return 프로필이 완성되었으면 true, 아니면 false
     */
    fun isProfileComplete(profile: UserProfile?): Boolean {
        // 프로필이 null이면 미완성
        if (profile == null) {
            return false
        }

        // 생년월일이 하나라도 있는지 확인
        val hasBirthDate = !profile.birthDateLunar.isNullOrEmpty() ||
                          !profile.birthDateSolar.isNullOrEmpty()

        // 모든 필수 필드가 채워져 있는지 확인
        return !profile.nickname.isNullOrEmpty() &&
               hasBirthDate &&
               !profile.solarOrLunar.isNullOrEmpty() &&
               !profile.birthTimeUnits.isNullOrEmpty() &&
               !profile.gender.isNullOrEmpty()
    }

    /**
     * UpdatedUserData 타입의 프로필이 완성되었는지 확인합니다.
     *
     * @param profile 확인할 업데이트된 사용자 데이터
     * @return 프로필이 완성되었으면 true, 아니면 false
     */
    fun isProfileComplete(profile: UpdatedUserData?): Boolean {
        // 프로필이 null이면 미완성
        if (profile == null) {
            return false
        }

        // 생년월일이 하나라도 있는지 확인
        val hasBirthDate = profile.birthDateLunar.isNotEmpty() ||
                          profile.birthDateSolar.isNotEmpty()

        // 모든 필수 필드가 채워져 있는지 확인
        return profile.nickname.isNotEmpty() &&
               hasBirthDate &&
               profile.solarOrLunar.isNotEmpty() &&
               profile.birthTimeUnits.isNotEmpty() &&
               profile.gender.isNotEmpty()
    }

    /**
     * 프로필이 기본값으로만 채워져 있는지 확인합니다.
     * 백엔드가 자동으로 설정한 기본값인지 판단합니다.
     *
     * @param profile 확인할 사용자 프로필
     * @return 기본값으로만 채워져 있으면 true
     */
    fun isDefaultProfile(profile: UserProfile?): Boolean {
        if (profile == null) return false

        // 기본값 패턴들
        val isDefaultNickname = profile.nickname.isNullOrEmpty() ||
                               profile.nickname == "DefaultUser" ||
                               profile.nickname == "User"
        val isDefaultBirthDate = profile.birthDateSolar == "1900-01-01" ||
                                profile.birthDateLunar == "1900-01-01"

        return isDefaultNickname || isDefaultBirthDate
    }
}