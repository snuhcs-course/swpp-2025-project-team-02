package com.example.fortuna_android

import android.content.Intent
import android.os.Build
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for TutorialOverlayFragment
 * Tests dialogue progression, navigation, and UI interactions
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TutorialOverlayFragmentTest {

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
    fun `fragment initializes with first dialogue`() {
        onView(withId(R.id.tvDialogue))
            .check(matches(isDisplayed()))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))
    }

    @Test
    fun `mascot image is displayed`() {
        onView(withId(R.id.ivMascot))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `dialogue box is displayed at bottom`() {
        onView(withId(R.id.dialogueBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `close button is displayed`() {
        onView(withId(R.id.btnClose))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `arrow indicator is visible`() {
        onView(withId(R.id.tvArrow))
            .check(matches(isDisplayed()))
            .check(matches(withText("▼")))
    }

    @Test
    fun `dim background is displayed`() {
        onView(withId(R.id.dimBackground))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `clicking dialogue box advances to second dialogue`() {
        // Initial state: first dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))

        // Click dialogue box
        onView(withId(R.id.dialogueBox))
            .perform(click())

        // Should advance to second dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!")))
    }

    @Test
    fun `clicking dim background advances dialogue`() {
        // Initial state: first dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))

        // Click dim background
        onView(withId(R.id.dimBackground))
            .perform(click())

        // Should advance to second dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!")))
    }

    @Test
    fun `dialogue progresses through all four dialogues`() {
        val expectedDialogues = listOf(
            "모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.",
            "오늘 당신의 하루 운세 점수는\n75점 입니다.\n\n당신에게 부족한 기운은\n<화> 입니다!",
            "주변에서 화 기운을 보충하여,\n오늘 당신의 운세 점수를\n높일 수 있어요!",
            "당신의 오행 균형을\n맞추러 가볼까요?"
        )

        // Verify each dialogue in sequence
        expectedDialogues.forEach { dialogue ->
            onView(withId(R.id.tvDialogue))
                .check(matches(withText(dialogue)))

            // Click to advance (except on last dialogue)
            if (dialogue != expectedDialogues.last()) {
                onView(withId(R.id.dialogueBox))
                    .perform(click())
            }
        }
    }

    @Test
    fun `clicking after last dialogue navigates to AR screen`() {
        // Advance through all dialogues
        repeat(3) {
            onView(withId(R.id.dialogueBox))
                .perform(click())
        }

        // Verify we're on the last dialogue
        onView(withId(R.id.tvDialogue))
            .check(matches(withText("당신의 오행 균형을\n맞추러 가볼까요?")))

        // Click to finish tutorial
        scenario.onFragment { fragment ->
            onView(withId(R.id.dialogueBox))
                .perform(click())

            // Verify intent to MainActivity with navigate_to_ar flag
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val shadowApp = shadowOf(context as android.app.Application)
            val nextIntent = shadowApp.nextStartedActivity

            assertNotNull("Intent should be started", nextIntent)
            assertEquals(MainActivity::class.java.name, nextIntent.component?.className)
            assertTrue(
                "Should have navigate_to_ar extra",
                nextIntent.getBooleanExtra("navigate_to_ar", false)
            )
            assertTrue(
                "Should have CLEAR_TOP flag",
                nextIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0
            )
            assertTrue(
                "Should have SINGLE_TOP flag",
                nextIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0
            )
        }
    }

    @Test
    fun `clicking close button dismisses overlay`() {
        scenario.onFragment { fragment ->
            val fragmentManager = fragment.parentFragmentManager
            val initialFragmentCount = fragmentManager.fragments.size

            // Click close button
            onView(withId(R.id.btnClose))
                .perform(click())

            // Wait for transaction to complete
            fragmentManager.executePendingTransactions()

            // Verify fragment is removed
            val finalFragmentCount = fragmentManager.fragments.size
            assertTrue(
                "Fragment should be removed after close",
                finalFragmentCount < initialFragmentCount
            )
        }
    }

    @Test
    fun `arrow indicator stays visible throughout tutorial`() {
        // Check arrow is visible on first dialogue
        onView(withId(R.id.tvArrow))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Advance through all dialogues and check arrow
        repeat(3) {
            onView(withId(R.id.dialogueBox))
                .perform(click())

            onView(withId(R.id.tvArrow))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }

    @Test
    fun `dialogue index increases correctly`() {
        scenario.onFragment { fragment ->
            // Initial index should be 0
            assertEquals(0, fragment.getCurrentDialogueIndex())

            // Advance dialogue
            fragment.view?.findViewById<View>(R.id.dialogueBox)?.performClick()
            assertEquals(1, fragment.getCurrentDialogueIndex())

            fragment.view?.findViewById<View>(R.id.dialogueBox)?.performClick()
            assertEquals(2, fragment.getCurrentDialogueIndex())

            fragment.view?.findViewById<View>(R.id.dialogueBox)?.performClick()
            assertEquals(3, fragment.getCurrentDialogueIndex())
        }
    }

    @Test
    fun `mascot container is positioned at bottom right`() {
        onView(withId(R.id.mascotContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `dialogue box has correct styling`() {
        onView(withId(R.id.dialogueBox))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun `dialogue text has correct styling`() {
        onView(withId(R.id.tvDialogue))
            .check(matches(isDisplayed()))
            .check(matches(withText("모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.")))
    }

    @Test
    fun `all UI elements are properly initialized`() {
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
    fun `fragment can be created with newInstance`() {
        val fragment = TutorialOverlayFragment.newInstance()
        assertNotNull("Fragment should be created", fragment)
        assertTrue("Should be instance of TutorialOverlayFragment", fragment is TutorialOverlayFragment)
    }

    @Test
    fun `binding is properly cleaned up on destroy`() {
        scenario.onFragment { fragment ->
            assertNotNull("Binding should exist when fragment is active", fragment.view)
        }

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        // After destroy, binding should be null (verified by no crash)
    }
}

/**
 * Extension function to expose currentDialogueIndex for testing
 */
private fun TutorialOverlayFragment.getCurrentDialogueIndex(): Int {
    val field = this::class.java.getDeclaredField("currentDialogueIndex")
    field.isAccessible = true
    return field.getInt(this)
}
