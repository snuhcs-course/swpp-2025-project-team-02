package com.example.fortuna_android.core

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.api.ApiService
import com.example.fortuna_android.api.RetrofitClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

/**
 * Smoke tests for MainActivity aligned with current behavior.
 * We avoid forcing runtime permission grants on device and mock network calls.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Mock Retrofit to prevent real network
        mockkObject(RetrofitClient)
        val mockService = mockk<ApiService>(relaxed = true)
        every { RetrofitClient.instance } returns mockService
        // Stub profile/token endpoints to succeed by default
        coEvery { mockService.getUserProfile() } returns Response.success(null)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun launchMainActivity_showsNavHost() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<androidx.fragment.app.FragmentContainerView>(com.example.fortuna_android.R.id.nav_host_fragment))
        }
    }

    @Test
    fun launchMainActivity_handlesPermissionsGracefully() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // If we reach here without SecurityException, permission flow is non-crashing.
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }
}
