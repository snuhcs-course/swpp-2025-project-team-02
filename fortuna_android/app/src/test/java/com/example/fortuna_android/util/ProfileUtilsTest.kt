package com.example.fortuna_android.util

import com.example.fortuna_android.api.UpdatedUserData
import com.example.fortuna_android.api.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUtilsTest {

    @Test
    fun `isProfileComplete returns false for null or missing fields`() {
        assertFalse(ProfileUtils.isProfileComplete(null as UserProfile?))

        val incomplete = UserProfile(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            profileImage = null,
            nickname = "",
            birthDateLunar = null,
            birthDateSolar = null,
            solarOrLunar = "",
            birthTimeUnits = "",
            gender = "",
            yearlyGanji = null,
            monthlyGanji = null,
            dailyGanji = null,
            hourlyGanji = null,
            createdAt = null,
            lastLogin = null,
            collectionStatus = null
        )
        assertFalse(ProfileUtils.isProfileComplete(incomplete))
    }

    @Test
    fun `isProfileComplete returns true when required fields present`() {
        val complete = UserProfile(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            profileImage = null,
            nickname = "Nick",
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
        assertTrue(ProfileUtils.isProfileComplete(complete))
    }

    @Test
    fun `isProfileComplete UpdatedUserData variants`() {
        val incomplete = UpdatedUserData(
            userId = 0,
            email = "",
            name = "",
            nickname = "",
            birthDateLunar = "",
            birthDateSolar = "",
            solarOrLunar = "",
            birthTimeUnits = "",
            gender = "",
            yearlyGanji = "",
            monthlyGanji = "",
            dailyGanji = "",
            hourlyGanji = ""
        )
        assertFalse(ProfileUtils.isProfileComplete(incomplete))

        val complete = UpdatedUserData(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            nickname = "Nick",
            birthDateLunar = "1990-01-01",
            birthDateSolar = "",
            solarOrLunar = "lunar",
            birthTimeUnits = "10",
            gender = "male",
            yearlyGanji = "갑",
            monthlyGanji = "을",
            dailyGanji = "병",
            hourlyGanji = "정"
        )
        assertTrue(ProfileUtils.isProfileComplete(complete))
    }

    @Test
    fun `isDefaultProfile detects default nickname or birthdate`() {
        val defaultNickname = UserProfile(
            userId = 1,
            email = "test@test.com",
            name = "Test",
            profileImage = null,
            nickname = "DefaultUser",
            birthDateLunar = null,
            birthDateSolar = null,
            solarOrLunar = "",
            birthTimeUnits = "",
            gender = "",
            yearlyGanji = null,
            monthlyGanji = null,
            dailyGanji = null,
            hourlyGanji = null,
            createdAt = null,
            lastLogin = null,
            collectionStatus = null
        )
        assertTrue(ProfileUtils.isDefaultProfile(defaultNickname))

        val defaultBirth = defaultNickname.copy(nickname = "Nick", birthDateSolar = "1900-01-01")
        assertTrue(ProfileUtils.isDefaultProfile(defaultBirth))

        val custom = defaultNickname.copy(nickname = "Nick", birthDateSolar = "2000-01-01")
        assertFalse(ProfileUtils.isDefaultProfile(custom))
    }
}
