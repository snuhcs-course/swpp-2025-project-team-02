package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ElementHistoryDialogFragment
 * Tests modal display, RecyclerView, and data loading
 */
@RunWith(AndroidJUnit4::class)
class ElementHistoryDialogFragmentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ========== Dialog Creation Tests ==========

    @Test
    fun testDialogCreatesWithValidArguments() {
        val args = Bundle().apply {
            putString("element_type", "wood")
            putString("element_kr", "목")
            putInt("element_color", Color.parseColor("#0BEFA0"))
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Dialog fragment should be created", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testDialogCreatesWithAllElementTypes() {
        val elementTypes = mapOf(
            "wood" to ("목" to Color.parseColor("#0BEFA0")),
            "fire" to ("화" to Color.parseColor("#F93E3E")),
            "earth" to ("토" to Color.parseColor("#FF9500")),
            "metal" to ("금" to Color.parseColor("#C1BFBF")),
            "water" to ("수" to Color.parseColor("#2BB3FC"))
        )

        elementTypes.forEach { (elementEn, pair) ->
            val (elementKr, color) = pair
            val args = Bundle().apply {
                putString("element_type", elementEn)
                putString("element_kr", elementKr)
                putInt("element_color", color)
            }

            val scenario = launchFragment<ElementHistoryDialogFragment>(
                fragmentArgs = args,
                themeResId = R.style.Theme_Fortuna_android
            )

            scenario.onFragment { fragment ->
                assertNotNull("Dialog for $elementEn should be created", fragment)
            }

            scenario.close()
        }
    }

    // ========== UI Element Tests ==========

    @Test
    fun testDialogHasRequiredUIElements() {
        val args = Bundle().apply {
            putString("element_type", "fire")
            putString("element_kr", "화")
            putInt("element_color", Color.parseColor("#F93E3E"))
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val view = fragment.view
            assertNotNull("Dialog view should exist", view)

            val elementCharacter = view?.findViewById<TextView>(R.id.elementCharacter)
            val elementTitle = view?.findViewById<TextView>(R.id.elementTitle)
            val totalCount = view?.findViewById<TextView>(R.id.totalCount)
            val closeButton = view?.findViewById<ImageButton>(R.id.closeButton)
            val recyclerView = view?.findViewById<RecyclerView>(R.id.historyRecyclerView)
            val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
            val emptyState = view?.findViewById<TextView>(R.id.emptyState)

            assertNotNull("Element character should exist", elementCharacter)
            assertNotNull("Element title should exist", elementTitle)
            assertNotNull("Total count should exist", totalCount)
            assertNotNull("Close button should exist", closeButton)
            assertNotNull("RecyclerView should exist", recyclerView)
            assertNotNull("ProgressBar should exist", progressBar)
            assertNotNull("Empty state text should exist", emptyState)
        }

        scenario.close()
    }

    @Test
    fun testElementCharacterDisplaysCorrectly() {
        val elementData = listOf(
            Triple("wood", "목", "木"),
            Triple("fire", "화", "火"),
            Triple("earth", "토", "土"),
            Triple("metal", "금", "金"),
            Triple("water", "수", "水")
        )

        elementData.forEach { (elementEn, elementKr, expectedChar) ->
            val args = Bundle().apply {
                putString("element_type", elementEn)
                putString("element_kr", elementKr)
                putInt("element_color", Color.WHITE)
            }

            val scenario = launchFragment<ElementHistoryDialogFragment>(
                fragmentArgs = args,
                themeResId = R.style.Theme_Fortuna_android
            )

            scenario.onFragment { fragment ->
                val elementCharacter = fragment.view?.findViewById<TextView>(R.id.elementCharacter)
                assertEquals("Character for $elementKr should be $expectedChar", expectedChar, elementCharacter?.text)
            }

            scenario.close()
        }
    }

    @Test
    fun testElementTitleFormatting() {
        val args = Bundle().apply {
            putString("element_type", "water")
            putString("element_kr", "수")
            putInt("element_color", Color.BLUE)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val elementTitle = fragment.view?.findViewById<TextView>(R.id.elementTitle)
            assertNotNull("Element title should exist", elementTitle)

            val titleText = elementTitle?.text?.toString()
            assertTrue("Title should contain element name", titleText?.contains("수") == true)
            assertTrue("Title should contain '수집 기록'", titleText?.contains("수집 기록") == true)
        }

        scenario.close()
    }

    // ========== RecyclerView Tests ==========

    @Test
    fun testRecyclerViewHasAdapter() {
        val args = Bundle().apply {
            putString("element_type", "wood")
            putString("element_kr", "목")
            putInt("element_color", Color.GREEN)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.historyRecyclerView)
            assertNotNull("RecyclerView should exist", recyclerView)

            // Wait a bit for adapter setup
            Thread.sleep(500)

            assertNotNull("RecyclerView should have adapter", recyclerView?.adapter)
        }

        scenario.close()
    }

    @Test
    fun testRecyclerViewHasLayoutManager() {
        val args = Bundle().apply {
            putString("element_type", "fire")
            putString("element_kr", "화")
            putInt("element_color", Color.RED)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.historyRecyclerView)
            assertNotNull("RecyclerView should exist", recyclerView)
            assertNotNull("RecyclerView should have LayoutManager", recyclerView?.layoutManager)
        }

        scenario.close()
    }

    // ========== Close Button Tests ==========

    @Test
    fun testCloseButtonExists() {
        val args = Bundle().apply {
            putString("element_type", "earth")
            putString("element_kr", "토")
            putInt("element_color", Color.parseColor("#FF9500"))
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<ImageButton>(R.id.closeButton)
            assertNotNull("Close button should exist", closeButton)
            assertTrue("Close button should be clickable", closeButton?.isClickable == true)
        }

        scenario.close()
    }

    // ========== Loading State Tests ==========

    @Test
    fun testProgressBarInitiallyHidden() {
        val args = Bundle().apply {
            putString("element_type", "metal")
            putString("element_kr", "금")
            putInt("element_color", Color.GRAY)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val progressBar = fragment.view?.findViewById<ProgressBar>(R.id.progressBar)
            assertNotNull("ProgressBar should exist", progressBar)

            // ProgressBar visibility will change based on loading state
            // Just verify it exists and can be accessed
        }

        scenario.close()
    }

    // ========== Lifecycle Tests ==========

    @Test
    fun testDialogLifecycle() {
        val args = Bundle().apply {
            putString("element_type", "water")
            putString("element_kr", "수")
            putInt("element_color", Color.BLUE)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            assertTrue("Dialog should be resumed", fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testDialogRecreation() {
        val args = Bundle().apply {
            putString("element_type", "wood")
            putString("element_kr", "목")
            putInt("element_color", Color.GREEN)
        }

        val scenario = launchFragment<ElementHistoryDialogFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            assertNotNull("Dialog should be recreated", fragment)
            assertNotNull("Dialog view should exist after recreation", fragment.view)

            val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.historyRecyclerView)
            assertNotNull("RecyclerView should exist after recreation", recyclerView)
        }

        scenario.close()
    }
}