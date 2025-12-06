package com.example.fortuna_android

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.api.ApiService
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.AuthContainerActivity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MainActivityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        mockkObject(RetrofitClient)
    }

    @After
    fun tearDown() {
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

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val shadowActivity = Shadows.shadowOf(controller.get())
        val nextIntent = shadowActivity.nextStartedActivity

        assertEquals(AuthContainerActivity::class.java.name, nextIntent?.component?.className)
        assertTrue("MainActivity should finish after navigation", controller.get().isFinishing)
    }

    @Test
    fun withValidTokens_andCompleteProfile_staysInActivity() {
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

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val shadowActivity = Shadows.shadowOf(controller.get())
        val nextIntent = shadowActivity.nextStartedActivity

        // No navigation to AuthContainerActivity expected
        if (nextIntent != null) {
            assertFalse(
                "Should not navigate away when profile is complete",
                nextIntent.component?.className == AuthContainerActivity::class.java.name
            )
        }
    }
}
