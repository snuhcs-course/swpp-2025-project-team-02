package com.example.fortuna_android.service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class S3UploadService {
    private val client = OkHttpClient()

    /**
     * Upload image to S3 using presigned URL
     * @param presignedUrl The presigned URL from server
     * @param imageFile The image file to upload
     * @return Boolean indicating success or failure
     */
    suspend fun uploadImageToS3(presignedUrl: String, imageFile: File): Result<Unit> {
        return try {
            val mediaType = when (imageFile.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }.toMediaType()

            val requestBody = imageFile.readBytes().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(presignedUrl)
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Upload failed with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload image bytes to S3 using presigned URL
     * @param presignedUrl The presigned URL from server
     * @param imageBytes The image bytes to upload
     * @param contentType The content type (e.g., "image/jpeg", "image/png", "image/webp")
     * @return Boolean indicating success or failure
     */
    suspend fun uploadImageToS3(
        presignedUrl: String,
        imageBytes: ByteArray,
        contentType: String = "image/jpeg"
    ): Result<Unit> {
        return try {
            val mediaType = contentType.toMediaType()
            val requestBody = imageBytes.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(presignedUrl)
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Upload failed with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}