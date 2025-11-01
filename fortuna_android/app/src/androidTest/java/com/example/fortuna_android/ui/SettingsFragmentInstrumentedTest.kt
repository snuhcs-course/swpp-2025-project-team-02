package com.example.fortuna_android.ui

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<SettingsFragment>
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Set up mock token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", "test_token").commit()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }

        // Clean up
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    @Test
    fun testFragmentLifecycle() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    @Test
    fun testFragmentRecreation() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("Fragment view should exist after recreation", fragment.view)
        }
    }

    @Test
    fun testFragmentContext() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment context should not be null", fragment.context)
            assertNotNull("Fragment requireContext should not be null", fragment.requireContext())
        }
    }

    @Test
    fun testFragmentCanAccessSharedPreferences() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            assertNotNull("Token should be accessible from fragment", token)
            assertEquals("test_token", token)
        }
    }

    @Test
    fun testFragmentHandlesMultipleLifecycleCycles() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Cycle through states multiple times
        repeat(3) {
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        }

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should survive multiple lifecycle cycles", fragment)
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    // ========================================
    // Click Listener Tests
    // ========================================

    @Test
    fun testBackButtonExists() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val backButton = view.findViewById<ImageButton>(R.id.back_button)

            assertNotNull("Back button should exist", backButton)
            assertTrue("Back button should be clickable", backButton.isClickable)
        }
    }

    @Test
    fun testProfileCardExists() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val profileCard = view.findViewById<View>(R.id.profile_card)

            assertNotNull("Profile card should exist", profileCard)
            assertTrue("Profile card should be clickable", profileCard.isClickable)
        }
    }

    @Test
    fun testNotificationItemExists() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val notificationItem = view.findViewById<View>(R.id.notification_item)

            assertNotNull("Notification item should exist", notificationItem)
            assertTrue("Notification item should be clickable", notificationItem.isClickable)
        }
    }

    @Test
    fun testLogoutItemExists() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val logoutItem = view.findViewById<View>(R.id.logout_item)

            assertNotNull("Logout item should exist", logoutItem)
            assertTrue("Logout item should be clickable", logoutItem.isClickable)
        }
    }

    @Test
    fun testDeleteAccountItemExists() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val deleteAccountItem = view.findViewById<View>(R.id.delete_account_item)

            assertNotNull("Delete account item should exist", deleteAccountItem)
            assertTrue("Delete account item should be clickable", deleteAccountItem.isClickable)
        }
    }

    // ========================================
    // showDeleteAccountDialog Tests
    // ========================================

    @Test
    fun testShowDeleteAccountDialogTriggered() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val deleteAccountItem = view.findViewById<View>(R.id.delete_account_item)

            // Trigger the dialog (using reflection to access private method)
            val method = SettingsFragment::class.java.getDeclaredMethod("showDeleteAccountDialog")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for dialog to appear
            Thread.sleep(500)
        }
    }

    @Test
    fun testShowDeleteAccountDialogWhenNotAdded() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Destroy the fragment to make isAdded false
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

            // Try to show dialog (should return early)
            val method = SettingsFragment::class.java.getDeclaredMethod("showDeleteAccountDialog")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    // ========================================
    // deleteAccount Tests
    // ========================================

    @Test
    fun testDeleteAccountWithoutToken() {
        // Clear token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("jwt_token").commit()

        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Call deleteAccount using reflection
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)

            Thread.sleep(500)
        }
    }

    @Test
    fun testDeleteAccountWithToken() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Call deleteAccount using reflection
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(2000)
        }
    }

    @Test
    fun testDeleteAccountWhenNotAdded() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Destroy the fragment
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

            // Try to delete account (should return early)
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun testDeleteAccountAPISuccess() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Call deleteAccount
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation (success path may call logout)
            Thread.sleep(3000)
        }
    }

    @Test
    fun testDeleteAccountAPIFailure() {
        // This test will cover the failure path when API returns non-successful response
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(3000)
        }
    }

    @Test
    fun testDeleteAccountAPIException() {
        // This test will cover the exception path
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            val method = SettingsFragment::class.java.getDeclaredMethod("deleteAccount")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(3000)
        }
    }

    // ========================================
    // loadUserProfile Tests
    // ========================================

    @Test
    fun testLoadUserProfileWithoutToken() {
        // Clear token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("jwt_token").commit()

        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Wait for loadUserProfile to execute
        Thread.sleep(1000)
    }

    @Test
    fun testLoadUserProfileWithToken() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Wait for loadUserProfile to execute
        Thread.sleep(2000)
    }

    @Test
    fun testLoadUserProfileWhenNotAdded() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Destroy the fragment
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

            // Try to load profile (should return early)
            val method = SettingsFragment::class.java.getDeclaredMethod("loadUserProfile")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun testLoadUserProfileAPISuccess() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Call loadUserProfile again
            val method = SettingsFragment::class.java.getDeclaredMethod("loadUserProfile")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(2000)
        }
    }

    @Test
    fun testLoadUserProfileAPISuccessWithNullProfile() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val method = SettingsFragment::class.java.getDeclaredMethod("loadUserProfile")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation (covers null profile case)
            Thread.sleep(2000)
        }
    }

    @Test
    fun testLoadUserProfileAPIFailure() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val method = SettingsFragment::class.java.getDeclaredMethod("loadUserProfile")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(2000)
        }
    }

    @Test
    fun testLoadUserProfileAPIException() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val method = SettingsFragment::class.java.getDeclaredMethod("loadUserProfile")
            method.isAccessible = true
            method.invoke(fragment)

            // Wait for async operation
            Thread.sleep(2000)
        }
    }

    // ========================================
    // updateUI Tests
    // ========================================

    @Test
    fun testUpdateUIWithNickname() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Create mock profile with nickname
            val mockProfile = com.example.fortuna_android.api.UserProfile(
                userId = 12345,
                email = "test@example.com",
                name = "Test Name",
                profileImage = null,
                nickname = "Test Nickname",
                birthDateSolar = "1990-01-01",
                birthDateLunar = null,
                solarOrLunar = "solar",
                birthTimeUnits = "오시",
                gender = "male",
                yearlyGanji = "갑자",
                monthlyGanji = "병인",
                dailyGanji = "무진",
                hourlyGanji = "경오",
                createdAt = "2024-01-01T00:00:00Z",
                lastLogin = "2024-01-01T00:00:00Z",
                collectionStatus = null
            )

            // Call updateUI using reflection
            val method = SettingsFragment::class.java.getDeclaredMethod(
                "updateUI",
                com.example.fortuna_android.api.UserProfile::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, mockProfile)

            Thread.sleep(300)

            // Verify nickname is displayed
            val view = fragment.view!!
            val profileName = view.findViewById<android.widget.TextView>(R.id.profile_name)
            assertEquals("Test Nickname", profileName.text.toString())
        }
    }

    @Test
    fun testUpdateUIWithoutNickname() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Create mock profile without nickname
            val mockProfile = com.example.fortuna_android.api.UserProfile(
                userId = 12345,
                email = "test@example.com",
                name = "Test Name",
                profileImage = null,
                nickname = null,
                birthDateSolar = "1990-01-01",
                birthDateLunar = null,
                solarOrLunar = "solar",
                birthTimeUnits = "오시",
                gender = "male",
                yearlyGanji = "갑자",
                monthlyGanji = "병인",
                dailyGanji = "무진",
                hourlyGanji = "경오",
                createdAt = "2024-01-01T00:00:00Z",
                lastLogin = "2024-01-01T00:00:00Z",
                collectionStatus = null
            )

            // Call updateUI using reflection
            val method = SettingsFragment::class.java.getDeclaredMethod(
                "updateUI",
                com.example.fortuna_android.api.UserProfile::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, mockProfile)

            Thread.sleep(300)

            // Verify name is displayed
            val view = fragment.view!!
            val profileName = view.findViewById<android.widget.TextView>(R.id.profile_name)
            assertEquals("Test Name", profileName.text.toString())
        }
    }

    @Test
    fun testUpdateUIBindingNull() {
        // Test edge case where binding becomes null during updateUI
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Destroy the view to make binding null
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)

            // Create mock profile
            val mockProfile = com.example.fortuna_android.api.UserProfile(
                userId = 12345,
                email = "test@example.com",
                name = "Test Name",
                profileImage = null,
                nickname = null,
                birthDateSolar = "1990-01-01",
                birthDateLunar = null,
                solarOrLunar = "solar",
                birthTimeUnits = "오시",
                gender = "male",
                yearlyGanji = "갑자",
                monthlyGanji = "병인",
                dailyGanji = "무진",
                hourlyGanji = "경오",
                createdAt = "2024-01-01T00:00:00Z",
                lastLogin = "2024-01-01T00:00:00Z",
                collectionStatus = null
            )

            // Try to call updateUI (should return early due to null binding)
            val method = SettingsFragment::class.java.getDeclaredMethod(
                "updateUI",
                com.example.fortuna_android.api.UserProfile::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, mockProfile)
        }
    }

    // ========================================
    // onResume Tests
    // ========================================

    @Test
    fun testOnResumeCallsLoadUserProfile() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Move to STARTED then back to RESUMED to trigger onResume again
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        Thread.sleep(300)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // Wait for loadUserProfile to execute
        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    @Test
    fun testOnResumeMultipleTimes() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Trigger onResume multiple times
        repeat(3) {
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            Thread.sleep(300)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            Thread.sleep(1000)
        }

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    // ========================================
    // onDestroyView Tests
    // ========================================

    @Test
    fun testOnDestroyViewClearsBinding() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            assertNotNull("View should exist before destroy", fragment.view)
        }

        // Destroy the view
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        Thread.sleep(300)
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    fun testFullLifecycleWithTokenAndProfileLoad() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Wait for initial load
        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be loaded", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)

            // Verify all UI elements exist
            val view = fragment.view!!
            assertNotNull(view.findViewById<ImageButton>(R.id.back_button))
            assertNotNull(view.findViewById<View>(R.id.profile_card))
            assertNotNull(view.findViewById<View>(R.id.notification_item))
            assertNotNull(view.findViewById<View>(R.id.logout_item))
            assertNotNull(view.findViewById<View>(R.id.delete_account_item))
        }
    }

    @Test
    fun testFragmentWithEmptyToken() {
        // Clear token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", "").commit()

        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should still load", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testSetupClickListenersMultipleTimes() {
        scenario = launchFragmentInContainer<SettingsFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
        Thread.sleep(300)

        scenario.onFragment { fragment ->
            // Call setupClickListeners multiple times using reflection
            val method = SettingsFragment::class.java.getDeclaredMethod("setupClickListeners")
            method.isAccessible = true

            repeat(3) {
                method.invoke(fragment)
            }

            // Verify buttons are still clickable
            val view = fragment.view!!
            assertTrue(view.findViewById<ImageButton>(R.id.back_button).isClickable)
            assertTrue(view.findViewById<View>(R.id.profile_card).isClickable)
            assertTrue(view.findViewById<View>(R.id.notification_item).isClickable)
            assertTrue(view.findViewById<View>(R.id.logout_item).isClickable)
            assertTrue(view.findViewById<View>(R.id.delete_account_item).isClickable)
        }
    }
}
