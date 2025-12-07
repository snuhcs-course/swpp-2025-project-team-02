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
import com.example.fortuna_android.databinding.FragmentSajuGuideNudgeOverlayBinding

/**
 * Element Collection Nudge overlay fragment that appears after fortune is generated
 * Shows Pokemon-style dialogue with mascot, guiding user to AR screen for element collection
 * Only appears once after user has seen the Saju Guide nudge and fortune is ready
 */
class ElementCollectionNudgeOverlayFragment : Fragment() {
    private var _binding: FragmentSajuGuideNudgeOverlayBinding? = null
    private val binding get() = _binding!!

    @VisibleForTesting
    internal var currentDialogueIndex = 0

    @VisibleForTesting
    internal val dialogues = listOf(
        "운세가 준비되었어요!",
        "오늘의 기운이 부족하네요.\n개운을 위해 기운을 보충해볼까요?",
        "AR 카메라로 주변에서\n부족한 오행을 찾아보세요!"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Reuse the same layout as SajuGuideNudgeOverlayFragment
        _binding = FragmentSajuGuideNudgeOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateDialogue()

        // Change button text for this nudge type
        binding.btnGoToGuide.text = "기운 보충하기"
    }

    private fun setupClickListeners() {
        // Close button - dismiss overlay without navigating
        binding.btnClose.setOnClickListener {
            markNudgeAsSeen()
            dismissOverlay()
        }

        // "나중에" button - dismiss overlay without navigating
        binding.btnLater.setOnClickListener {
            markNudgeAsSeen()
            dismissOverlay()
        }

        // "기운 보충하기" button - navigate to AR screen
        binding.btnGoToGuide.setOnClickListener {
            markNudgeAsSeen()
            navigateToAR()
        }

        // Dialogue box click - advance to next dialogue
        binding.dialogueBox.setOnClickListener {
            advanceDialogue()
        }

        // Dim overlay click - advance to next dialogue
        binding.dimOverlay.setOnClickListener {
            advanceDialogue()
        }
    }

    private fun advanceDialogue() {
        val result = calculateDialogueAdvance(currentDialogueIndex, dialogues.size)
        if (result.shouldAdvance) {
            currentDialogueIndex = result.nextIndex
            updateDialogue()
        }
        // On last dialogue, buttons are visible - user must click a button
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

        val uiState = calculateDialogueUIState(currentDialogueIndex, dialogues.size)
        binding.tvArrow.visibility = if (uiState.showArrow) View.VISIBLE else View.GONE
        binding.btnLater.visibility = if (uiState.showButtons) View.VISIBLE else View.GONE
        binding.btnGoToGuide.visibility = if (uiState.showButtons) View.VISIBLE else View.GONE
    }

    /**
     * Pure function to calculate UI state for dialogue - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun calculateDialogueUIState(currentIndex: Int, totalDialogues: Int): DialogueUIState {
        val isLastDialogue = currentIndex == totalDialogues - 1
        return DialogueUIState(
            isLastDialogue = isLastDialogue,
            showArrow = !isLastDialogue,
            showButtons = isLastDialogue
        )
    }

    /**
     * Data class for dialogue UI state
     */
    @VisibleForTesting
    internal data class DialogueUIState(
        val isLastDialogue: Boolean,
        val showArrow: Boolean,
        val showButtons: Boolean
    )

    /**
     * Get the dialogue text for a given index - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun getDialogueForIndex(index: Int): String? {
        return if (index in dialogues.indices) dialogues[index] else null
    }

    private fun markNudgeAsSeen() {
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_HAS_SEEN_NUDGE, true).apply()
        Log.d(TAG, "Element Collection nudge marked as seen")
    }

    private fun dismissOverlay() {
        val fragmentManager = requireActivity().supportFragmentManager
        val canPop = !fragmentManager.isStateSaved &&
            fragmentManager.popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        if (!canPop) {
            Log.w(TAG, "Cannot pop back stack, force removing fragment")
            fragmentManager.beginTransaction()
                .remove(this)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
        } else {
            Log.d(TAG, "Nudge overlay dismissed via popBackStack")
        }
    }

    private fun navigateToAR() {
        val mainActivity = requireActivity() as? MainActivity

        // Dismiss overlay first
        try {
            val fragmentManager = requireActivity().supportFragmentManager
            if (isAdded && !requireActivity().isFinishing) {
                val overlayDismissed = if (!fragmentManager.isStateSaved) {
                    fragmentManager.popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                } else {
                    false
                }

                if (!overlayDismissed) {
                    fragmentManager.beginTransaction()
                        .remove(this)
                        .commitAllowingStateLoss()
                    fragmentManager.executePendingTransactions()
                }

                // Navigate to AR screen using Intent (same as TutorialOverlayFragment)
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    putExtra("navigate_to_ar", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                mainActivity?.startActivity(intent)

                Log.d(TAG, "Navigated to AR screen")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to AR: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ElementCollectionNudge"
        const val PREF_KEY_HAS_SEEN_NUDGE = "has_seen_element_collection_nudge"

        fun newInstance(): ElementCollectionNudgeOverlayFragment {
            return ElementCollectionNudgeOverlayFragment()
        }

        /**
         * Check if the element collection nudge should be shown
         * @param context Context to access SharedPreferences
         * @return true if nudge should be shown (after seeing Saju Guide nudge)
         */
        fun shouldShowNudge(context: Context): Boolean {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val hasSeenElementNudge = prefs.getBoolean(PREF_KEY_HAS_SEEN_NUDGE, false)
            val hasSeenSajuGuideNudge = prefs.getBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            val wentToSajuGuide = prefs.getBoolean(PREF_KEY_WENT_TO_SAJU_GUIDE, false)

            // Show if: saw saju guide nudge AND went to saju guide AND hasn't seen element nudge
            return hasSeenSajuGuideNudge && wentToSajuGuide && !hasSeenElementNudge
        }

        // Flag to track if user actually went to Saju Guide (clicked "가이드 보기")
        const val PREF_KEY_WENT_TO_SAJU_GUIDE = "went_to_saju_guide_from_nudge"

        /**
         * Mark that user went to Saju Guide from the nudge
         */
        fun markWentToSajuGuide(context: Context) {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_KEY_WENT_TO_SAJU_GUIDE, true).apply()
        }
    }
}
