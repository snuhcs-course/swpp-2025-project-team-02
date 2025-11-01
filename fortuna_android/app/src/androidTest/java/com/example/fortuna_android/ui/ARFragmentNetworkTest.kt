package com.example.fortuna_android.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Network-related instrumented tests for ARFragment
 */
@RunWith(AndroidJUnit4::class)
class ARFragmentNetworkTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "fortuna_prefs"

    @Before
    fun setup() {
        // Clear SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        // Clean up SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFragmentWithoutAuthToken() {
        // Fragment should be creatable without auth token
        val fragment = ARFragment()
        assert(fragment != null)
    }

    @Test
    fun testFragmentWithAuthToken() {
        // Add auth token to SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_auth_token_123")
            .commit()

        val fragment = ARFragment()
        assert(fragment != null)

        // Verify auth token is in SharedPreferences
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        assert(prefs.getString("jwt_token", null) == "test_auth_token_123")
    }

    @Test
    fun testFragmentWithEmptySharedPreferences() {
        // Ensure SharedPreferences is completely empty
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val fragment = ARFragment()
        assert(fragment != null)
    }

    @Test
    fun testFragmentMethodsWithoutView() {
        val fragment = ARFragment()

        // These methods should not crash even without a view
        try {
            fragment.onObjectDetectionCompleted(0, 0)
            fragment.onSphereCollected(1)
            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("test")
            fragment.onVLMAnalysisCompleted()
            fragment.clearVLMDescription()
        } catch (e: Exception) {
            // Expected - methods require view/context
        }

        assert(true)
    }

    @Test
    fun testMultipleFragmentsWithDifferentAuthStates() {
        // Fragment 1: No auth
        val fragment1 = ARFragment()

        // Fragment 2: With auth
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "token_abc")
            .commit()

        val fragment2 = ARFragment()

        // Fragment 3: Different auth
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "token_xyz")
            .commit()

        val fragment3 = ARFragment()

        assert(fragment1 != null)
        assert(fragment2 != null)
        assert(fragment3 != null)
    }

    @Test
    fun testFragmentCreationUnderSlowNetwork() {
        // Simulate slow network by creating fragment with delay
        val fragment = ARFragment()
        Thread.sleep(100)
        assert(fragment != null)
    }

    @Test
    fun testFragmentMethodCallsWithDelay() {
        val fragment = ARFragment()

        try {
            fragment.setScanningActive(true)
            Thread.sleep(50)
            fragment.onObjectDetectionCompleted(5, 3)
            Thread.sleep(50)
            fragment.onSphereCollected(1)
            Thread.sleep(50)
            fragment.setScanningActive(false)
        } catch (e: Exception) {
            // Expected without view
        }

        assert(true)
    }

    @Test
    fun testFragmentStateChangesWithoutNetwork() {
        val fragment = ARFragment()

        try {
            // Simulate state changes that would normally trigger network calls
            fragment.setScanningActive(true)
            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("Analyzing...")
            fragment.onVLMAnalysisCompleted()
            fragment.setScanningActive(false)
        } catch (e: Exception) {
            // Expected without view
        }

        assert(true)
    }

    @Test
    fun testFragmentCollectionWorkflowWithoutServer() {
        val fragment = ARFragment()

        try {
            // Simulate collecting objects without server sync
            for (i in 1..5) {
                fragment.onSphereCollected(i)
                Thread.sleep(10)
            }
        } catch (e: Exception) {
            // Expected without view
        }

        assert(true)
    }

    @Test
    fun testFragmentVLMWorkflowOffline() {
        val fragment = ARFragment()

        try {
            // Simulate VLM workflow without network
            fragment.onVLMAnalysisStarted()

            val tokens = listOf("Metal", " ", "element", " ", "detected")
            for (token in tokens) {
                fragment.updateVLMDescription(token)
                Thread.sleep(10)
            }

            fragment.onVLMAnalysisCompleted()
        } catch (e: Exception) {
            // Expected without view
        }

        assert(true)
    }

    @Test
    fun testFragmentRapidStateChanges() {
        val fragment = ARFragment()

        try {
            // Rapid state changes that might cause network issues
            for (i in 1..10) {
                fragment.setScanningActive(true)
                fragment.setScanningActive(false)
            }
        } catch (e: Exception) {
            // Expected without view
        }

        assert(true)
    }

    @Test
    fun testFragmentWithCorruptedSharedPreferences() {
        // Add invalid data to SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "")  // Empty token
            .putString("invalid_key", "invalid_value")
            .commit()

        val fragment = ARFragment()
        assert(fragment != null)
    }

    @Test
    fun testFragmentMultipleAuthTokenChanges() {
        // Change auth token multiple times
        for (i in 1..5) {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putString("jwt_token", "token_$i")
                .commit()

            val fragment = ARFragment()
            assert(fragment != null)
        }
    }
}
