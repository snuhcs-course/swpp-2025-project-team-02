package com.example.fortuna_android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fortuna_android.databinding.FragmentTutorialOverlayBinding
import com.example.fortuna_android.ui.FortuneCardView

/**
 * Tutorial overlay fragment that shows Pokemon-style dialogue with mascot
 * Displays over the main screen with a dim background
 */
class TutorialOverlayFragment : Fragment() {
    private var _binding: FragmentTutorialOverlayBinding? = null
    private val binding get() = _binding!!

    @VisibleForTesting
    internal var currentDialogueIndex = 0

    @VisibleForTesting
    internal val dialogues = listOf(
        "모든 것은 오행이요,\n오행의 조화가 복을 의미합니다.",
        "가장 먼저 보이는 오행은\n오늘 당신에게 부족한 기운을 의미합니다!",
        "오늘의 운세 점수입니다\n부족한 기운을 수집해 점수를 올려보세요!",
        "오늘 부족한 오행에 대한\n사주 풀이를 확인할 수 있어요!",
        "오늘의 운을 열러 가볼까요?"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateDialogue()
    }

    private fun setupClickListeners() {
        // Close button - dismiss overlay
        binding.btnClose.setOnClickListener {
            dismissOverlay()
        }

        // Dialogue box click - advance to next dialogue
        binding.dialogueBox.setOnClickListener {
            advanceDialogue()
        }

        // Spotlight overlay click - advance to next dialogue
        binding.spotlightOverlay.setOnClickListener {
            advanceDialogue()
        }
    }

    private fun advanceDialogue() {
        val result = calculateDialogueAdvance(currentDialogueIndex, dialogues.size)
        if (result.shouldAdvance) {
            currentDialogueIndex = result.nextIndex
            updateDialogue()
        } else {
            navigateToARScreen()
        }
    }

    /**
     * Pure function to calculate dialogue advance state - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun calculateDialogueAdvance(currentIndex: Int, totalDialogues: Int): DialogueAdvanceResult {
        val nextIndex = currentIndex + 1
        return if (nextIndex < totalDialogues) {
            DialogueAdvanceResult(shouldAdvance = true, nextIndex = nextIndex)
        } else {
            DialogueAdvanceResult(shouldAdvance = false, nextIndex = currentIndex)
        }
    }

    /**
     * Data class for dialogue advance calculation result
     */
    @VisibleForTesting
    internal data class DialogueAdvanceResult(
        val shouldAdvance: Boolean,
        val nextIndex: Int
    )

    private fun updateDialogue() {
        binding.tvDialogue.text = dialogues[currentDialogueIndex]

        // Show arrow indicator for all dialogues
        binding.tvArrow.visibility = View.VISIBLE

        // Enable spotlight based on dialogue index
        val spotlightTarget = getSpotlightTarget(currentDialogueIndex)
        when (spotlightTarget) {
            SpotlightTarget.FORTUNE_CARD_ELEMENT -> highlightFortuneCardElement()
            SpotlightTarget.FORTUNE_SCORE -> highlightFortuneScore()
            SpotlightTarget.ELEMENT_BALANCE -> highlightElementBalance()
            SpotlightTarget.NONE -> binding.spotlightOverlay.clearSpotlight()
        }
    }

    /**
     * Pure function to determine which spotlight target to show - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun getSpotlightTarget(dialogueIndex: Int): SpotlightTarget {
        return when (dialogueIndex) {
            1 -> SpotlightTarget.FORTUNE_CARD_ELEMENT
            2 -> SpotlightTarget.FORTUNE_SCORE
            3 -> SpotlightTarget.ELEMENT_BALANCE
            else -> SpotlightTarget.NONE
        }
    }

    /**
     * Enum for spotlight target types
     */
    @VisibleForTesting
    internal enum class SpotlightTarget {
        NONE,
        FORTUNE_CARD_ELEMENT,
        FORTUNE_SCORE,
        ELEMENT_BALANCE
    }

