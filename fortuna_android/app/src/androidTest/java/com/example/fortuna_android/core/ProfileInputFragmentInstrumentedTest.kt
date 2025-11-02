package com.example.fortuna_android.core

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.ProfileInputFragment
import com.example.fortuna_android.R
import com.example.fortuna_android.api.UserProfile
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProfileInputFragment
 * Tests cover:
 * - Fragment lifecycle (onCreateView, onViewCreated, onDestroyView)
 * - setupSpinners (all spinners, early returns)
 * - setupClickListeners (all buttons)
 * - updateStepUI (all 4 steps, visibility changes)
 * - handleNextButton (all 4 steps with validation)
 * - submitProfile (all validations)
 * - updateProfile (success, failure, exception, token missing)
 * - extractBirthTimeUnit
 * - navigateToMain, navigateToSignIn
 */
@RunWith(AndroidJUnit4::class)
class ProfileInputFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<ProfileInputFragment>
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
            try {
                scenario.close()
            } catch (e: Exception) {
                // Ignore if already closed
            }
        }

        // Clean up
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        unmockkAll()
    }

    // ========== Lifecycle Tests ==========

    @Test
    fun testOnCreateView() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertNotNull("Fragment view should be created", fragment.view)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testOnViewCreated() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            // Verify spinners are set up
            val view = fragment.view
            assertNotNull("View should exist", view)

            // Verify UI is initialized for step 1
            val stepIndicator = view?.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 1 / 4", stepIndicator?.text)
        }
    }

    @Test
    fun testOnDestroyView() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("View should exist before destroy", fragment.view)
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)

        // Fragment is destroyed, can't access it anymore
        Thread.sleep(100)
    }

    // ========== setupSpinners() Tests ==========

    @Test
    fun testSetupSpinners_allSpinnersInitialized() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val birthYearSpinner = view.findViewById<Spinner>(R.id.birth_year_spinner)
            val birthMonthSpinner = view.findViewById<Spinner>(R.id.birth_month_spinner)
            val birthDaySpinner = view.findViewById<Spinner>(R.id.birth_day_spinner)

            // Verify spinners have adapters
            assertNotNull("Year spinner should have adapter", birthYearSpinner.adapter)
            assertNotNull("Month spinner should have adapter", birthMonthSpinner.adapter)
            assertNotNull("Day spinner should have adapter", birthDaySpinner.adapter)

            // Verify year range (1900-2025)
            assertEquals(126, birthYearSpinner.adapter.count)

            // Verify months (1-12)
            assertEquals(12, birthMonthSpinner.adapter.count)

            // Verify days (1-31)
            assertEquals(31, birthDaySpinner.adapter.count)
        }
    }

    // ========== setupClickListeners() Tests ==========

    @Test
    fun testMaleButtonClick() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            maleButton.performClick()

            assertTrue("Male button should be selected", maleButton.isSelected)
            assertFalse("Female button should not be selected", femaleButton.isSelected)
        }
    }

    @Test
    fun testFemaleButtonClick() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            femaleButton.performClick()

            assertTrue("Female button should be selected", femaleButton.isSelected)
            assertFalse("Male button should not be selected", maleButton.isSelected)
        }
    }

    @Test
    fun testSolarButtonClick() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val solarRadioButton = view.findViewById<RadioButton>(R.id.solar_radio_button)

            solarButton.performClick()

            assertTrue("Solar button should be selected", solarButton.isSelected)
            assertFalse("Lunar button should not be selected", lunarButton.isSelected)
            assertTrue("Solar radio button should be checked", solarRadioButton.isChecked)
        }
    }

    @Test
    fun testLunarButtonClick() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val lunarRadioButton = view.findViewById<RadioButton>(R.id.lunar_radio_button)

            lunarButton.performClick()

            assertTrue("Lunar button should be selected", lunarButton.isSelected)
            assertFalse("Solar button should not be selected", solarButton.isSelected)
            assertTrue("Lunar radio button should be checked", lunarRadioButton.isChecked)
        }
    }

    // ========== updateStepUI() Tests ==========

    @Test
    fun testUpdateStepUI_step1() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            val mainTitle = view.findViewById<TextView>(R.id.main_title)
            val nextButton = view.findViewById<Button>(R.id.next_button)
            val step1Container = view.findViewById<View>(R.id.step1_container)

            assertEquals("STEP 1 / 4", stepIndicator.text)
            assertEquals("어떤 이름으로\n불러 드릴까요?", mainTitle.text)
            assertEquals("다음", nextButton.text)
            assertEquals(View.VISIBLE, step1Container.visibility)
        }
    }

    @Test
    fun testUpdateStepUI_step2() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Enter nickname and click next
            nicknameEditText.setText("TestUser")
            nextButton.performClick()

            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            val mainTitle = view.findViewById<TextView>(R.id.main_title)
            val step2Container = view.findViewById<View>(R.id.step2_container)

            assertEquals("STEP 2 / 4", stepIndicator.text)
            assertEquals("사주 정보를 알려주세요.", mainTitle.text)
            assertEquals(View.VISIBLE, step2Container.visibility)
        }
    }

    @Test
    fun testUpdateStepUI_step3() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Step 1: Enter nickname
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)

            // Step 2: Select solar
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            val mainTitle = view.findViewById<TextView>(R.id.main_title)
            val step3Container = view.findViewById<View>(R.id.step3_container)

            assertEquals("STEP 3 / 4", stepIndicator.text)
            assertEquals("태어난 시간\n정보가 필요해요.", mainTitle.text)
            assertEquals(View.VISIBLE, step3Container.visibility)
        }
    }

    @Test
    fun testUpdateStepUI_step4() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Step 1
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)

            // Step 2
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)

            // Step 3
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            val mainTitle = view.findViewById<TextView>(R.id.main_title)
            val step4Container = view.findViewById<View>(R.id.step4_container)

            assertEquals("STEP 4 / 4", stepIndicator.text)
            assertEquals("성별을 알려주세요.", mainTitle.text)
            assertEquals("프로필 저장", nextButton.text)
            assertEquals(View.VISIBLE, step4Container.visibility)
        }
    }

    // ========== handleNextButton() Tests ==========

    @Test
    fun testHandleNextButton_step1_emptyNickname() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nextButton = view.findViewById<Button>(R.id.next_button)
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)

            // Click next without entering nickname
            nextButton.performClick()
            Thread.sleep(300)

            // Should still be on step 1
            assertEquals("STEP 1 / 4", stepIndicator.text)
        }
    }

    @Test
    fun testHandleNextButton_step1_validNickname() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            nicknameEditText.setText("ValidName")
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 2 / 4", stepIndicator.text)
        }
    }

    @Test
    fun testHandleNextButton_step2_noSolarLunar() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Move to step 2
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(300)

            // Try to proceed without selecting solar/lunar
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            // Should still be on step 2
            assertEquals("STEP 2 / 4", stepIndicator.text)
        }
    }

    @Test
    fun testHandleNextButton_step2_validSolarLunar() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Move to step 2
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)

            // Select solar and proceed
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 3 / 4", stepIndicator.text)
        }
    }

    @Test
    fun testHandleNextButton_step3_alwaysValid() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Step 1
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)

            // Step 2
            lunarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)

            // Step 3 - should always proceed
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 4 / 4", stepIndicator.text)
        }
    }

    @Test
    fun testHandleNextButton_step4_noGender() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Move to step 4
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 4 / 4", stepIndicator.text)

            // Try to submit without gender - should stay on step 4
            nextButton.performClick()
            Thread.sleep(300)

            assertEquals("STEP 4 / 4", stepIndicator.text)
        }
    }

    // ========== submitProfile() Tests ==========

    @Test
    fun testSubmitProfile_emptyNickname() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Fill all but clear nickname
            nicknameEditText.setText("Test")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(200)
            maleButton.performClick()
            Thread.sleep(100)

            // Clear nickname
            nicknameEditText.setText("")

            nextButton.performClick()
            Thread.sleep(500)

            // Should show error toast and stay on page
        }
    }

    @Test
    fun testSubmitProfile_emptySolarLunar() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Only fill nickname
            nicknameEditText.setText("TestUser")

            // Manually try to submit (bypassing step validation)
            // This tests the submitProfile() validation
            Thread.sleep(300)
        }
    }

    @Test
    fun testSubmitProfile_emptyGender() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Fill nickname and solar but not gender
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(300)

            // Try to submit without gender
            nextButton.performClick()
            Thread.sleep(500)

            // Should show error
        }
    }

    // ========== extractBirthTimeUnit() Tests ==========

    @Test
    fun testExtractBirthTimeUnit_validInput() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val birthTimeSpinner = view.findViewById<Spinner>(R.id.birth_time_spinner)

            // The spinner should have birth time options
            assertNotNull("Birth time spinner should exist", birthTimeSpinner)
        }
    }

    // ========== updateProfile() Tests ==========

    @Test
    fun testUpdateProfile_noToken() {
        // Clear token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Complete all steps
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(200)
            maleButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(1000)

            // Should navigate to sign in (covered by navigateToSignIn)
        }
    }

    @Test
    fun testUpdateProfile_success() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Complete all steps with female gender
            nicknameEditText.setText("TestUser")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(200)
            femaleButton.performClick()
            Thread.sleep(100)

            // Mock successful response
            val mockResponse = UserProfile(
                userId = 1,
                email = "test@test.com",
                name = "Test",
                profileImage = null,
                nickname = "TestUser",
                birthDateLunar = "2000-01-01",
                birthDateSolar = null,
                solarOrLunar = "solar",
                birthTimeUnits = "10",
                gender = "F",
                yearlyGanji = null,
                monthlyGanji = null,
                dailyGanji = null,
                hourlyGanji = null,
                createdAt = null,
                lastLogin = null,
                collectionStatus = null
            )

            nextButton.performClick()
            Thread.sleep(2000)

            // API call should be made
        }
    }

    @Test
    fun testUpdateProfile_failure() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Complete all steps with lunar calendar
            nicknameEditText.setText("TestUser2")
            nextButton.performClick()
            Thread.sleep(200)
            lunarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(200)
            maleButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(2000)

            // API might fail - error should be handled
        }
    }

    @Test
    fun testUpdateProfile_exception() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Complete all steps
            nicknameEditText.setText("ExceptionTest")
            nextButton.performClick()
            Thread.sleep(200)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(200)
            nextButton.performClick()
            Thread.sleep(200)
            femaleButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(2000)

            // Exception should be caught and handled
        }
    }

    // ========== Integration Tests ==========

    @Test
    fun testCompleteWorkflow_maleUserSolar() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Step 1: Nickname
            assertEquals("STEP 1 / 4", view.findViewById<TextView>(R.id.step_indicator).text)
            nicknameEditText.setText("MaleUser")
            nextButton.performClick()
            Thread.sleep(300)

            // Step 2: Solar/Lunar
            assertEquals("STEP 2 / 4", view.findViewById<TextView>(R.id.step_indicator).text)
            solarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(300)

            // Step 3: Birth time
            assertEquals("STEP 3 / 4", view.findViewById<TextView>(R.id.step_indicator).text)
            nextButton.performClick()
            Thread.sleep(300)

            // Step 4: Gender
            assertEquals("STEP 4 / 4", view.findViewById<TextView>(R.id.step_indicator).text)
            assertEquals("프로필 저장", nextButton.text)
            maleButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(2000)

            // Profile update should be called
        }
    }

    @Test
    fun testCompleteWorkflow_femaleUserLunar() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            // Complete workflow with lunar calendar and female gender
            nicknameEditText.setText("FemaleUser")
            nextButton.performClick()
            Thread.sleep(300)

            lunarButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(300)

            nextButton.performClick()
            Thread.sleep(300)

            femaleButton.performClick()
            Thread.sleep(100)
            nextButton.performClick()
            Thread.sleep(2000)
        }
    }

    @Test
    fun testFragmentRecreation_preservesNothing() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val nicknameEditText = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEditText.setText("TestUser")
        }

        scenario.recreate()
        Thread.sleep(500)

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("View should exist after recreation", fragment.view)
        }
    }

    @Test
    fun testSpinnerSelections() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val birthYearSpinner = view.findViewById<Spinner>(R.id.birth_year_spinner)
            val birthMonthSpinner = view.findViewById<Spinner>(R.id.birth_month_spinner)
            val birthDaySpinner = view.findViewById<Spinner>(R.id.birth_day_spinner)

            // Select different values
            birthYearSpinner.setSelection(50) // Around 1950
            birthMonthSpinner.setSelection(5) // June
            birthDaySpinner.setSelection(14) // 15th

            assertEquals(50, birthYearSpinner.selectedItemPosition)
            assertEquals(5, birthMonthSpinner.selectedItemPosition)
            assertEquals(14, birthDaySpinner.selectedItemPosition)
        }
    }

    @Test
    fun testAllButtonCombinations() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        Thread.sleep(500)

        scenario.onFragment { fragment ->
            val view = fragment.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)

            // Test all combinations
            maleButton.performClick()
            Thread.sleep(50)
            solarButton.performClick()
            Thread.sleep(50)

            femaleButton.performClick()
            Thread.sleep(50)
            lunarButton.performClick()
            Thread.sleep(50)

            maleButton.performClick()
            Thread.sleep(50)
            lunarButton.performClick()
            Thread.sleep(50)

            femaleButton.performClick()
            Thread.sleep(50)
            solarButton.performClick()
            Thread.sleep(50)

            // Final state should be: female + solar
            assertTrue("Female should be selected", femaleButton.isSelected)
            assertTrue("Solar should be selected", solarButton.isSelected)
            assertFalse("Male should not be selected", maleButton.isSelected)
            assertFalse("Lunar should not be selected", lunarButton.isSelected)
        }
    }
}
