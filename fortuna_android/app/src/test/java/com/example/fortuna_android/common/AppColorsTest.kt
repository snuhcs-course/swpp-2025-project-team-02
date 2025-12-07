package com.example.fortuna_android.common

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AppColorsTest {

    @Test
    fun `getElementColorByStem maps stems to colors`() {
        assertEquals(Color.parseColor(AppColors.ELEMENT_WOOD), AppColors.getElementColorByStem("갑"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_FIRE), AppColors.getElementColorByStem("병"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_EARTH), AppColors.getElementColorByStem("기"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_METAL), AppColors.getElementColorByStem("경"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_WATER), AppColors.getElementColorByStem("임"))
        assertEquals(Color.parseColor(AppColors.COLOR_LIGHT_GRAY), AppColors.getElementColorByStem("unknown"))
    }

    @Test
    fun `getElementColorByBranch maps branches to colors`() {
        assertEquals(Color.parseColor(AppColors.ELEMENT_WOOD), AppColors.getElementColorByBranch("인"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_FIRE), AppColors.getElementColorByBranch("오"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_EARTH), AppColors.getElementColorByBranch("술"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_METAL), AppColors.getElementColorByBranch("신"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_WATER), AppColors.getElementColorByBranch("자"))
        assertEquals(Color.parseColor(AppColors.COLOR_LIGHT_GRAY), AppColors.getElementColorByBranch("???"))
    }

    @Test
    fun `getElementColorByEnglish and Korean map names`() {
        assertEquals(Color.parseColor(AppColors.ELEMENT_WOOD), AppColors.getElementColorByEnglish("wood"))
        assertEquals(Color.parseColor(AppColors.ELEMENT_FIRE), AppColors.getElementColorByKorean("불"))
        assertEquals(Color.parseColor(AppColors.COLOR_WHITE), AppColors.getElementColorByEnglish("nope"))
        assertEquals(Color.parseColor(AppColors.COLOR_LIGHT_GRAY), AppColors.getElementColorByKorean("없음"))
    }
}
