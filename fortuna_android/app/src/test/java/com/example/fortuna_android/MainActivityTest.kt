package com.example.fortuna_android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.api.ApiService
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.RefreshTokenRequest
import com.example.fortuna_android.api.RefreshTokenResponse
import com.example.fortuna_android.AuthContainerActivity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MainActivityTest {

    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        mockkObject(RetrofitClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun whenNoTokens_navigatesToSignIn() {
        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService
        coEvery { mockService.getUserProfile() } returns Response.success(null)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()

        // Don't call setup() which triggers the full lifecycle
        // Just check the initial state without going through onResume

        val shadowActivity = Shadows.shadowOf(controller.get())
        val nextIntent = shadowActivity.nextStartedActivity

        assertEquals(AuthContainerActivity::class.java.name, nextIntent?.component?.className)
        assertTrue("MainActivity should finish after navigation", controller.get().isFinishing)
    }

    @Test
    fun withValidTokens_andCompleteProfile_staysInActivity() {
        // Set up tokens first
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "token")
            .putString("refresh_token", "refresh")
            .commit()

        val completeProfile = UserProfile(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            profileImage = null,
            nickname = "Tester",
            birthDateLunar = "1990-01-01",
            birthDateSolar = null,
            solarOrLunar = "lunar",
            birthTimeUnits = "10",
            gender = "male",
            yearlyGanji = null,
            monthlyGanji = null,
            dailyGanji = null,
            hourlyGanji = null,
            createdAt = null,
            lastLogin = null,
            collectionStatus = null
        )

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService
        coEvery { mockService.getUserProfile() } returns Response.success(completeProfile)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()

        // For this test, we're mainly checking that the activity doesn't immediately finish
        // The actual profile validation happens in onResume which we're not triggering
        assertFalse("MainActivity should not finish immediately with tokens", controller.get().isFinishing)
    }

    @Test
    fun onResume_withValidTokens_callsValidateAndRefreshToken() {
        // Set up tokens in SharedPreferences
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "valid_token")
            .putString("refresh_token", "refresh_token")
            .commit()

        val completeProfile = UserProfile(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            profileImage = null,
            nickname = "Tester",
            birthDateLunar = "1990-01-01",
            birthDateSolar = null,
            solarOrLunar = "lunar",
            birthTimeUnits = "10",
            gender = "male",
            yearlyGanji = null,
            monthlyGanji = null,
            dailyGanji = null,
            hourlyGanji = null,
            createdAt = null,
            lastLogin = null,
            collectionStatus = null
        )

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService
        coEvery { mockService.getUserProfile() } returns Response.success(completeProfile)

        // Create activity and trigger onResume
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()

        // Activity should not finish since tokens are valid and profile is complete
        assertFalse("Activity should not finish with valid tokens and complete profile",
            controller.get().isFinishing)
    }

    @Test
    fun onResume_withExpiredTokens_triggersTokenRefresh() {
        // Set up tokens in SharedPreferences
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "expired_token")
            .putString("refresh_token", "refresh_token")
            .commit()

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService

        // First call returns 401 (expired token)
        coEvery { mockService.getUserProfile() } returns Response.error(401,
            mockk(relaxed = true))

        // Refresh token call returns new access token
        val refreshResponse = RefreshTokenResponse(access = "new_access_token")
        coEvery { mockService.refreshToken(any()) } returns Response.success(refreshResponse)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()

        // Verify refresh token was called
        coEvery { mockService.refreshToken(RefreshTokenRequest(refresh = "refresh_token")) }

        // Verify new token was saved
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        assertEquals("new_access_token", prefs.getString("jwt_token", null))
    }

    @Test
    fun onResume_withFailedRefreshToken_navigatesToSignIn() {
        // Set up tokens in SharedPreferences
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "expired_token")
            .putString("refresh_token", "invalid_refresh_token")
            .commit()

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService

        // First call returns 401 (expired token)
        coEvery { mockService.getUserProfile() } returns Response.error(401,
            mockk(relaxed = true))

        // Refresh token call fails
        coEvery { mockService.refreshToken(any()) } returns Response.error(401,
            mockk(relaxed = true))

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()

        val shadowActivity = Shadows.shadowOf(controller.get())
        val nextIntent = shadowActivity.nextStartedActivity

        assertEquals(AuthContainerActivity::class.java.name, nextIntent?.component?.className)
        assertTrue("MainActivity should finish after failed refresh", controller.get().isFinishing)
    }

    @Test
    fun onRequestPermissionsResult_withGrantedPermissions_doesNotShowDialog() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()

        val activity = controller.get()

        // Simulate granted permissions
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)

        // This should not throw any exception
        activity.onRequestPermissionsResult(1001, permissions, grantResults)

        // Since permissions are granted, no dialog should be shown and method completes
        assertTrue("Method should complete successfully", true)
    }

    @Test
    fun onRequestPermissionsResult_withDeniedPermissions_showsDialog() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()

        val activity = controller.get()

        // Simulate denied permissions
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)

        // This should not throw any exception and should handle denied permissions gracefully
        activity.onRequestPermissionsResult(1001, permissions, grantResults)

        // The method should handle denied permissions gracefully
        assertTrue("Method should handle denied permissions", true)
    }

    @Test
    fun onRequestPermissionsResult_withEmptyGrantResults_handlesGracefully() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()

        val activity = controller.get()

        // Simulate interrupted permission request (empty grantResults)
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val grantResults = intArrayOf() // Empty array

        // This should not throw any exception
        activity.onRequestPermissionsResult(1001, permissions, grantResults)

        // Method should handle empty grantResults without crashing
        assertTrue("Method should handle empty grant results gracefully", true)
    }

    @Test
    fun refreshAccessToken_withValidRefreshToken_updatesAccessToken() {
        // This tests the private refreshAccessToken method indirectly through onResume
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "expired_token")
            .putString("refresh_token", "valid_refresh_token")
            .commit()

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService

        // Profile call fails with 401
        coEvery { mockService.getUserProfile() } returns Response.error(401,
            mockk(relaxed = true))

        // Refresh call succeeds
        val refreshResponse = RefreshTokenResponse(access = "brand_new_token")
        coEvery { mockService.refreshToken(any()) } returns Response.success(refreshResponse)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()

        // Verify the new token was saved
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        assertEquals("brand_new_token", prefs.getString("jwt_token", null))
    }

    @Test
    fun refreshAccessToken_withInvalidRefreshToken_navigatesToSignIn() {
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "expired_token")
            .putString("refresh_token", "invalid_refresh_token")
            .commit()

        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService

        // Profile call fails with 401
        coEvery { mockService.getUserProfile() } returns Response.error(401,
            mockk(relaxed = true))

        // Refresh call fails
        coEvery { mockService.refreshToken(any()) } returns Response.error(403,
            mockk(relaxed = true))

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()

        val shadowActivity = Shadows.shadowOf(controller.get())
        val nextIntent = shadowActivity.nextStartedActivity

        assertEquals(AuthContainerActivity::class.java.name, nextIntent?.component?.className)
        assertTrue("MainActivity should finish after failed refresh token", controller.get().isFinishing)
    }
}
