package com.example.fortuna_android.common.helpers

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Utility functions for image conversion
 */
object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Convert ARCore camera Image (YUV_420_888) to Bitmap
     *
     * @param image Camera image from ARCore (must be YUV_420_888 format)
     * @return Bitmap or null if conversion fails
     */
    fun convertYuvImageToBitmap(image: Image): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Image format is not YUV_420_888: ${image.format}")
            return null
        }

        try {
            // Get image dimensions
            val width = image.width
            val height = image.height

            // Get YUV planes
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            // Allocate NV21 byte array
            val nv21 = ByteArray(ySize + uSize + vSize)

            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)

            // Copy UV planes (interleave U and V)
            val pixelStride = planes[1].pixelStride
            if (pixelStride == 1) {
                // Planes are already in NV21 format
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)
            } else {
                // Need to interleave U and V manually
                val uvWidth = width / 2
                val uvHeight = height / 2

                for (row in 0 until uvHeight) {
                    for (col in 0 until uvWidth) {
                        val uvIndex = ySize + (row * uvWidth + col) * 2

                        // V first (NV21 format)
                        nv21[uvIndex] = vBuffer.get(row * planes[2].rowStride + col * pixelStride)
                        // U second
                        nv21[uvIndex + 1] = uBuffer.get(row * planes[1].rowStride + col * pixelStride)
                    }
                }
            }

            // Convert NV21 to JPEG
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
            val jpegBytes = out.toByteArray()

            // Decode JPEG to Bitmap
            return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert YUV image to Bitmap", e)
            return null
        }
    }

    /**
     * Optimize bitmap for VLM processing
     * Downscales to maxSize while maintaining aspect ratio
     *
     * @param bitmap Input bitmap
     * @param maxSize Maximum dimension (width or height)
     * @return Optimized bitmap (may be the same instance if already small enough)
     */
    fun optimizeImageForVLM(bitmap: Bitmap, maxSize: Int = 336): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // If already small enough, return as-is
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        // Calculate scale to fit within maxSize x maxSize
        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(TAG, "Optimizing image: ${width}x${height} → ${newWidth}x${newHeight} (scale: $scale)")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Crop and downscale object region for VLM classification
     *
     * Extracts the bounding box region from a full camera image and downscales to a square
     * for fast VLM inference. Uses 96x96 target size for optimal speed/quality balance.
     *
     * @param bitmap Full camera image
     * @param boundingBox Object bounding box from ML Kit detection
     * @param targetSize Target dimension for square output (default: 96x96 for speed)
     * @return Cropped and downscaled square bitmap
     */
    fun cropObjectForVLM(bitmap: Bitmap, boundingBox: android.graphics.Rect, targetSize: Int = 96): Bitmap {
        try {
            // Clamp bounding box to image bounds to prevent out-of-bounds errors
            val left = boundingBox.left.coerceIn(0, bitmap.width - 1)
            val top = boundingBox.top.coerceIn(0, bitmap.height - 1)
            val right = boundingBox.right.coerceIn(left + 1, bitmap.width)
            val bottom = boundingBox.bottom.coerceIn(top + 1, bitmap.height)

            val width = right - left
            val height = bottom - top

            Log.d(TAG, "Cropping object: bbox=[$left,$top,$right,$bottom] size=${width}x${height}")

            // Crop object from full image
            val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)

            // Downscale to square (VLM expects square input)
            val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)

            // Clean up intermediate bitmap if different from final
            if (scaled != cropped) {
                cropped.recycle()
            }

            Log.d(TAG, "Cropped object: ${width}x${height} → ${targetSize}x${targetSize}")
            return scaled

        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop object, using full image fallback", e)
            // Fallback: return downscaled full image
            return optimizeImageForVLM(bitmap, targetSize)
        }
    }
}
