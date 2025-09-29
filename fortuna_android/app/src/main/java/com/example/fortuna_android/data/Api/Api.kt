package com.example.fortuna_android.data.Api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
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

interface LoginApiService {
    @POST("api/user/auth/google/")
    suspend fun loginWithGoogle(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/user/profile/")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>
}

object RetrofitClient {
    private const val BASE_URL = "http://172.30.1.66:8000/"

    val instance: LoginApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(LoginApiService::class.java)
    }
}