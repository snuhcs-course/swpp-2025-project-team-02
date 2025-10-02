package com.example.fortuna_android.api

import com.google.gson.annotations.SerializedName

// GET /api/core/chakra/images/
data class ImageResponse(
    val status: String,
    val data: ImageData
)
data class ImageData(
    val date: String,
    val images: List<ImageItem>,
    val count: Int
)
data class ImageItem(
    val filename: String,
    val path: String,
    val url: String
)

// POST /api/core/chakra/upload/
data class UploadResponse(
    val status: String,
    val data: UploadData?
)
data class UploadData(
    val filename: String?,
    val url: String?,
    @SerializedName("chakra_type")
    val chakraType: String?
)

// GET /api/core/fortune/tomorrow/
data class FortuneResponse(
    val status: String,
    val data: FortuneData
)

data class FortuneData(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("generated_at")
    val generatedAt: String,
    @SerializedName("for_date")
    val forDate: String,
    @SerializedName("tomorrow_gapja")
    val tomorrowGapja: TomorrowGapja,
    val fortune: Fortune
)

data class TomorrowGapja(
    val code: Int,
    val name: String,
    val chinese: String,
    val element: String,
    val animal: String
)

data class Fortune(
    @SerializedName("tomorrow_date")
    val tomorrowDate: String,
    @SerializedName("saju_compatibility")
    val sajuCompatibility: String,
    @SerializedName("overall_fortune")
    val overallFortune: Int,
    @SerializedName("fortune_summary")
    val fortuneSummary: String,
    @SerializedName("element_balance")
    val elementBalance: String,
    @SerializedName("chakra_readings")
    val chakraReadings: List<ChakraReading>,
    @SerializedName("daily_guidance")
    val dailyGuidance: DailyGuidance,
    @SerializedName("special_message")
    val specialMessage: String
)

data class ChakraReading(
    @SerializedName("chakra_type")
    val chakraType: String,
    val strength: Int,
    val message: String,
    @SerializedName("location_significance")
    val locationSignificance: String
)

data class DailyGuidance(
    @SerializedName("best_time")
    val bestTime: String,
    @SerializedName("lucky_direction")
    val luckyDirection: String,
    @SerializedName("lucky_color")
    val luckyColor: String,
    @SerializedName("activities_to_embrace")
    val activitiesToEmbrace: List<String>,
    @SerializedName("activities_to_avoid")
    val activitiesToAvoid: List<String>,
    @SerializedName("key_advice")
    val keyAdvice: String
)

// Authentication and user profile related data classes

// POST /api/auth/google/ 요청 시 Body에 담을 데이터
data class LoginRequest(
    @SerializedName("id_token") val idToken: String
)

// POST /api/auth/google/ 요청 성공 시 응답 데이터
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("profile_image") val profileImage: String,
    @SerializedName("is_new_user") val isNewUser: Boolean,
    @SerializedName("needs_additional_info") val needsAdditionalInfo: Boolean,
)

// GET /api/profile/ 요청 성공 시 응답 데이터
data class User(
    @SerializedName("pk") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String
)

// GET /api/user/profile/ 요청 성공 시 응답 데이터 (사용자 상세 프로필)
data class UserProfile(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("profile_image") val profileImage: String?,
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("birth_date_solar") val birthDateSolar: String?,
    @SerializedName("birth_date_lunar") val birthDateLunar: String?,
    @SerializedName("solar_or_lunar") val solarOrLunar: String?,
    @SerializedName("birth_time_units") val birthTimeUnits: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("yearly_ganji") val yearlyGanji: String?,
    @SerializedName("monthly_ganji") val monthlyGanji: String?,
    @SerializedName("daily_ganji") val dailyGanji: String?,
    @SerializedName("hourly_ganji") val hourlyGanji: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("last_login") val lastLogin: String?
)

// PATCH /api/user/profile/ 요청 시 Body에 담을 데이터
data class UpdateProfileRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("solar_or_lunar") val solarOrLunar: String,
    @SerializedName("birth_time_units") val birthTimeUnits: String,
    @SerializedName("gender") val gender: String
)

// PATCH /api/user/profile/ 응답 데이터
data class UpdateProfileResponse(
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UpdatedUserData
)

data class UpdatedUserData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("birth_date_solar") val birthDateSolar: String,
    @SerializedName("birth_date_lunar") val birthDateLunar: String,
    @SerializedName("solar_or_lunar") val solarOrLunar: String,
    @SerializedName("birth_time_units") val birthTimeUnits: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("yearly_ganji") val yearlyGanji: String,
    @SerializedName("monthly_ganji") val monthlyGanji: String,
    @SerializedName("daily_ganji") val dailyGanji: String,
    @SerializedName("hourly_ganji") val hourlyGanji: String
)

// POST /api/user/auth/logout/ 요청 시 Body에 담을 데이터
data class LogoutRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

// POST /api/user/auth/logout/ 응답 데이터 (구조를 확인한 후 수정 예정)
data class LogoutResponse(
    @SerializedName("message") val message: String?
)

// POST /api/user/auth/refresh/ 요청 시 Body에 담을 데이터
data class RefreshTokenRequest(
    @SerializedName("refresh") val refresh: String
)

// POST /api/user/auth/refresh/ 응답 데이터
data class RefreshTokenResponse(
    @SerializedName("access") val access: String
)
