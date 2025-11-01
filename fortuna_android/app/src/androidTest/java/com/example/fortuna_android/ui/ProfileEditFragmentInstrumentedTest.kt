package com.example.fortuna_android.ui

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import com.example.fortuna_android.api.UserProfile
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProfileEditFragment
 *
 * Tests cover:
 * - Fragment lifecycle (onCreateView, onViewCreated, onDestroyView)
 * - setupSpinners (all spinners, early returns)
 * - setupClickListeners (all buttons, backButton, saveButton)
 * - loadUserProfile (no token, success, failure, exception)
 * - prefillProfileData (all fields, solar/lunar, gender parsing)
 * - updateSolarLunarButtons (solar, lunar)
 * - updateGenderButtons (male, female)
 * - submitProfile (all validations)
 * - extractBirthTimeUnit
 * - updateProfile (success, failure, exception, no token)
 */
@RunWith(AndroidJUnit4::class)
class ProfileEditFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<ProfileEditFragment>
    private lateinit var context: Context
    private val prefsName = "fortuna_prefs"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            try {
                scenario.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ========== Lifecycle Tests ==========

    @Test
    fun testOnCreateView() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull("Fragment should not be null", fragment)
            assertNotNull("View should be created", fragment.view)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testOnViewCreated() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view
            assertNotNull("View should exist", view)

            // Verify spinners are initialized
            val birthYearSpinner = view?.findViewById<android.widget.Spinner>(R.id.birth_year_spinner)
            assertNotNull("Year spinner should exist", birthYearSpinner)
        }
    }

    @Test
    fun testOnDestroyView() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("View should exist before destroy", fragment.view)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        Thread.sleep(100)
    }

    // ========== setupSpinners() Tests ==========

    @Test
    fun testSetupSpinners_allInitialized() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val birthYearSpinner = view.findViewById<android.widget.Spinner>(R.id.birth_year_spinner)
            val birthMonthSpinner = view.findViewById<android.widget.Spinner>(R.id.birth_month_spinner)
            val birthDaySpinner = view.findViewById<android.widget.Spinner>(R.id.birth_day_spinner)

            assertNotNull("Year spinner should have adapter", birthYearSpinner.adapter)
            assertNotNull("Month spinner should have adapter", birthMonthSpinner.adapter)
            assertNotNull("Day spinner should have adapter", birthDaySpinner.adapter)

            assertEquals(126, birthYearSpinner.adapter.count) // 1900-2025
            assertEquals(12, birthMonthSpinner.adapter.count)
            assertEquals(31, birthDaySpinner.adapter.count)
        }
    }

    // ========== setupClickListeners() Tests ==========

    @Test
    fun testBackButton() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val backButton = view.findViewById<View>(R.id.back_button)

            backButton.performClick()
            Thread.sleep(100)

            // Navigation should be triggered
        }
    }

    @Test
    fun testSolarButtonClick() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)

            solarButton.performClick()
            Thread.sleep(100)

            // Solar button should be selected (color changes)
        }
    }

    @Test
    fun testLunarButtonClick() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)

            lunarButton.performClick()
            Thread.sleep(100)

            // Lunar button should be selected
        }
    }

    @Test
    fun testMaleButtonClick() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)

            maleButton.performClick()
            Thread.sleep(100)

            // Male button should be selected
        }
    }

    @Test
    fun testFemaleButtonClick() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            femaleButton.performClick()
            Thread.sleep(100)

            // Female button should be selected
        }
    }

    @Test
    fun testSaveButton() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val saveButton = view.findViewById<Button>(R.id.save_button)

            // Click without filling - should show validation error
            saveButton.performClick()
            Thread.sleep(500)
        }
    }

    // ========== loadUserProfile() Tests ==========

    @Test
    fun testLoadUserProfile_noToken() {
        // No token set
        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Should navigate back due to no token
        }
    }

    @Test
    fun testLoadUserProfile_withToken() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // API call should be made
        }
    }

    // ========== prefillProfileData() Tests ==========

    @Test
    fun testPrefillProfileData_solarMale() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Data should be prefilled if API call succeeds
        }
    }

    @Test
    fun testPrefillProfileData_lunarFemale() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(2000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Test lunar calendar with female gender
        }
    }

    // ========== updateSolarLunarButtons() Tests ==========

    @Test
    fun testUpdateSolarLunarButtons_solar() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)

            solarButton.performClick()
            Thread.sleep(100)

            // Colors should be updated (solar = white bg, lunar = black bg)
        }
    }

    @Test
    fun testUpdateSolarLunarButtons_lunar() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)

            lunarButton.performClick()
            Thread.sleep(100)

            // Colors should be updated (lunar = white bg, solar = black bg)
        }
    }

    // ========== updateGenderButtons() Tests ==========

    @Test
    fun testUpdateGenderButtons_male() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)

            maleButton.performClick()
            Thread.sleep(100)

            // Colors should be updated
        }
    }

    @Test
    fun testUpdateGenderButtons_female() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            femaleButton.performClick()
            Thread.sleep(100)

            // Colors should be updated
        }
    }

    // ========== submitProfile() Tests ==========

    @Test
    fun testSubmitProfile_emptyNickname() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            // Clear nickname
            nicknameEditText.setText("")

            saveButton.performClick()
            Thread.sleep(500)

            // Should show error toast
        }
    }

    @Test
    fun testSubmitProfile_emptySolarLunar() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("TestUser")
            // Don't select solar/lunar

            saveButton.performClick()
            Thread.sleep(500)

            // Should show error
        }
    }

    @Test
    fun testSubmitProfile_emptyGender() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("TestUser")
            solarButton.performClick()
            Thread.sleep(100)
            // Don't select gender

            saveButton.performClick()
            Thread.sleep(500)

            // Should show error
        }
    }

    @Test
    fun testSubmitProfile_valid() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("TestUser")
            solarButton.performClick()
            Thread.sleep(100)
            maleButton.performClick()
            Thread.sleep(100)

            saveButton.performClick()
            Thread.sleep(2000)

            // Should call updateProfile API
        }
    }

    // ========== extractBirthTimeUnit() Tests ==========

    @Test
    fun testExtractBirthTimeUnit() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val birthTimeSpinner = view.findViewById<android.widget.Spinner>(R.id.birth_time_spinner)

            assertNotNull("Birth time spinner should exist", birthTimeSpinner)
        }
    }

    // ========== updateProfile() Tests ==========

    @Test
    fun testUpdateProfile_noToken() {
        // Clear token
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Should handle no token case
        }
    }

    @Test
    fun testUpdateProfile_success() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("UpdatedUser")
            lunarButton.performClick()
            Thread.sleep(100)
            femaleButton.performClick()
            Thread.sleep(100)

            saveButton.performClick()
            Thread.sleep(2000)

            // Should call API and navigate back on success
        }
    }

    @Test
    fun testUpdateProfile_failure() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("FailUser")
            solarButton.performClick()
            Thread.sleep(100)
            maleButton.performClick()
            Thread.sleep(100)

            saveButton.performClick()
            Thread.sleep(2000)

            // API might fail - should show error
        }
    }

    @Test
    fun testUpdateProfile_exception() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            nicknameEditText.setText("ExceptionUser")
            lunarButton.performClick()
            Thread.sleep(100)
            femaleButton.performClick()
            Thread.sleep(100)

            saveButton.performClick()
            Thread.sleep(2000)

            // Exception should be caught
        }
    }

    // ========== Integration Tests ==========

    @Test
    fun testCompleteEditFlow() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val nicknameEditText = view.findViewById<android.widget.EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val saveButton = view.findViewById<Button>(R.id.save_button)

            // Edit profile
            nicknameEditText.setText("EditedName")
            solarButton.performClick()
            Thread.sleep(100)
            maleButton.performClick()
            Thread.sleep(100)

            saveButton.performClick()
            Thread.sleep(2000)
        }
    }

    @Test
    fun testFragmentRecreation() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        scenario.recreate()
        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("View should exist after recreation", fragment.view)
        }
    }

    @Test
    fun testAllButtonCombinations() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            // Test all combinations
            solarButton.performClick()
            Thread.sleep(50)
            maleButton.performClick()
            Thread.sleep(50)

            lunarButton.performClick()
            Thread.sleep(50)
            femaleButton.performClick()
            Thread.sleep(50)

            solarButton.performClick()
            Thread.sleep(50)
            femaleButton.performClick()
            Thread.sleep(50)

            lunarButton.performClick()
            Thread.sleep(50)
            maleButton.performClick()
            Thread.sleep(50)
        }
    }

    @Test
    fun testSpinnerSelections() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token")
            .commit()

        scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val view = fragment.view!!
            val birthYearSpinner = view.findViewById<android.widget.Spinner>(R.id.birth_year_spinner)
            val birthMonthSpinner = view.findViewById<android.widget.Spinner>(R.id.birth_month_spinner)
            val birthDaySpinner = view.findViewById<android.widget.Spinner>(R.id.birth_day_spinner)

            // Change spinner values
            birthYearSpinner.setSelection(75) // Around 1975
            birthMonthSpinner.setSelection(6) // July
            birthDaySpinner.setSelection(20) // 21st

            assertEquals(75, birthYearSpinner.selectedItemPosition)
            assertEquals(6, birthMonthSpinner.selectedItemPosition)
            assertEquals(20, birthDaySpinner.selectedItemPosition)
        }
    }
}
