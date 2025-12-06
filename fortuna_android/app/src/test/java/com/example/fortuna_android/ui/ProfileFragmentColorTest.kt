package com.example.fortuna_android.ui

import com.example.fortuna_android.common.AppColors
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ProfileFragmentColorTest {

    private fun getElementColor(fragment: ProfileFragment, stem: String): Int {
        val method = ProfileFragment::class.java.getDeclaredMethod("getElementColor", String::class.java)
        method.isAccessible = true
        return method.invoke(fragment, stem) as Int
    }

    @Test
    fun getElementColor_matchesAppColors() {
        val fragment = ProfileFragment()
        val expectedWood = AppColors.getElementColorByStem("갑")
        val expectedFire = AppColors.getElementColorByStem("병")
        val expectedEarth = AppColors.getElementColorByStem("무")
        val expectedMetal = AppColors.getElementColorByStem("경")
        val expectedWater = AppColors.getElementColorByStem("임")

        assertEquals(expectedWood, getElementColor(fragment, "갑"))
        assertEquals(expectedFire, getElementColor(fragment, "병"))
        assertEquals(expectedEarth, getElementColor(fragment, "무"))
        assertEquals(expectedMetal, getElementColor(fragment, "경"))
        assertEquals(expectedWater, getElementColor(fragment, "임"))
    }
}
