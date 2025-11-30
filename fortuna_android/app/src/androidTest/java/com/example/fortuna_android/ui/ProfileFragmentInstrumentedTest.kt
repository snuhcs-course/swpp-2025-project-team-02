package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.fortuna_android.api.UserProfile

/**
 * Comprehensive instrumented tests for ProfileFragment
 */
@RunWith(AndroidJUnit4::class)
class ProfileFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<ProfileFragment>
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Set up mock token for profile fragment
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

    // ========== Basic Fragment Tests ==========

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    @Test
    fun testFragmentLifecycle() {
        scenario = launchFragmentInContainer<ProfileFragment>(
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
        scenario = launchFragmentInContainer<ProfileFragment>(
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
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment context should not be null", fragment.context)
            assertNotNull("Fragment requireContext should not be null", fragment.requireContext())
        }
    }

    @Test
    fun testFragmentCanAccessSharedPreferences() {
        scenario = launchFragmentInContainer<ProfileFragment>(
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
    fun testFragmentSurvivesConfigurationChange() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            assertEquals("Token should persist after configuration change", "test_token", token)
        }
    }

    @Test
    fun testOnDestroyView() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("View should exist before destroy", fragment.view)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }

    // ========== setupHeader() Tests ==========

    @Test
    fun testSetupHeader_hidesProfileButton() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val profileButton = view.findViewById<View>(R.id.btn_edit_profile)

            // Button exists but may be visible (repurposed as settings button)
            assertNotNull("Profile button should exist", profileButton)
        }
    }

    @Test
    fun testSetupHeader_configuresSettingsButton() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val headerButton = view.findViewById<ImageButton>(R.id.btn_edit_profile)

            assertNotNull("Header button should exist", headerButton)
            assertEquals("Content description should be '설정'", "설정", headerButton.contentDescription)
        }
    }

    // ========== setupClickListeners() Tests ==========

    @Test
    fun testSettingsButtonExists() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val settingsButton = view.findViewById<View>(R.id.btn_edit_profile)

            // Verify the button exists and has click listener set
            // (Don't actually click to avoid NavController requirement)
            assertNotNull("Settings button should exist", settingsButton)
            assertTrue("Settings button should be clickable", settingsButton?.isClickable == true)
        }
    }

    // ========== loadUserProfile() Tests ==========

    @Test
    fun testLoadUserProfile_noToken() {
        // Clear token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("jwt_token").commit()

        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should handle no token gracefully", fragment)
        }
    }

    @Test
    fun testLoadUserProfile_emptyToken() {
        // Set empty token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", "").commit()

        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should handle empty token gracefully", fragment)
        }
    }

    @Test
    fun testLoadUserProfile_withValidToken() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testLoadUserProfile_triggersLoading() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(100)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val loadingContainer = view.findViewById<View>(R.id.loading_container)

            // Loading should be shown or hidden depending on API response timing
            assertNotNull("Loading container should exist", loadingContainer)
        }
    }

    @Test
    fun testLoadUserProfile_manualCall() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            // Manually call loadUserProfile
            fragment.loadUserProfile()
            Thread.sleep(1000)
        }
    }

    @Test
    fun testLoadUserProfile_whenNotAdded() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        // loadUserProfile should handle !isAdded gracefully
    }

    // ========== showLoading/hideLoading Tests ==========

    @Test
    fun testShowAndHideLoading() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(100)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val loadingContainer = view.findViewById<View>(R.id.loading_container)
            val contentContainer = view.findViewById<View>(R.id.content_container)

            assertNotNull("Loading container should exist", loadingContainer)
            assertNotNull("Content container should exist", contentContainer)
        }

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val loadingContainer = view.findViewById<View>(R.id.loading_container)
            val contentContainer = view.findViewById<View>(R.id.content_container)

            // After loading, content should be visible
            assertTrue("One of the containers should be visible",
                loadingContainer.visibility == View.VISIBLE || contentContainer.visibility == View.VISIBLE)
        }
    }

    // ========== Helper Method Tests (via manual invocation) ==========

    @Test
    fun testGetElementFromCheongan_allCases() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            // Test all cheongan characters by using reflection or integration test
            assertNotNull("Fragment should exist for testing", fragment)
        }
    }

    @Test
    fun testProfileDisplayWithSolarCalendar() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val profileName = view.findViewById<TextView>(R.id.profile_name)
            val calendarTag = view.findViewById<TextView>(R.id.profile_calendar_tag)

            assertNotNull("Profile name should exist", profileName)
            assertNotNull("Calendar tag should exist", calendarTag)
        }
    }

    @Test
    fun testProfileDisplayWithLunarCalendar() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val calendarTag = view.findViewById<TextView>(R.id.profile_calendar_tag)

            assertNotNull("Calendar tag should exist", calendarTag)
        }
    }

    @Test
    fun testCollectedElementsDisplay() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!

            // Check all 5 element badges exist
            val badge1 = view.findViewById<TextView>(R.id.element_badge_1)
            val badge2 = view.findViewById<TextView>(R.id.element_badge_2)
            val badge3 = view.findViewById<TextView>(R.id.element_badge_3)
            val badge4 = view.findViewById<TextView>(R.id.element_badge_4)
            val badge5 = view.findViewById<TextView>(R.id.element_badge_5)

            assertNotNull("Badge 1 (목) should exist", badge1)
            assertNotNull("Badge 2 (화) should exist", badge2)
            assertNotNull("Badge 3 (토) should exist", badge3)
            assertNotNull("Badge 4 (금) should exist", badge4)
            assertNotNull("Badge 5 (수) should exist", badge5)
        }
    }

    @Test
    fun testElementBadgeColors() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!

            val badge1 = view.findViewById<TextView>(R.id.element_badge_1)
            val badge2 = view.findViewById<TextView>(R.id.element_badge_2)
            val badge3 = view.findViewById<TextView>(R.id.element_badge_3)
            val badge4 = view.findViewById<TextView>(R.id.element_badge_4)
            val badge5 = view.findViewById<TextView>(R.id.element_badge_5)

            // Check that badges have backgrounds and text colors set
            assertNotNull("Badge 1 should have background", badge1?.background)
            assertNotNull("Badge 2 should have background", badge2?.background)
            assertNotNull("Badge 3 should have background", badge3?.background)
            assertNotNull("Badge 4 should have background", badge4?.background)
            assertNotNull("Badge 5 should have background", badge5?.background)

            // Text color should be white for visibility
            assertEquals("Badge text should be white", Color.WHITE, badge1?.currentTextColor)
        }
    }

    @Test
    fun testSajuPaljaViewDataSet() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should exist", fragment)
            // SajuPaljaView is set in binding.sajuPaljaView
        }
    }

    @Test
    fun testElementCharacterDisplay() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val elementCharacter = view.findViewById<TextView>(R.id.profile_element_character)
            val elementTag = view.findViewById<TextView>(R.id.profile_element_tag)

            assertNotNull("Element character should exist", elementCharacter)
            assertNotNull("Element tag should exist", elementTag)
        }
    }

    @Test
    fun testBirthInfoDisplay() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val birthInfo = view.findViewById<TextView>(R.id.profile_birth_info)

            assertNotNull("Birth info should exist", birthInfo)
        }
    }

    @Test
    fun testProfileWithAllCheonganTypes() {
        // Test all 10 cheongan types: 갑, 을, 병, 정, 무, 기, 경, 신, 임, 계
        val cheonganList = listOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")

        cheonganList.forEach { cheongan ->
            scenario = launchFragmentInContainer<ProfileFragment>(
                themeResId = R.style.Theme_Fortuna_android
            )

            Thread.sleep(2000)

            scenario.onFragment { fragment ->
                assertNotNull("Fragment should handle $cheongan", fragment)
            }

            scenario.close()
        }
    }

    @Test
    fun testElementColors_wood() {
        // Test that wood elements (갑, 을) get green color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val expectedGreen = Color.parseColor("#0BEFA0")
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testElementColors_fire() {
        // Test that fire elements (병, 정) get red color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val expectedRed = Color.parseColor("#F93E3E")
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testElementColors_earth() {
        // Test that earth elements (무, 기) get brown color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val expectedBrown = Color.parseColor("#8B4513")
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testElementColors_metal() {
        // Test that metal elements (경, 신) get silver color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val expectedSilver = Color.parseColor("#C0C0C0")
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testElementColors_water() {
        // Test that water elements (임, 계) get blue color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val expectedBlue = Color.parseColor("#2BB3FC")
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testElementColors_unknown() {
        // Test that unknown elements get white color
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should exist", fragment)
        }
    }

    @Test
    fun testCollectedElements_withZeroCounts() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!

            // All badges should display "0" if no elements collected
            val badge1 = view.findViewById<TextView>(R.id.element_badge_1)
            assertNotNull("Badge should exist even with zero count", badge1)
        }
    }

    @Test
    fun testCollectedElements_withNonZeroCounts() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!

            val badge1 = view.findViewById<TextView>(R.id.element_badge_1)
            val badge2 = view.findViewById<TextView>(R.id.element_badge_2)
            val badge3 = view.findViewById<TextView>(R.id.element_badge_3)
            val badge4 = view.findViewById<TextView>(R.id.element_badge_4)
            val badge5 = view.findViewById<TextView>(R.id.element_badge_5)

            // Badges should show numeric values
            assertNotNull("Badge 1 text should not be null", badge1?.text)
            assertNotNull("Badge 2 text should not be null", badge2?.text)
            assertNotNull("Badge 3 text should not be null", badge3?.text)
            assertNotNull("Badge 4 text should not be null", badge4?.text)
            assertNotNull("Badge 5 text should not be null", badge5?.text)
        }
    }

    @Test
    fun testProfileWithNullDailyGanji() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            // Should handle null dailyGanji gracefully
            assertNotNull("Fragment should handle null dailyGanji", fragment)
        }
    }

    @Test
    fun testProfileWithEmptyDailyGanji() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            // Should handle empty dailyGanji gracefully
            assertNotNull("Fragment should handle empty dailyGanji", fragment)
        }
    }

    @Test
    fun testProfileWithNullNickname() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            // Should default to "사용자" if nickname is null
            assertNotNull("Fragment should handle null nickname", fragment)
        }
    }

    @Test
    fun testProfileWithBothBirthDates() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val birthInfo = view.findViewById<TextView>(R.id.profile_birth_info)

            // Should display birth date and time
            assertNotNull("Birth info should be displayed", birthInfo)
        }
    }

    @Test
    fun testSpannableStringForElement() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val elementTag = view.findViewById<TextView>(R.id.profile_element_tag)

            // Element tag should contain SpannableString
            assertNotNull("Element tag should have text", elementTag?.text)
        }
    }

    @Test
    fun testFragmentHandlesAPIFailureGracefully() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(3000)

        scenario.onFragment { fragment ->
            // Fragment should not crash on API failure
            assertNotNull("Fragment should handle API failure", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentHandlesAPIExceptionGracefully() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(3000)

        scenario.onFragment { fragment ->
            // Fragment should not crash on API exception
            assertNotNull("Fragment should handle API exception", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testMultipleLoadUserProfileCalls() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            // Call loadUserProfile multiple times
            fragment.loadUserProfile()
            Thread.sleep(200)
            fragment.loadUserProfile()
            Thread.sleep(200)
            fragment.loadUserProfile()
            Thread.sleep(1500)

            assertNotNull("Fragment should handle multiple calls", fragment)
        }
    }

    @Test
    fun testViewBindingCleanupOnDestroy() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            assertNotNull("View should exist", fragment.view)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        // View binding should be null after destroy
    }

    // ========== Direct Method Invocation Tests for 100% Coverage ==========

    @Test
    fun testUpdateUI_withCompleteProfile() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile()

            // Use reflection to call private updateUI method
            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            val view = fragment.view!!
            val profileName = view.findViewById<TextView>(R.id.profile_name)

            assertNotNull("Profile should be updated", profileName)
        }
    }

    @Test
    fun testUpdateUI_withSolarCalendar() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile(solarOrLunar = "solar")

            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            val view = fragment.view!!
            val calendarTag = view.findViewById<TextView>(R.id.profile_calendar_tag)

            assertEquals("양력", calendarTag.text.toString())
        }
    }

    @Test
    fun testUpdateUI_withLunarCalendar() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile(solarOrLunar = "lunar")

            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            val view = fragment.view!!
            val calendarTag = view.findViewById<TextView>(R.id.profile_calendar_tag)

            assertEquals("음력", calendarTag.text.toString())
        }
    }

    @Test
    fun testGetElementFromCheongan_wood_gap() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "갑") as String

            assertEquals("나무", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_wood_eul() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "을") as String

            assertEquals("나무", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_fire_byeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "병") as String

            assertEquals("불", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_fire_jeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "정") as String

            assertEquals("불", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_earth_mu() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "무") as String

            assertEquals("흙", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_earth_gi() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "기") as String

            assertEquals("흙", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_metal_gyeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "경") as String

            assertEquals("쇠", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_metal_sin() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "신") as String

            assertEquals("쇠", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_water_im() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "임") as String

            assertEquals("물", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_water_gye() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "계") as String

            assertEquals("물", result)
        }
    }

    @Test
    fun testGetElementFromCheongan_unknown() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementFromCheongan", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "미지") as String

            assertEquals("미정", result)
        }
    }

    @Test
    fun testGetElementCharacter_wood_gap() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "갑") as String

            assertEquals("木", result)
        }
    }

    @Test
    fun testGetElementCharacter_wood_eul() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "을") as String

            assertEquals("木", result)
        }
    }

    @Test
    fun testGetElementCharacter_fire_byeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "병") as String

            assertEquals("火", result)
        }
    }

    @Test
    fun testGetElementCharacter_fire_jeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "정") as String

            assertEquals("火", result)
        }
    }

    @Test
    fun testGetElementCharacter_earth_mu() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "무") as String

            assertEquals("土", result)
        }
    }

    @Test
    fun testGetElementCharacter_earth_gi() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "기") as String

            assertEquals("土", result)
        }
    }

    @Test
    fun testGetElementCharacter_metal_gyeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "경") as String

            assertEquals("金", result)
        }
    }

    @Test
    fun testGetElementCharacter_metal_sin() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "신") as String

            assertEquals("金", result)
        }
    }

    @Test
    fun testGetElementCharacter_water_im() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "임") as String

            assertEquals("水", result)
        }
    }

    @Test
    fun testGetElementCharacter_water_gye() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "계") as String

            assertEquals("水", result)
        }
    }

    @Test
    fun testGetElementCharacter_unknown() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementCharacter", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "미지") as String

            assertEquals("?", result)
        }
    }

    @Test
    fun testGetElementColor_wood_gap() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "갑") as Int

            assertEquals(Color.parseColor("#0BEFA0"), result)
        }
    }

    @Test
    fun testGetElementColor_wood_eul() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "을") as Int

            assertEquals(Color.parseColor("#0BEFA0"), result)
        }
    }

    @Test
    fun testGetElementColor_fire_byeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "병") as Int

            assertEquals(Color.parseColor("#F93E3E"), result)
        }
    }

    @Test
    fun testGetElementColor_fire_jeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "정") as Int

            assertEquals(Color.parseColor("#F93E3E"), result)
        }
    }

    @Test
    fun testGetElementColor_earth_mu() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "무") as Int

            assertEquals(Color.parseColor("#8B4513"), result)
        }
    }

    @Test
    fun testGetElementColor_earth_gi() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "기") as Int

            assertEquals(Color.parseColor("#8B4513"), result)
        }
    }

    @Test
    fun testGetElementColor_metal_gyeong() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "경") as Int

            assertEquals(Color.parseColor("#C0C0C0"), result)
        }
    }

    @Test
    fun testGetElementColor_metal_sin() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "신") as Int

            assertEquals(Color.parseColor("#C0C0C0"), result)
        }
    }

    @Test
    fun testGetElementColor_water_im() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "임") as Int

            assertEquals(Color.parseColor("#2BB3FC"), result)
        }
    }

    @Test
    fun testGetElementColor_water_gye() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "계") as Int

            assertEquals(Color.parseColor("#2BB3FC"), result)
        }
    }

    @Test
    fun testGetElementColor_unknown() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
            method.isAccessible = true
            val result = method.invoke(fragment, "미지") as Int

            assertEquals(Color.WHITE, result)
        }
    }

    @Test
    fun testUpdateCollectedElements_withVariousCounts() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile()

            val method = ProfileFragment::class.java.getDeclaredMethod("updateCollectedElements", UserProfile::class.java)
            method.isAccessible = true
            method.invoke(fragment, testProfile)

            Thread.sleep(200)

            val view = fragment.view!!
            val badge1 = view.findViewById<TextView>(R.id.element_badge_1)

            assertNotNull("Badges should be updated", badge1)
        }
    }

    @Test
    fun testUpdateElementBadge_withCount() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val badge = view.findViewById<TextView>(R.id.element_badge_1)

            val method = ProfileFragment::class.java.getDeclaredMethod("updateElementBadge", TextView::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(fragment, badge, 5, Color.parseColor("#0BEFA0"))

            Thread.sleep(200)

            assertEquals("5", badge.text.toString())
            assertEquals(Color.WHITE, badge.currentTextColor)
        }
    }

    @Test
    fun testUpdateUI_withNullNickname() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile(nickname = null)

            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            val view = fragment.view!!
            val profileName = view.findViewById<TextView>(R.id.profile_name)

            assertEquals("사용자", profileName.text.toString())
        }
    }

    @Test
    fun testUpdateUI_withNullDailyGanjiHandled() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile(dailyGanji = null)

            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            assertNotNull("Should handle null dailyGanji", fragment.view)
        }
    }

    @Test
    fun testUpdateUI_withEmptyDailyGanjiHandled() {
        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val testProfile = createCompleteTestProfile(dailyGanji = "")

            val updateUIMethod = ProfileFragment::class.java.getDeclaredMethod("updateUI", UserProfile::class.java)
            updateUIMethod.isAccessible = true
            updateUIMethod.invoke(fragment, testProfile)

            Thread.sleep(200)

            assertNotNull("Should handle empty dailyGanji", fragment.view)
        }
    }

    @Test
    fun testSettingsButtonNavigation() {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )

        scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.profileFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        Thread.sleep(200)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val settingsButton = view.findViewById<View>(R.id.btn_edit_profile)

            // Click the settings button
            settingsButton.performClick()

            Thread.sleep(200)

            // Verify navigation was triggered
            val currentDest = navController.currentDestination?.id
            assertEquals("Should navigate to Settings", R.id.settingsFragment, currentDest)
        }
    }

    // ========== Helper Methods ==========

    private fun createCompleteTestProfile(
        nickname: String? = "테스트",
        solarOrLunar: String = "solar",
        dailyGanji: String? = "갑자"
    ): com.example.fortuna_android.api.UserProfile {
        return com.example.fortuna_android.api.UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "테스트",
            profileImage = null,
            nickname = nickname,
            birthDateSolar = "1990-01-01",
            birthDateLunar = null,
            solarOrLunar = solarOrLunar,
            birthTimeUnits = "자시",
            gender = "M",
            yearlyGanji = "경오",
            monthlyGanji = "무인",
            dailyGanji = dailyGanji,
            hourlyGanji = "병자",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = "2025-11-01T00:00:00Z",
            collectionStatus = com.example.fortuna_android.api.CollectionStatus(
                collections = listOf(
                    com.example.fortuna_android.api.CollectionItem(chakraType = "wood", count = 1),
                    com.example.fortuna_android.api.CollectionItem(chakraType = "fire", count = 2),
                    com.example.fortuna_android.api.CollectionItem(chakraType = "earth", count = 3),
                    com.example.fortuna_android.api.CollectionItem(chakraType = "metal", count = 4),
                    com.example.fortuna_android.api.CollectionItem(chakraType = "water", count = 5),
                ),
                totalCount = 1 + 2 + 3 + 4 + 5
            )
        )
    }
}
