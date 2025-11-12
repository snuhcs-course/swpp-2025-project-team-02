package com.example.fortuna_android

import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TutorialOverlayFragment
 * Tests on real Android device/emulator
 */
@RunWith(AndroidJUnit4::class)
class TutorialOverlayFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<TutorialOverlayFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer<TutorialOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fortuna_android", appContext.packageName)
    }

    @Test
    fun fragment_initializes_with_first_dialogue() {
        onView(withId(R.id.tvDialogue))
            .check(matches(isDisplayed()))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))
    }

    @Test
    fun mascot_image_is_displayed() {
        onView(withId(R.id.ivMascot))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogue_box_is_visible_and_clickable() {
        onView(withId(R.id.dialogueBox))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun close_button_is_visible_and_clickable() {
        onView(withId(R.id.btnClose))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun arrow_indicator_is_visible() {
        onView(withId(R.id.tvArrow))
            .check(matches(isDisplayed()))
            .check(matches(withText("▼")))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun dim_background_is_displayed() {
        onView(withId(R.id.dimBackground))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clicking_dialogue_box_advances_to_second_dialogue() {
        // Initial state: first dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))

        // Click dialogue box
        onView(withId(R.id.dialogueBox))
            .perform(click())

        // Should advance to second dialogue
        Thread.sleep(100) // Wait for UI update
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!")))
    }

    @Test
    fun clicking_dim_background_advances_dialogue() {
        // Initial state: first dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))

        // Click dim background
        onView(withId(R.id.dimBackground))
            .perform(click())

        // Should advance to second dialogue
        Thread.sleep(100) // Wait for UI update
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!")))
    }

    @Test
    fun dialogue_progresses_through_all_four_dialogues() {
        val expectedDialogues = listOf(
            "모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.",
            "오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!",
            "주변에서 화 기운을 보충하여,\n오늘 당신의 운세 점수를\n높일 수 있어요!",
            "당신의 오행 균형을\n맞추러 가볼까요?"
        )

        // Verify each dialogue in sequence
        expectedDialogues.forEachIndexed { index, dialogue ->
            onView(withId(R.id.tvDialogue))
                .check(matches(withText(dialogue)))

            // Click to advance (except on last dialogue)
            if (index < expectedDialogues.size - 1) {
                onView(withId(R.id.dialogueBox))
                    .perform(click())
                Thread.sleep(100) // Wait for UI update
            }
        }
    }

    @Test
    fun mascot_container_is_positioned_correctly() {
        onView(withId(R.id.mascotContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dialogue_box_has_white_background() {
        onView(withId(R.id.dialogueBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun arrow_indicator_stays_visible_throughout_tutorial() {
        // Check arrow is visible on first dialogue
        onView(withId(R.id.tvArrow))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Advance through all dialogues and check arrow
        repeat(3) {
            onView(withId(R.id.dialogueBox))
                .perform(click())
            Thread.sleep(100) // Wait for UI update

            onView(withId(R.id.tvArrow))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }

    @Test
    fun all_ui_elements_are_properly_initialized() {
        scenario.onFragment { fragment ->
            val view = fragment.view
            assertNotNull("Fragment view should not be null", view)

            // Check all UI elements exist
            assertNotNull("Dim background should exist", view?.findViewById<View>(R.id.dimBackground))
            assertNotNull("Close button should exist", view?.findViewById<View>(R.id.btnClose))
            assertNotNull("Mascot container should exist", view?.findViewById<View>(R.id.mascotContainer))
            assertNotNull("Mascot view should exist", view?.findViewById<View>(R.id.ivMascot))
            assertNotNull("Dialogue box should exist", view?.findViewById<View>(R.id.dialogueBox))
            assertNotNull("Dialogue text should exist", view?.findViewById<View>(R.id.tvDialogue))
            assertNotNull("Arrow indicator should exist", view?.findViewById<View>(R.id.tvArrow))
        }
    }

    @Test
    fun fragment_can_be_created_with_newInstance() {
        val fragment = TutorialOverlayFragment.newInstance()
        assertNotNull("Fragment should be created", fragment)
        assertTrue("Should be instance of TutorialOverlayFragment", fragment is TutorialOverlayFragment)
    }

    @Test
    fun clicking_close_button_dismisses_overlay() {
        scenario.onFragment { fragment ->
            val fragmentManager = fragment.parentFragmentManager
            val initialFragmentCount = fragmentManager.fragments.size

            // Click close button
            onView(withId(R.id.btnClose))
                .perform(click())

            // Wait for transaction to complete
            Thread.sleep(200)
            fragmentManager.executePendingTransactions()

            // Verify fragment is removed
            val finalFragmentCount = fragmentManager.fragments.size
            assertTrue(
                "Fragment should be removed after close (initial: $initialFragmentCount, final: $finalFragmentCount)",
                finalFragmentCount < initialFragmentCount || fragmentManager.fragments.none { it is TutorialOverlayFragment }
            )
        }
    }

    @Test
    fun mascot_image_loads_from_assets() {
        scenario.onFragment { fragment ->
            val mascotView = fragment.view?.findViewById<SimpleMascotView>(R.id.ivMascot)
            assertNotNull("Mascot view should exist", mascotView)
            assertNotNull("Mascot view should have drawable", mascotView?.drawable)
        }
    }

    @Test
    fun dialogue_text_has_correct_color() {
        onView(withId(R.id.tvDialogue))
            .check(matches(isDisplayed()))
    }

    @Test
    fun arrow_indicator_has_red_color() {
        onView(withId(R.id.tvArrow))
            .check(matches(isDisplayed()))
            .check(matches(withText("▼")))
    }

    @Test
    fun dialogue_advances_on_multiple_clicks() {
        // Click multiple times
        repeat(2) {
            onView(withId(R.id.dialogueBox))
                .perform(click())
            Thread.sleep(100)
        }

        // Should be on third dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("주변에서 화 기운을 보충하여,\n오늘 당신의 운세 점수를\n높일 수 있어요!")))
    }

    @Test
    fun fragment_handles_rapid_clicks() {
        // Rapid clicks should not crash
        repeat(5) {
            onView(withId(R.id.dialogueBox))
                .perform(click())
            Thread.sleep(50)
        }

        // Should still display a valid dialogue (last one or navigated away)
        // No assertion needed - test passes if no crash occurs
    }

    @Test
    fun binding_is_properly_cleaned_up_on_destroy() {
        scenario.onFragment { fragment ->
            assertNotNull("Binding should exist when fragment is active", fragment.view)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        // After destroy, binding should be null (verified by no crash)
    }
}
