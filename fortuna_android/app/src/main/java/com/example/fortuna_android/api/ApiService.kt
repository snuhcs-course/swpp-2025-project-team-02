package com.example.fortuna_android.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

typealias ApiResponse<T> = Response<ServerResponse<T>>

interface ApiService {
    @POST("api/user/auth/google/")
    suspend fun loginWithGoogle(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/user/profile/")
    suspend fun getProfile(): Response<User>

    @GET("api/user/profile/")
    suspend fun getUserProfile(): Response<UserProfile>

    @PATCH("api/user/profile/")
    suspend fun updateUserProfile(
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @POST("api/user/auth/logout/")
    suspend fun logout(@Body request: LogoutRequest): Response<LogoutResponse>

    @GET("api/core/chakra/images/")
    suspend fun getImages(
        @Query("date") date: String
    ): Response<ImageResponse>

    @GET("api/core/chakra/upload-url/")
    suspend fun getImageUploadUrl(): ApiResponse<UploadUrlData>

    @Multipart
    @POST("api/core/chakra/upload/") // Replace with your actual upload endpoint
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("chakra_type") chakraType: RequestBody
    ): Response<UploadResponse>

    @GET("api/core/fortune/tomorrow/")
    suspend fun getFortune(
        @Query("date") date: String
    ): Response<FortuneResponse>

    @GET("api/fortune/today")
    suspend fun getTodayFortune(): Response<TodayFortuneResponse>

    @POST("api/user/auth/refresh/")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @DELETE("api/user/delete/")
    suspend fun deleteAccount(): Response<Unit>

    @GET("api/core/chakra/needed-element/")
    suspend fun getNeededElement(
        @Query("date") date: String? = null
    ): Response<NeededElementResponse>

    @POST("api/chakra/collect/")
    suspend fun collectElement(
        @Body request: CollectElementRequest
    ): Response<CollectElementResponse>

    @GET("api/chakra/collection-status/")
    suspend fun getCollectionStatus(): Response<CollectionStatusResponse>

    @GET("api/core/chakras/today-progress/")
    suspend fun getTodayProgress(): Response<TodayProgressResponse>
}