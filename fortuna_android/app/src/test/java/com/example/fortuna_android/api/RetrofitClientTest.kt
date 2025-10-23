package com.example.fortuna_android.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.BuildConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RetrofitClient
 * Tests singleton initialization, configuration, and API service creation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetrofitClientTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset authInterceptor using reflection
        resetAuthInterceptor()
    }

    /**
     * Reset RetrofitClient's authInterceptor using reflection
     */
    private fun resetAuthInterceptor() {
        try {
            val authInterceptorField = RetrofitClient::class.java.getDeclaredField("authInterceptor")
            authInterceptorField.isAccessible = true
            authInterceptorField.set(RetrofitClient, null)
        } catch (e: Exception) {
            println("Warning: Could not reset authInterceptor: ${e.message}")
        }
    }

    @Test
    fun `test getBaseUrl returns correct base URL`() {
        val baseUrl = RetrofitClient.getBaseUrl()

        assertEquals(BuildConfig.API_BASE_URL, baseUrl)
        assertNotNull(baseUrl)
    }

    @Test
    fun `test getBaseUrl returns non-empty string`() {
        val baseUrl = RetrofitClient.getBaseUrl()

        assertTrue(baseUrl.isNotEmpty())
    }

    @Test
    fun `test initialize sets authInterceptor`() {
        // Initialize with context
        RetrofitClient.initialize(context)

        // Access private authInterceptor field using reflection
        val authInterceptorField = RetrofitClient::class.java.getDeclaredField("authInterceptor")
        authInterceptorField.isAccessible = true
        val authInterceptor = authInterceptorField.get(RetrofitClient)

        assertNotNull("AuthInterceptor should be initialized", authInterceptor)
        assertTrue("AuthInterceptor should be instance of AuthInterceptor",
            authInterceptor is AuthInterceptor)
    }

    @Test
    fun `test initialize uses application context`() {
        val mockContext = ApplicationProvider.getApplicationContext<Context>()

        RetrofitClient.initialize(mockContext)

        // Verify authInterceptor was set
        val authInterceptorField = RetrofitClient::class.java.getDeclaredField("authInterceptor")
        authInterceptorField.isAccessible = true
        val authInterceptor = authInterceptorField.get(RetrofitClient)

        assertNotNull(authInterceptor)
    }

    @Test
    fun `test authInterceptor is null before initialization`() {
        // Access private authInterceptor field
        val authInterceptorField = RetrofitClient::class.java.getDeclaredField("authInterceptor")
        authInterceptorField.isAccessible = true
        val authInterceptor = authInterceptorField.get(RetrofitClient)

        assertNull("AuthInterceptor should be null before initialization", authInterceptor)
    }

    @Test
    fun `test logging interceptor field exists`() {
        // Test that loggingInterceptor is properly defined
        val loggingInterceptorField = RetrofitClient::class.java.getDeclaredField("loggingInterceptor")
        loggingInterceptorField.isAccessible = true
        val loggingInterceptor = loggingInterceptorField.get(RetrofitClient)

        assertNotNull("Logging interceptor should exist", loggingInterceptor)
    }

    @Test
    fun `test instance creates ApiService`() {
        RetrofitClient.initialize(context)

        val apiService = RetrofitClient.instance

        assertNotNull("ApiService instance should not be null", apiService)
        assertTrue("Instance should be ApiService", apiService is ApiService)
    }

    @Test
    fun `test instance is singleton`() {
        RetrofitClient.initialize(context)

        val instance1 = RetrofitClient.instance
        val instance2 = RetrofitClient.instance

        assertSame("Instance should be the same object (singleton)", instance1, instance2)
    }

    @Test
    fun `test BASE_URL constant is accessible`() {
        val baseUrlField = RetrofitClient::class.java.getDeclaredField("BASE_URL")
        baseUrlField.isAccessible = true
        val baseUrl = baseUrlField.get(RetrofitClient) as String

        assertNotNull(baseUrl)
        assertEquals(BuildConfig.API_BASE_URL, baseUrl)
    }

    @Test
    fun `test RetrofitClient is object singleton`() {
        // Verify RetrofitClient is a Kotlin object (singleton)
        val objectInstance = RetrofitClient::class.objectInstance

        assertNotNull("RetrofitClient should be a Kotlin object", objectInstance)
        assertSame("Object instance should be the same", RetrofitClient, objectInstance)
    }

    @Test
    fun `test multiple initialize calls use latest context`() {
        val context1 = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ApplicationProvider.getApplicationContext<Context>()

        RetrofitClient.initialize(context1)
        val authInterceptor1 = getAuthInterceptorViaReflection()

        RetrofitClient.initialize(context2)
        val authInterceptor2 = getAuthInterceptorViaReflection()

        // Second initialization should replace first
        assertNotNull(authInterceptor2)
        assertNotSame("Should create new AuthInterceptor on re-initialization",
            authInterceptor1, authInterceptor2)
    }

    @Test
    fun `test okHttpClient field exists`() {
        // Verify okHttpClient lazy delegate exists
        val okHttpClientField = RetrofitClient::class.java.getDeclaredField("okHttpClient\$delegate")
        okHttpClientField.isAccessible = true
        val delegate = okHttpClientField.get(RetrofitClient)

        assertNotNull("okHttpClient lazy delegate should exist", delegate)
    }

    @Test
    fun `test instance field exists`() {
        // Verify instance lazy delegate exists
        val instanceField = RetrofitClient::class.java.getDeclaredField("instance\$delegate")
        instanceField.isAccessible = true
        val delegate = instanceField.get(RetrofitClient)

        assertNotNull("instance lazy delegate should exist", delegate)
    }

    @Test
    fun `test instance creation does not throw exception`() {
        RetrofitClient.initialize(context)

        try {
            val apiService = RetrofitClient.instance
            assertNotNull(apiService)
        } catch (e: Exception) {
            fail("Instance creation should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `test getBaseUrl is consistent`() {
        val baseUrl1 = RetrofitClient.getBaseUrl()
        val baseUrl2 = RetrofitClient.getBaseUrl()

        assertEquals("getBaseUrl should return consistent values", baseUrl1, baseUrl2)
    }

    @Test
    fun `test initialize with application context`() {
        val appContext = context.applicationContext

        RetrofitClient.initialize(appContext)

        val authInterceptor = getAuthInterceptorViaReflection()
        assertNotNull("AuthInterceptor should be initialized with application context",
            authInterceptor)
    }

    @Test
    fun `test RetrofitClient object is accessible`() {
        assertNotNull("RetrofitClient object should be accessible", RetrofitClient)
    }

    @Test
    fun `test constants are properly defined`() {
        // Test that BASE_URL field exists and is accessible
        val baseUrlField = RetrofitClient::class.java.getDeclaredField("BASE_URL")
        baseUrlField.isAccessible = true

        assertNotNull("BASE_URL field should exist", baseUrlField)
        assertEquals("BASE_URL should be String type", String::class.java, baseUrlField.type)
    }

    /**
     * Helper method to get authInterceptor via reflection
     */
    private fun getAuthInterceptorViaReflection(): AuthInterceptor? {
        val authInterceptorField = RetrofitClient::class.java.getDeclaredField("authInterceptor")
        authInterceptorField.isAccessible = true
        return authInterceptorField.get(RetrofitClient) as? AuthInterceptor
    }
}
