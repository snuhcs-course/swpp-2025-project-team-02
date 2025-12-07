package com.example.fortuna_android.ui

import android.widget.LinearLayout
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.IntroPagerAdapter
import com.example.fortuna_android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@LooperMode(LooperMode.Mode.PAUSED)
class SajuGuideFragmentTest {

    @Test
    fun viewPager_hasExpectedPages_andDotsMatch() {
        val expectedCount = IntroPagerAdapter().itemCount

        val scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val viewPager = fragment.requireView().findViewById<ViewPager2>(R.id.viewPager)
            assertEquals(expectedCount, viewPager.adapter?.itemCount)

            val dots = fragment.requireView().findViewById<LinearLayout>(R.id.dotsIndicator)
            assertEquals(expectedCount, dots.childCount)
        }
    }
}
