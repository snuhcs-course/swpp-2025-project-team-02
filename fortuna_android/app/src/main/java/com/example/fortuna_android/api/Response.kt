package com.example.fortuna_android.api

import com.google.gson.annotations.SerializedName

abstract class ServerResponse<T>  {
    abstract val status: Int
    abstract val data: T
}

// GET /api/core/chakra/upload-url/
data class UploadUrlData(
    val uploadUrl: String,
    val key: String,
    val fileId: String,
    val expiresIn: Int
)

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
    @SerializedName("fortune_id")
    val fortuneId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("for_date")
    val forDate: String,
    @SerializedName("fortune_status")
    val fortuneStatus: String,
    val fortune: Fortune,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("tomorrow_gapja")
    val tomorrowGapja: TomorrowGapja
)

data class TomorrowGapja(
    val code: Int,
    val name: String,
    val element: String
)

data class Fortune(
    @SerializedName("tomorrow_date")
    val tomorrowDate: String,
    @SerializedName("daily_guidance")
    val dailyGuidance: DailyGuidance,
    @SerializedName("chakra_readings")
    val chakraReadings: List<ChakraReading>,
    @SerializedName("element_balance")
    val elementBalance: String,
    @SerializedName("fortune_summary")
    val fortuneSummary: String,
    @SerializedName("overall_fortune")
    val overallFortune: Int,
    @SerializedName("special_message")
    val specialMessage: String,
    @SerializedName("saju_compatibility")
    val sajuCompatibility: String
)

data class ChakraReading(
    val message: String,
    val strength: Int,
    @SerializedName("chakra_type")
    val chakraType: String,
    @SerializedName("location_significance")
    val locationSignificance: String
)

data class DailyGuidance(
    @SerializedName("best_time")
    val bestTime: String,
    @SerializedName("key_advice")
    val keyAdvice: String,
    @SerializedName("lucky_color")
    val luckyColor: String,
    @SerializedName("lucky_direction")
    val luckyDirection: String,
    @SerializedName("activities_to_avoid")
    val activitiesToAvoid: List<String>,
    @SerializedName("activities_to_embrace")
    val activitiesToEmbrace: List<String>
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
    @SerializedName("last_login") val lastLogin: String?,
    @SerializedName("collection_status") val collectionStatus: CollectionStatus?
)

data class CollectionStatus(
    @SerializedName("collections") val collections: List<CollectionItem>,
    @SerializedName("total_count") val totalCount: Int
) {
    /**
     * Get count for a specific chakra type
     * Returns 0 if not found
     */
    fun getCount(chakraType: String): Int {
        return collections.find { it.chakraType.equals(chakraType, ignoreCase = true) }?.count ?: 0
    }

    /**
     * Helper properties for easy access to element counts
     */
    val wood: Int get() = getCount("wood")
    val fire: Int get() = getCount("fire")
    val earth: Int get() = getCount("earth")
    val metal: Int get() = getCount("metal")
    val water: Int get() = getCount("water")
}

data class CollectionItem(
    @SerializedName("chakra_type") val chakraType: String,
    @SerializedName("count") val count: Int
)

// Deprecated - keeping for backward compatibility with old API responses
@Deprecated("Use CollectionStatus instead")
data class CollectedElements(
    @SerializedName("wood") val wood: Int = 0,    // 木 (green) - wood
    @SerializedName("fire") val fire: Int = 0,    // 火 (red) - fire
    @SerializedName("earth") val earth: Int = 0,   // 土 (orange) - earth
    @SerializedName("metal") val metal: Int = 0,   // 金 (gray) - metal
    @SerializedName("water") val water: Int = 0    // 水 (blue) - water
)

// PATCH /api/user/profile/ 요청 시 Body에 담을 데이터
data class UpdateProfileRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("input_birth_date") val inputBirthDate: String,
    @SerializedName("input_calendar_type") val inputCalendarType: String,
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

// GET /api/fortune/today/
data class TodayFortuneResponse(
    val status: String,
    val data: TodayFortuneData
)

