package com.example.fortuna_android.data.Api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

// Retrofit API 통신을 위한 데이터 클래스 정의

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

interface LoginApiService {
    @POST("api/user/auth/google/")
    suspend fun loginWithGoogle(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/user/profile/")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @GET("api/user/profile/")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>

    @PATCH("api/user/profile/")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @POST("api/user/auth/logout/")
    suspend fun logout(@Body request: LogoutRequest): Response<LogoutResponse>
}

object RetrofitClient {
//    private const val BASE_URL = "http://10.15.57.163:8000/"
    private const val BASE_URL = "http://10.0.2.2:8000/"
    val instance: LoginApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(LoginApiService::class.java)
    }
}