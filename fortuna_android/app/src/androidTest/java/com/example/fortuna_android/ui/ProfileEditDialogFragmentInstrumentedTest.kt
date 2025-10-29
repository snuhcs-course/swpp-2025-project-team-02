package com.example.fortuna_android.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Before
    fun setup() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testNewInstanceCreatesFragment() {
        val testProfile = UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            profileImage = null,
            nickname = "TestNick",
            birthDateSolar = "1990-01-01",
            birthDateLunar = null,
            solarOrLunar = "solar",
            birthTimeUnits = "12:00",
            gender = "male",
            yearlyGanji = "庚午",
            monthlyGanji = "戊寅",
            dailyGanji = "甲子",
            hourlyGanji = "丙午",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = null,
            collectionStatus = null
        )

        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {
            // Callback
        }

        assertNotNull(fragment)
        assertTrue(fragment is ProfileEditDialogFragment)
    }

    @Test
    fun testFragmentIsDialogFragment() {
        val testProfile = UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            profileImage = null,
            nickname = "TestNick",
            birthDateSolar = "1990-01-01",
            birthDateLunar = null,
            solarOrLunar = "solar",
            birthTimeUnits = "12:00",
            gender = "male",
            yearlyGanji = "庚午",
            monthlyGanji = "戊寅",
            dailyGanji = "甲子",
            hourlyGanji = "丙午",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = null,
            collectionStatus = null
        )

        val fragment = ProfileEditDialogFragment.newInstance(testProfile) {}

        assertTrue(fragment is androidx.fragment.app.DialogFragment)
    }

    @Test
    fun testMultipleInstances() {
        val profile1 = UserProfile(
            userId = 1,
            email = "user1@example.com",
            name = "User 1",
            profileImage = null,
            nickname = "User1Nick",
            birthDateSolar = "1990-01-01",
            birthDateLunar = null,
            solarOrLunar = "solar",
            birthTimeUnits = "12:00",
            gender = "male",
            yearlyGanji = "庚午",
            monthlyGanji = "戊寅",
            dailyGanji = "甲子",
            hourlyGanji = "丙午",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = null,
            collectionStatus = null
        )

        val profile2 = UserProfile(
            userId = 2,
            email = "user2@example.com",
            name = "User 2",
            profileImage = null,
            nickname = "User2Nick",
            birthDateSolar = null,
            birthDateLunar = "1995-06-15",
            solarOrLunar = "lunar",
            birthTimeUnits = "18:30",
            gender = "female",
            yearlyGanji = "乙亥",
            monthlyGanji = "壬午",
            dailyGanji = "丁卯",
            hourlyGanji = "己酉",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = null,
            collectionStatus = null
        )

        val fragment1 = ProfileEditDialogFragment.newInstance(profile1) {}
        val fragment2 = ProfileEditDialogFragment.newInstance(profile2) {}

        assertNotNull(fragment1)
        assertNotNull(fragment2)
        assertNotSame(fragment1, fragment2)
    }
}