data class TodayFortuneData(
    @SerializedName("fortune_id")
    val fortuneId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("generated_at")
    val generatedAt: String?,
    @SerializedName("for_date")
    val forDate: String?,
    val fortune: TodayFortune,
    @SerializedName("fortune_score")
    val fortuneScore: FortuneScore
)

data class TodayFortune(
    @SerializedName("today_fortune_summary")
    val todayFortuneSummary: String,
    @SerializedName("today_daily_guidance")
    val todayDailyGuidance: String,
    @SerializedName("today_element_balance_description")
    val todayElementBalanceDescription: String
)

data class FortuneScore(
    @SerializedName("entropy_score")
    val entropyScore: Double,
    val elements: Map<String, ElementPillar>,
    @SerializedName("element_distribution")
    val elementDistribution: Map<String, ElementDistribution>,
    val interpretation: String
)

data class ElementPillar(
    @SerializedName("two_letters")
    val twoLetters: String,
    val stem: StemBranchDetail,
    val branch: StemBranchDetail
)

data class StemBranchDetail(
    @SerializedName("korean_name")
    val koreanName: String,
    val element: String,
    @SerializedName("element_color")
    val elementColor: String,
    @SerializedName("yin_yang")
    val yinYang: String,
    val animal: String? = null
)

data class ElementDistribution(
    val count: Int,
    val percentage: Double
)

// GET /api/core/chakra/needed-element/
data class NeededElementResponse(
    val status: String,
    val data: NeededElementData
)

data class NeededElementData(
    val date: String,
    @SerializedName("needed_element")
    val neededElement: String  // Korean: 목/화/토/금/수
)

// POST /api/chakra/collect
data class CollectElementRequest(
    @SerializedName("chakra_type") val chakraType: String  // English: fire/water/earth/metal/wood
)

data class CollectElementResponse(
    val status: String,
    val data: CollectElementData
)

data class CollectElementData(
    val message: String,
    @SerializedName("collected_elements")
    val collectedElements: CollectedElements
)

// GET /api/chakra/collection-status
data class CollectionStatusResponse(
    val status: String,
    val data: CollectionStatusData
)

data class CollectionStatusData(
    @SerializedName("collected_elements")
    val collectedElements: CollectedElements
)

// GET /api/core/chakras/today-progress/
data class TodayProgressResponse(
    val status: String,
    val data: TodayProgressData
)

data class TodayProgressData(
    val date: String,
    @SerializedName("needed_element")
    val neededElement: String,
    @SerializedName("needed_element_en")
    val neededElementEn: String,
    @SerializedName("current_count")
    val currentCount: Int,
    @SerializedName("target_count")
    val targetCount: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percentage")
    val progressPercentage: Int
)

// GET /api/chakra/monthly-history/
data class MonthlyHistoryResponse(
    val status: String,
    val data: MonthlyHistoryData
)

data class MonthlyHistoryData(
    val year: Int,
    val month: Int,
    val days: List<DayData>,
    val summary: SummaryData
)

data class DayData(
    val date: String,
    @SerializedName("needed_element")
    val neededElement: String,
    @SerializedName("needed_element_en")
    val neededElementEn: String,
    @SerializedName("target_count")
    val targetCount: Int,
    @SerializedName("collected_count")
    val collectedCount: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("progress_percentage")
    val progressPercentage: Int
)

data class SummaryData(
    @SerializedName("total_days")
    val totalDays: Int,
    @SerializedName("completed_days")
    val completedDays: Int,
    @SerializedName("completion_rate")
    val completionRate: Int,
    @SerializedName("total_collected")
    val totalCollected: Int
)

// GET /api/chakra/element-focused-history/
data class ElementHistoryResponse(
    val status: String,
    val data: ElementHistoryData
)

data class ElementHistoryData(
    val element: String,  // English: wood/fire/earth/metal/water
    @SerializedName("element_kr")
    val elementKr: String,  // Korean: 목/화/토/금/수
    @SerializedName("total_count")
    val totalCount: Int,
    val history: List<ElementHistoryDay>
)

data class ElementHistoryDay(
    val date: String,  // YYYY-MM-DD format
    @SerializedName("collected_count")
    val collectedCount: Int
)
