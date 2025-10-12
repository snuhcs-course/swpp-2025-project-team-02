package com.example.fortuna_android.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
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

    @GET("api/core/chakra/images/")
    suspend fun getImages(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<ImageResponse>

    @Multipart
    @POST("api/core/chakra/upload/") // Replace with your actual upload endpoint
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("chakra_type") chakraType: RequestBody
    ): Response<UploadResponse>

    @GET("api/core/fortune/tomorrow/")
    suspend fun getFortune(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<FortuneResponse>

    @POST("api/user/auth/refresh/")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
}