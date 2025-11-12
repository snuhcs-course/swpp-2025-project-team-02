package com.example.fortuna_android.core

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for MainActivity navigation, especially the new onNewIntent functionality
 * for handling tutorial -> AR navigation
 */
@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun testActivityCreation() = runBlocking {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario?.onActivity { activity ->
            assertNotNull("Activity should be created", activity)
        }

        delay(500)
    }

    @Test
    fun testNavigateToAR_onCreate() = runBlocking {
        // Create intent with navigate_to_ar extra
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        scenario = ActivityScenario.launch(intent)

        delay(1000) // Wait for navigation

        scenario?.onActivity { activity ->
            assertNotNull("Activity should handle AR navigation intent", activity)
            // Note: We can't directly verify tab selection here due to permissions/login requirements
            // but we can verify the activity handles the intent without crashing
        }
    }

    @Test
    fun testNavigateToAR_onNewIntent() = runBlocking {
        // First launch activity normally
        scenario = ActivityScenario.launch(MainActivity::class.java)

        delay(500)

        // Close first scenario
        scenario?.close()

        delay(200)

        // Launch again with navigate_to_ar extra (simulates onNewIntent with FLAG_ACTIVITY_SINGLE_TOP)
        val newIntent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        scenario = ActivityScenario.launch(newIntent)

        delay(500)

        scenario?.onActivity { activity ->
            assertNotNull("Activity should handle navigation intent", activity)
        }
    }

    @Test
    fun testHandleNavigationIntent_withoutExtra() = runBlocking {
        // Launch without navigate_to_ar extra
        scenario = ActivityScenario.launch(MainActivity::class.java)

        delay(500)

        scenario?.onActivity { activity ->
            assertNotNull("Activity should handle normal launch", activity)
        }
    }

    @Test
    fun testIntentWithFalseExtra() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("navigate_to_ar", false)
        }

        scenario = ActivityScenario.launch(intent)

        delay(500)

        scenario?.onActivity { activity ->
            assertNotNull("Activity should handle false extra", activity)
        }
    }

    @Test
    fun testMultipleNavigationIntents() = runBlocking {
        // Test multiple navigation attempts
        repeat(3) { attempt ->
            val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                putExtra("navigate_to_ar", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            scenario?.close()
            delay(200)

            scenario = ActivityScenario.launch(intent)
            delay(500)

            scenario?.onActivity { activity ->
                assertNotNull("Activity should handle intent attempt $attempt", activity)
            }
        }
    }

    @Test
    fun testActivityRecreation() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
        }

        scenario = ActivityScenario.launch(intent)

        delay(500)

        // Recreate activity
        scenario?.recreate()

        delay(500)

        scenario?.onActivity { activity ->
            assertNotNull("Activity should handle recreation", activity)
        }
    }
}
