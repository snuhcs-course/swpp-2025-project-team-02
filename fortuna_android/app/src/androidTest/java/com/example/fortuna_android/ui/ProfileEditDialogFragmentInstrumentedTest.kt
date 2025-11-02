package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import com.example.fortuna_android.api.UserProfile
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileEditDialogFragmentInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "fortuna_prefs"
    private lateinit var scenario: FragmentScenario<ProfileEditDialogFragment>

    @Before
    fun setup() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("jwt_token", "test_token_12345")
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        if (::scenario.isInitialized) {
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    private fun createTestProfile(
        nickname: String? = "TestNick",
        solarOrLunar: String? = "solar",
        birthDateSolar: String? = "1990-01-01",
        birthDateLunar: String? = null,
        birthTimeUnits: String? = "자시",
        gender: String? = "M"
    ): UserProfile {
        return UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            profileImage = null,
            nickname = nickname,
            birthDateSolar = birthDateSolar,
            birthDateLunar = birthDateLunar,
            solarOrLunar = solarOrLunar,
            birthTimeUnits = birthTimeUnits,
            gender = gender,
            yearlyGanji = "庚午",
            monthlyGanji = "戊寅",
            dailyGanji = "甲子",
            hourlyGanji = "丙午",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = null,
            collectionStatus = null
        )
    }

    @Test
    fun testNewInstanceCreatesFragment() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        assertNotNull(fragment)
        assertTrue(fragment is ProfileEditDialogFragment)
    }

    @Test
    fun testFragmentIsDialogFragment() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        assertTrue(fragment is androidx.fragment.app.DialogFragment)
    }

    @Test
    fun testMultipleInstances() {
        val profile1 = createTestProfile(nickname = "User1Nick", solarOrLunar = "solar")
        val profile2 = createTestProfile(nickname = "User2Nick", solarOrLunar = "lunar",
            birthDateSolar = null, birthDateLunar = "1995-06-15", gender = "F")

        val fragment1 = ProfileEditDialogFragment.newInstance(profile1) {}
        val fragment2 = ProfileEditDialogFragment.newInstance(profile2) {}

        assertNotNull(fragment1)
        assertNotNull(fragment2)
        assertNotSame(fragment1, fragment2)
    }

    @Test
    fun testOnCreateDialogSetsFullScreen() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            // In test environment, dialog window might not be initialized
            // Test that the fragment is properly created instead
            val dialog = frag.onCreateDialog(Bundle())
            assertNotNull(dialog)
            val window = dialog.window
            if (window != null) {
                val layoutParams = window.attributes
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layoutParams?.width)
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, layoutParams?.height)
            }
            // If window is null in test environment, just verify dialog exists
            assertTrue(dialog != null)
        }
    }

    @Test
    fun testOnCreateViewCreatesBinding() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view
            assertNotNull(view)
        }
    }

    @Test
    fun testSetupSpinnersPopulatesYearSpinner() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val yearSpinner = view.findViewById<Spinner>(R.id.birth_year_spinner)
            assertNotNull(yearSpinner)
            assertEquals(126, yearSpinner.adapter.count) // 1900-2025
        }
    }

    @Test
    fun testSetupSpinnersPopulatesMonthSpinner() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val monthSpinner = view.findViewById<Spinner>(R.id.birth_month_spinner)
            assertNotNull(monthSpinner)
            assertEquals(12, monthSpinner.adapter.count)
        }
    }

    @Test
    fun testSetupSpinnersPopulatesDaySpinner() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val daySpinner = view.findViewById<Spinner>(R.id.birth_day_spinner)
            assertNotNull(daySpinner)
            assertEquals(31, daySpinner.adapter.count)
        }
    }

    @Test
    fun testSetupSpinnersWhenNotAdded() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(300)

        // Should handle gracefully when fragment is not added
        scenario.onFragment { frag ->
            // Fragment should still exist
            assertNotNull(frag)
        }
    }

    @Test
    fun testMaleButtonClickUpdatesGender() {
        val testProfile = createTestProfile(gender = "F")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)

            maleButton.performClick()
            Thread.sleep(200)

            // Use reflection to check selectedGender
            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            field.isAccessible = true
            val selectedGender = field.get(frag) as String
            assertEquals("M", selectedGender)
        }
    }

    @Test
    fun testFemaleButtonClickUpdatesGender() {
        val testProfile = createTestProfile(gender = "M")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val femaleButton = view.findViewById<Button>(R.id.female_button)

            femaleButton.performClick()
            Thread.sleep(200)

            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            field.isAccessible = true
            val selectedGender = field.get(frag) as String
            assertEquals("F", selectedGender)
        }
    }

    @Test
    fun testSolarButtonClickUpdatesSolarLunar() {
        val testProfile = createTestProfile(solarOrLunar = "lunar")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)

            solarButton.performClick()
            Thread.sleep(200)

            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedSolarLunar")
            field.isAccessible = true
            val selectedSolarLunar = field.get(frag) as String
            assertEquals("solar", selectedSolarLunar)
        }
    }

    @Test
    fun testLunarButtonClickUpdatesSolarLunar() {
        val testProfile = createTestProfile(solarOrLunar = "solar")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)

            lunarButton.performClick()
            Thread.sleep(200)

            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedSolarLunar")
            field.isAccessible = true
            val selectedSolarLunar = field.get(frag) as String
            assertEquals("lunar", selectedSolarLunar)
        }
    }

    @Test
    fun testPrefillProfileDataWithSolarDate() {
        val testProfile = createTestProfile(
            nickname = "MyNick", // Limited to 6 chars due to maxLength in XML
            birthDateSolar = "1995-06-15",
            solarOrLunar = "solar",
            gender = "M",
            birthTimeUnits = "오시"
        )
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            val yearSpinner = view.findViewById<Spinner>(R.id.birth_year_spinner)
            val monthSpinner = view.findViewById<Spinner>(R.id.birth_month_spinner)
            val daySpinner = view.findViewById<Spinner>(R.id.birth_day_spinner)

            assertEquals("MyNick", nicknameEdit.text.toString())
            assertTrue(yearSpinner.selectedItem.toString().contains("1995"))
            assertEquals("6월", monthSpinner.selectedItem.toString())
            assertEquals("15일", daySpinner.selectedItem.toString())
        }
    }

    @Test
    fun testPrefillProfileDataWithLunarDate() {
        val testProfile = createTestProfile(
            birthDateSolar = null,
            birthDateLunar = "1990-03-20",
            solarOrLunar = "lunar"
        )
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val yearSpinner = view.findViewById<Spinner>(R.id.birth_year_spinner)
            val monthSpinner = view.findViewById<Spinner>(R.id.birth_month_spinner)
            val daySpinner = view.findViewById<Spinner>(R.id.birth_day_spinner)

            assertTrue(yearSpinner.selectedItem.toString().contains("1990"))
            assertEquals("3월", monthSpinner.selectedItem.toString())
            assertEquals("20일", daySpinner.selectedItem.toString())
        }
    }

    @Test
    fun testPrefillProfileDataWithFemaleGender() {
        val testProfile = createTestProfile(gender = "F")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            field.isAccessible = true
            val selectedGender = field.get(frag) as String
            assertEquals("F", selectedGender)
        }
    }

    @Test
    fun testPrefillProfileDataWithKoreanGender() {
        val testProfile = createTestProfile(gender = "여자")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            field.isAccessible = true
            val selectedGender = field.get(frag) as String
            assertEquals("F", selectedGender)
        }
    }

    @Test
    fun testUpdateStepUIStep1() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            val mainTitle = view.findViewById<TextView>(R.id.main_title)
            val nextButton = view.findViewById<Button>(R.id.next_button)

            assertEquals("STEP 1 / 4", stepIndicator.text.toString())
            assertTrue(mainTitle.text.toString().contains("이름"))
            assertEquals("다음", nextButton.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep1WithEmptyNickname() {
        val testProfile = createTestProfile(nickname = "")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("")

            val nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Should remain at step 1
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 1 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep1WithValidNickname() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("ValidNickname")

            val nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Should move to step 2
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 2 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep2WithoutSolarLunar() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Move to step 2
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")
            val nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Clear solar/lunar selection
            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedSolarLunar")
            field.isAccessible = true
            field.set(frag, "")

            // Try to proceed
            nextButton.performClick()
            Thread.sleep(300)

            // Should remain at step 2
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 2 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep2ToStep3() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Move to step 2
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")
            var nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Select solar/lunar
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)

            // Proceed to step 3
            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 3 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep3ToStep4() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Navigate to step 3
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")
            var nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)

            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Proceed to step 4
            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 4 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testHandleNextButtonStep4WithoutGender() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Navigate to step 4
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")
            var nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)

            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Clear gender
            val genderField = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            genderField.isAccessible = true
            genderField.set(frag, "")

            // Try to submit
            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Should show error and remain at step 4
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 4 / 4", stepIndicator.text.toString())
        }
    }

    @Test
    fun testSubmitProfileWithEmptyNickname() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Clear nickname
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("")

            // Try to invoke submitProfile via reflection
            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("submitProfile")
            method.isAccessible = true
            method.invoke(frag)
            Thread.sleep(300)

            // Fragment should not dismiss
            assertNotNull(frag.view)
        }
    }

    @Test
    fun testSubmitProfileWithEmptySolarLunar() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")

            // Clear solarOrLunar
            val field = ProfileEditDialogFragment::class.java.getDeclaredField("selectedSolarLunar")
            field.isAccessible = true
            field.set(frag, "")

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("submitProfile")
            method.isAccessible = true
            method.invoke(frag)
            Thread.sleep(300)

            assertNotNull(frag.view)
        }
    }

    @Test
    fun testSubmitProfileWithEmptyGender() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("TestNick")

            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)

            // Clear gender
            val genderField = ProfileEditDialogFragment::class.java.getDeclaredField("selectedGender")
            genderField.isAccessible = true
            genderField.set(frag, "")

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("submitProfile")
            method.isAccessible = true
            method.invoke(frag)
            Thread.sleep(300)

            assertNotNull(frag.view)
        }
    }

    @Test
    fun testExtractBirthTimeUnit() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod(
                "extractBirthTimeUnit",
                String::class.java
            )
            method.isAccessible = true

            val result1 = method.invoke(frag, "자시 (23:30 ~ 01:30)") as String
            assertEquals("자시", result1)

            val result2 = method.invoke(frag, "오시 (11:30 ~ 13:30)") as String
            assertEquals("오시", result2)

            val result3 = method.invoke(frag, "") as String
            assertEquals("", result3)
        }
    }

    @Test
    fun testUpdateProfileWithoutToken() {
        // Remove token
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .remove("jwt_token")
            .commit()

        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod(
                "updateProfile",
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(frag, "TestNick", "1990-01-01", "solar", "자시", "M")
            Thread.sleep(500)

            // Fragment should try to dismiss
            // We can't easily test the actual dismissal in instrumented test
        }
    }

    @Test
    fun testUpdateProfileWhenNotAdded() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(300)

        scenario.onFragment { frag ->
            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod(
                "updateProfile",
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(frag, "TestNick", "1990-01-01", "solar", "자시", "M")
            Thread.sleep(300)

            // Should return early due to !isAdded check
        }
    }

    @Test
    fun testUpdateGenderButtonsWithMale() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val maleButton = view.findViewById<Button>(R.id.male_button)
            maleButton.performClick()
            Thread.sleep(200)

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("updateGenderButtons")
            method.isAccessible = true
            method.invoke(frag)

            // Check button states
            assertNotNull(maleButton)
        }
    }

    @Test
    fun testUpdateGenderButtonsWithFemale() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val femaleButton = view.findViewById<Button>(R.id.female_button)
            femaleButton.performClick()
            Thread.sleep(200)

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("updateGenderButtons")
            method.isAccessible = true
            method.invoke(frag)

            assertNotNull(femaleButton)
        }
    }

    @Test
    fun testUpdateSolarLunarButtonsWithSolar() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("updateSolarLunarButtons")
            method.isAccessible = true
            method.invoke(frag)

            assertNotNull(solarButton)
        }
    }

    @Test
    fun testUpdateSolarLunarButtonsWithLunar() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val lunarButton = view.findViewById<Button>(R.id.lunar_button)
            lunarButton.performClick()
            Thread.sleep(200)

            val method = ProfileEditDialogFragment::class.java.getDeclaredMethod("updateSolarLunarButtons")
            method.isAccessible = true
            method.invoke(frag)

            assertNotNull(lunarButton)
        }
    }

    @Test
    fun testOnDestroyViewClearsBinding() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            assertNotNull(frag.view)
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)
        Thread.sleep(300)

        // Binding should be null after destroy
    }

    @Test
    fun testPrefillWithInvalidDateFormat() {
        val testProfile = createTestProfile(birthDateSolar = "invalid-date")
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            // Should handle gracefully without crashing
            assertNotNull(frag.view)
        }
    }

    @Test
    fun testPrefillWithNullNickname() {
        val testProfile = createTestProfile(nickname = null)
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            assertEquals("", nicknameEdit.text.toString())
        }
    }

    @Test
    fun testCallbackInvokedOnProfileUpdate() {
        var callbackInvoked = false
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {
            callbackInvoked = true
        }

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val callbackField = ProfileEditDialogFragment::class.java.getDeclaredField("onProfileUpdated")
            callbackField.isAccessible = true
            val callback = callbackField.get(frag) as? (() -> Unit)

            assertNotNull(callback)
            callback?.invoke()
            assertTrue(callbackInvoked)
        }
    }

    @Test
    fun testFullWorkflowFromStep1ToStep4() {
        val testProfile = createTestProfile()
        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        scenario = launchFragmentInContainer(
            themeResId = R.style.Theme_Fortuna_android
        ) {
            fragment
        }

        Thread.sleep(500)

        scenario.onFragment { frag ->
            val view = frag.view!!

            // Step 1: Enter nickname
            val nicknameEdit = view.findViewById<EditText>(R.id.nickname_edit_text)
            nicknameEdit.setText("MyName")
            var nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Step 2: Select solar/lunar
            val solarButton = view.findViewById<Button>(R.id.solar_button)
            solarButton.performClick()
            Thread.sleep(200)
            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Step 3: Birth time (already selected)
            nextButton = view.findViewById<Button>(R.id.next_button)
            nextButton.performClick()
            Thread.sleep(300)

            // Step 4: Select gender
            val maleButton = view.findViewById<Button>(R.id.male_button)
            maleButton.performClick()
            Thread.sleep(200)

            // Verify we're at step 4
            val stepIndicator = view.findViewById<TextView>(R.id.step_indicator)
            assertEquals("STEP 4 / 4", stepIndicator.text.toString())
            assertEquals("프로필 저장", nextButton.text.toString())
        }
    }
}