    /**
     * Get the dialogue text for a given index - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun getDialogueForIndex(index: Int): String? {
        return if (index in dialogues.indices) dialogues[index] else null
    }

    /**
     * Highlight the fortune card's element character with circular spotlight
     */
    private fun highlightFortuneCardElement() {
        // Need to find the FortuneCardView in the parent activity
        val mainActivity = requireActivity() as? MainActivity
        if (mainActivity != null) {
            // Post to ensure the view is laid out before we get its location
            binding.root.post {
                try {
                    // Find the fortune card view by ID from MainActivity's layout
                    val fortuneCardView = mainActivity.findViewById<FortuneCardView>(
                        resources.getIdentifier("fortuneCardView", "id", requireContext().packageName)
                    )

                    if (fortuneCardView != null) {
                        // Find the element character TextView inside the fortune card
                        val elementCharView = fortuneCardView.findViewById<View>(
                            resources.getIdentifier("tvElementCharacter", "id", requireContext().packageName)
                        )

                        if (elementCharView != null) {
                            // Scroll to the element view in the background
                            scrollToView(elementCharView)

                            // Wait for scroll to finish, then set spotlight
                            // Post with delay to ensure smooth scroll animation completes
                            binding.root.postDelayed({
                                if (isAdded && _binding != null) {
                                    // Set spotlight with 60dp padding around the element
                                    binding.spotlightOverlay.setSpotlight(elementCharView, 60f * resources.displayMetrics.density)
                                }
                            }, 400) // 400ms delay for smooth scroll animation
                        } else {
                            // Fallback: clear spotlight if view not found
                            binding.spotlightOverlay.clearSpotlight()
                        }
                    } else {
                        // Fallback: clear spotlight if fortune card not found
                        binding.spotlightOverlay.clearSpotlight()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error highlighting fortune card element", e)
                    binding.spotlightOverlay.clearSpotlight()
                }
            }
        } else {
            // Fallback: clear spotlight if activity not found
            binding.spotlightOverlay.clearSpotlight()
        }
    }

    /**
     * Highlight the fortune score with circular spotlight
     */
    private fun highlightFortuneScore() {
        // Need to find the FortuneCardView in the parent activity
        val mainActivity = requireActivity() as? MainActivity
        if (mainActivity != null) {
            // Post to ensure the view is laid out before we get its location
            binding.root.post {
                try {
                    // Find the fortune card view by ID from MainActivity's layout
                    val fortuneCardView = mainActivity.findViewById<FortuneCardView>(
                        resources.getIdentifier("fortuneCardView", "id", requireContext().packageName)
                    )

                    if (fortuneCardView != null) {
                        // Find the score TextView (tvOverallFortune) inside the fortune card
                        val scoreView = fortuneCardView.findViewById<View>(
                            resources.getIdentifier("tvOverallFortune", "id", requireContext().packageName)
                        )

                        if (scoreView != null) {
                            // Scroll to the score view in the background
                            scrollToView(scoreView)

                            // Wait for scroll to finish, then set spotlight
                            // Post with delay to ensure smooth scroll animation completes
                            binding.root.postDelayed({
                                if (isAdded && _binding != null) {
                                    // Set spotlight with 60dp padding around the score
                                    binding.spotlightOverlay.setSpotlight(scoreView, 60f * resources.displayMetrics.density)
                                }
                            }, 400) // 400ms delay for smooth scroll animation
                        } else {
                            // Fallback: clear spotlight if view not found
                            binding.spotlightOverlay.clearSpotlight()
                        }
                    } else {
                        // Fallback: clear spotlight if fortune card not found
                        binding.spotlightOverlay.clearSpotlight()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error highlighting fortune score", e)
                    binding.spotlightOverlay.clearSpotlight()
                }
            }
        } else {
            // Fallback: clear spotlight if activity not found
            binding.spotlightOverlay.clearSpotlight()
        }
    }

    /**
     * Highlight the element balance section with circular spotlight
     */
    private fun highlightElementBalance() {
        // Need to find the FortuneCardView in the parent activity
        val mainActivity = requireActivity() as? MainActivity
        if (mainActivity != null) {
            // Post to ensure the view is laid out before we get its location
            binding.root.post {
                try {
                    // Find the fortune card view by ID from MainActivity's layout
                    val fortuneCardView = mainActivity.findViewById<FortuneCardView>(
                        resources.getIdentifier("fortuneCardView", "id", requireContext().packageName)
                    )

                    if (fortuneCardView != null) {
                        // Find the element balance layout inside the fortune card
                        val elementBalanceView = fortuneCardView.findViewById<View>(
                            resources.getIdentifier("layoutElementBalance", "id", requireContext().packageName)
                        )

                        if (elementBalanceView != null) {
                            // Scroll to the element balance view in the background
                            scrollToView(elementBalanceView)

                            // Wait for scroll to finish, then set spotlight
                            // Post with delay to ensure smooth scroll animation completes
                            binding.root.postDelayed({
                                if (isAdded && _binding != null) {
                                    // Set spotlight with 40dp padding around the element balance section
                                    binding.spotlightOverlay.setSpotlight(elementBalanceView, 40f * resources.displayMetrics.density)
                                }
                            }, 400) // 400ms delay for smooth scroll animation
                        } else {
                            // Fallback: clear spotlight if view not found
                            binding.spotlightOverlay.clearSpotlight()
                        }
                    } else {
                        // Fallback: clear spotlight if fortune card not found
                        binding.spotlightOverlay.clearSpotlight()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error highlighting element balance", e)
                    binding.spotlightOverlay.clearSpotlight()
                }
            }
        } else {
            // Fallback: clear spotlight if activity not found
            binding.spotlightOverlay.clearSpotlight()
        }
    }

    /**
     * Scroll the background ScrollView to bring the target view into view
     */
    private fun scrollToView(targetView: View) {
        try {
            val mainActivity = requireActivity() as? MainActivity ?: return

            // Find the ScrollView in fragment_today_fortune.xml by ID
            val scrollViewId = resources.getIdentifier("scrollViewTodayFortune", "id", requireContext().packageName)
            val scrollView = mainActivity.findViewById<android.widget.ScrollView>(scrollViewId)

            if (scrollView != null) {
                // Get the target view's position on screen
                val targetLocation = IntArray(2)
                targetView.getLocationOnScreen(targetLocation)

                // Get the ScrollView's position on screen
                val scrollViewLocation = IntArray(2)
                scrollView.getLocationOnScreen(scrollViewLocation)

                // Calculate the target view's Y position relative to ScrollView's top
                // We need to add the current scroll position to get the absolute position within ScrollView content
                val currentScrollY = scrollView.scrollY
                val targetRelativeY = targetLocation[1] - scrollViewLocation[1] + currentScrollY

                // Calculate scroll position to center the target view on screen
                val screenHeight = resources.displayMetrics.heightPixels
                val centerOffset = (screenHeight / 2) - (targetView.height / 2)
                val scrollY = targetRelativeY - centerOffset

                // Smooth scroll to the calculated position
                val finalScrollY = scrollY.coerceAtLeast(0)
                scrollView.smoothScrollTo(0, finalScrollY)
                Log.d(TAG, "Scrolling to view - currentScroll: $currentScrollY, targetRelative: $targetRelativeY, finalScroll: $finalScrollY")
            } else {
                Log.w(TAG, "ScrollView not found, cannot scroll to target view")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling to view", e)
        }
    }

    private fun dismissOverlay() {
        val fragmentManager = requireActivity().supportFragmentManager
        val canPop = !fragmentManager.isStateSaved &&
            fragmentManager.popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        if (!canPop) {
            Log.w(TAG, "Cannot pop back stack (state saved or not in stack), force removing fragment")
            fragmentManager.beginTransaction()
                .remove(this)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
        } else {
            Log.d(TAG, "Overlay dismissed via popBackStack")
        }
    }

    private fun navigateToARScreen() {
        // Mark home tutorial as seen
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_home_tutorial", true).apply()
        Log.d(TAG, "Home tutorial marked as seen")

        // Get MainActivity and Intent before dismissing
        val mainActivity = requireActivity() as? MainActivity
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Dismiss overlay first, and only navigate if dismissal is successful
        try {
            val fragmentManager = requireActivity().supportFragmentManager
            if (isAdded && !requireActivity().isFinishing) {
                // If state is already saved, skip pop and force-remove allowing state loss
                val overlayDismissed = if (!fragmentManager.isStateSaved) {
                    Log.d(TAG, "Attempting normal popBackStack with TAG")
                    fragmentManager.popBackStackImmediate(
                        TAG,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                } else {
                    Log.w(TAG, "Activity state is saved, will use commitAllowingStateLoss")
                    false
                }

                if (!overlayDismissed) {
                    Log.w(TAG, "PopBackStack failed or skipped, force removing fragment")
                    fragmentManager.beginTransaction()
                        .remove(this)
                        .commitAllowingStateLoss()
                    fragmentManager.executePendingTransactions()
                    Log.d(TAG, "Fragment removed via commitAllowingStateLoss")
                }

                // Navigate to AR screen AFTER dismissal
                mainActivity?.startActivity(intent)
                Log.d(TAG, "Tutorial overlay dismissed and navigating to AR screen")
            } else {
                Log.w(TAG, "Fragment not added or activity finishing, skipping dismissal and navigation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay: ${e.message}", e)
            // Still try to navigate even if dismissal failed
            try {
                mainActivity?.startActivity(intent)
                Log.w(TAG, "Attempted AR navigation despite dismissal error")
            } catch (navException: Exception) {
                Log.e(TAG, "Navigation also failed: ${navException.message}", navException)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TutorialOverlayFragment"

        fun newInstance(): TutorialOverlayFragment {
            return TutorialOverlayFragment()
        }
    }
}
