package com.example.fortuna_android.service

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Unit tests for S3UploadService
 * Tests S3 file upload functionality with 100% coverage
 */
class S3UploadServiceTest {

    private lateinit var service: S3UploadService
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        service = S3UploadService()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create a temporary test file
        tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(ByteArray(100) { it.toByte() })
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    // ========== uploadImageToS3(File) Tests ==========

    @Test
    fun `test uploadImageToS3 with File succeeds with 200 response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, tempFile)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)
        assertEquals("Should return Unit", Unit, result.getOrNull())

        // Verify request
        val request = mockWebServer.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/upload", request.path)
        assertEquals("image/jpeg", request.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with File succeeds with 204 response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, tempFile)

        // Assert
        assertTrue("Upload should succeed with 204", result.isSuccess)
    }

    @Test
    fun `test uploadImageToS3 with File fails with 400 response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, tempFile)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Should have exception", exception)
        assertTrue("Should be IOException", exception is IOException)
        assertTrue("Should contain error code", exception!!.message!!.contains("400"))
    }

    @Test
    fun `test uploadImageToS3 with File fails with 500 response`() = runTest {
        // Arrange
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, tempFile)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Should contain error code 500", exception!!.message!!.contains("500"))
    }

    @Test
    fun `test uploadImageToS3 with File handles network error`() = runTest {
        // Arrange - Server is started but we'll use invalid URL
        val invalidUrl = "http://localhost:99999/upload"

        // Act
        val result = service.uploadImageToS3(invalidUrl, tempFile)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        assertNotNull("Should have exception", result.exceptionOrNull())
    }

    @Test
    fun `test uploadImageToS3 with JPEG file extension`() = runTest {
        // Arrange
        val jpegFile = File.createTempFile("test", ".jpeg")
        jpegFile.writeBytes(ByteArray(50))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, jpegFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("image/jpeg", request.getHeader("Content-Type"))
        } finally {
            jpegFile.delete()
        }
    }

    @Test
    fun `test uploadImageToS3 with PNG file extension`() = runTest {
        // Arrange
        val pngFile = File.createTempFile("test", ".png")
        pngFile.writeBytes(ByteArray(50))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, pngFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("image/png", request.getHeader("Content-Type"))
        } finally {
            pngFile.delete()
        }
    }

    @Test
    fun `test uploadImageToS3 with PNG file extension uppercase`() = runTest {
        // Arrange
        val pngFile = File.createTempFile("test", ".PNG")
        pngFile.writeBytes(ByteArray(50))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, pngFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("image/png", request.getHeader("Content-Type"))
        } finally {
            pngFile.delete()
        }
    }

    @Test
    fun `test uploadImageToS3 with WEBP file extension`() = runTest {
        // Arrange
        val webpFile = File.createTempFile("test", ".webp")
        webpFile.writeBytes(ByteArray(50))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, webpFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("image/webp", request.getHeader("Content-Type"))
        } finally {
            webpFile.delete()
        }
    }

    @Test
    fun `test uploadImageToS3 with unknown file extension`() = runTest {
        // Arrange
        val unknownFile = File.createTempFile("test", ".xyz")
        unknownFile.writeBytes(ByteArray(50))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, unknownFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("application/octet-stream", request.getHeader("Content-Type"))
        } finally {
            unknownFile.delete()
        }
    }

    @Test
    fun `test uploadImageToS3 with File uploads correct content`() = runTest {
        // Arrange
        val testContent = "Test image content".toByteArray()
        tempFile.writeBytes(testContent)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, tempFile)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertArrayEquals("Content should match", testContent, request.body.readByteArray())
    }

    // ========== uploadImageToS3(ByteArray) Tests ==========

    @Test
    fun `test uploadImageToS3 with ByteArray succeeds with 200 response`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100) { it.toByte() }
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)
        assertEquals("Should return Unit", Unit, result.getOrNull())

        // Verify request
        val request = mockWebServer.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("image/jpeg", request.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray succeeds with 204 response`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(204))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should succeed with 204", result.isSuccess)
    }

    @Test
    fun `test uploadImageToS3 with ByteArray and custom content type`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes, "image/png")

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertEquals("image/png", request.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray and webp content type`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes, "image/webp")

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertEquals("image/webp", request.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray uses default content type`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act - Not specifying content type, should use default
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertEquals("image/jpeg", request.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray fails with 400 response`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(400))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Should have exception", exception)
        assertTrue("Should be IOException", exception is IOException)
        assertTrue("Should contain error code", exception!!.message!!.contains("400"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray fails with 403 response`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(403))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        assertTrue("Should contain error code 403",
            result.exceptionOrNull()!!.message!!.contains("403"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray fails with 500 response`() = runTest {
        // Arrange
        val imageBytes = ByteArray(100)
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        assertTrue("Should contain error code 500",
            result.exceptionOrNull()!!.message!!.contains("500"))
    }

    @Test
    fun `test uploadImageToS3 with ByteArray handles network error`() = runTest {
        // Arrange - Invalid URL
        val imageBytes = ByteArray(100)
        val invalidUrl = "http://localhost:99999/upload"

        // Act
        val result = service.uploadImageToS3(invalidUrl, imageBytes)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        assertNotNull("Should have exception", result.exceptionOrNull())
    }

    @Test
    fun `test uploadImageToS3 with ByteArray uploads correct content`() = runTest {
        // Arrange
        val testContent = "Test image bytes".toByteArray()
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, testContent)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertArrayEquals("Content should match", testContent, request.body.readByteArray())
    }

    @Test
    fun `test uploadImageToS3 with ByteArray uploads empty array`() = runTest {
        // Arrange
        val emptyBytes = ByteArray(0)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, emptyBytes)

        // Assert
        assertTrue("Upload should succeed", result.isSuccess)

        val request = mockWebServer.takeRequest()
        assertEquals("Content length should be 0", 0, request.body.size)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test uploadImageToS3 File and ByteArray produce same request for same content`() = runTest {
        // Arrange
        val content = "Same content".toByteArray()
        tempFile.writeBytes(content)

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val resultFile = service.uploadImageToS3(url, tempFile)
        val resultBytes = service.uploadImageToS3(url, content, "image/jpeg")

        // Assert
        assertTrue("Both should succeed", resultFile.isSuccess && resultBytes.isSuccess)

        val requestFile = mockWebServer.takeRequest()
        val requestBytes = mockWebServer.takeRequest()

        assertArrayEquals("Content should be same",
            requestFile.body.readByteArray(),
            requestBytes.body.readByteArray())
        assertEquals("Content-Type should be same",
            requestFile.getHeader("Content-Type"),
            requestBytes.getHeader("Content-Type"))
    }

    @Test
    fun `test uploadImageToS3 with File handles non-existent file`() = runTest {
        // Arrange
        val nonExistentFile = File("/non/existent/path/image.jpg")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result = service.uploadImageToS3(url, nonExistentFile)

        // Assert
        assertTrue("Upload should fail", result.isFailure)
        assertNotNull("Should have exception", result.exceptionOrNull())
    }

    @Test
    fun `test multiple consecutive uploads succeed`() = runTest {
        // Arrange
        val imageBytes = ByteArray(50) { it.toByte() }
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        // Act
        val result1 = service.uploadImageToS3(url, imageBytes)
        val result2 = service.uploadImageToS3(url, imageBytes)
        val result3 = service.uploadImageToS3(url, imageBytes)

        // Assert
        assertTrue("First upload should succeed", result1.isSuccess)
        assertTrue("Second upload should succeed", result2.isSuccess)
        assertTrue("Third upload should succeed", result3.isSuccess)
        assertEquals("Should have 3 requests", 3, mockWebServer.requestCount)
    }

    @Test
    fun `test uploadImageToS3 with large file`() = runTest {
        // Arrange - Create a larger file (1MB)
        val largeFile = File.createTempFile("large_test", ".jpg")
        largeFile.writeBytes(ByteArray(1024 * 1024) { (it % 256).toByte() })
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val url = mockWebServer.url("/upload").toString()

        try {
            // Act
            val result = service.uploadImageToS3(url, largeFile)

            // Assert
            assertTrue("Upload should succeed", result.isSuccess)

            val request = mockWebServer.takeRequest()
            assertEquals("Content size should match", 1024 * 1024, request.body.size)
        } finally {
            largeFile.delete()
        }
    }
}
