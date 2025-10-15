package com.example.fortuna_android.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Create a separate Retrofit instance for refresh token calls to avoid circular dependency
    private val refreshRetrofit: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(RetrofitClient.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip token handling for certain endpoints
        if (shouldSkipTokenHandling(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        // Get current access token
        val accessToken = prefs.getString(KEY_TOKEN, null)

        // If no token is available, proceed without authentication
        // This handles the case before login or when user is not authenticated
        if (accessToken.isNullOrEmpty()) {
            Log.d(TAG, "No access token available, proceeding without authentication")
            return chain.proceed(originalRequest)
        }

        // Add Authorization header to the request
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        // Execute the request
        val response = chain.proceed(authenticatedRequest)

        // If we get a 401 Unauthorized, try to refresh the token
        if (response.code == 401) {
            Log.d(TAG, "Received 401, attempting token refresh")
            response.close() // Close the original response

            val refreshToken = prefs.getString(REFRESH_TOKEN, null)
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "No refresh token available for token refresh")
                return chain.proceed(originalRequest)
            }

            // Try to refresh the token
            val newAccessToken = refreshAccessToken(refreshToken)

            if (newAccessToken != null) {
                Log.d(TAG, "Token refresh successful")
                // Retry the original request with the new token
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                return chain.proceed(newRequest)
            } else {
                Log.e(TAG, "Token refresh failed")
                // Clear invalid tokens
                clearTokens()
                return chain.proceed(originalRequest)
            }
        }

        return response
    }

    private fun shouldSkipTokenHandling(request: Request): Boolean {
        val url = request.url.toString()
        return url.contains("/auth/google/") ||
               url.contains("/auth/refresh/") ||
               url.contains("/auth/logout/") ||
               url.contains("/chakra/upload-url/")
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            runBlocking {
                val refreshRequest = RefreshTokenRequest(refresh = refreshToken)
                val response = refreshRetrofit.refreshToken(refreshRequest)

                if (response.isSuccessful) {
                    val newAccessToken = response.body()?.access
                    if (newAccessToken != null) {
                        // Save the new access token
                        prefs.edit().putString(KEY_TOKEN, newAccessToken).apply()
                        Log.d(TAG, "New access token saved")
                        newAccessToken
                    } else {
                        Log.e(TAG, "Refresh response body is null")
                        null
                    }
                } else {
                    Log.e(TAG, "Token refresh failed with code: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during token refresh", e)
            null
        }
    }

    private fun clearTokens() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(REFRESH_TOKEN)
            .apply()
        Log.d(TAG, "Tokens cleared due to refresh failure")
    }
}