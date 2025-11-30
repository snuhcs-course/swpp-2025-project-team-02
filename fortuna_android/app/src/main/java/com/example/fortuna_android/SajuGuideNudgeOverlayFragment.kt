package com.example.fortuna_android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fortuna_android.databinding.FragmentSajuGuideNudgeOverlayBinding

/**
 * Saju Guide Nudge overlay fragment that appears during fortune generation
 * Shows Pokemon-style dialogue with mascot, guiding user to Saju Guide tab
 * Only appears once after first onboarding when fortune is generating
 */
class SajuGuideNudgeOverlayFragment : Fragment() {
    private var _binding: FragmentSajuGuideNudgeOverlayBinding? = null
    private val binding get() = _binding!!

    private var currentDialogueIndex = 0
    private val dialogues = listOf(
        "운세가 생성되는 동안\n사주 가이드를 둘러보세요!",
        "사주 가이드에서는 오행의 의미와\n사주팔자에 대해 알 수 있어요.",
        "지금 가이드를 확인해 볼까요?"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSajuGuideNudgeOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateDialogue()
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

        // "가이드 보기" button - navigate to Saju Guide with walkthrough
        binding.btnGoToGuide.setOnClickListener {
            markNudgeAsSeen()
            // Set flag to start walkthrough when SajuGuideFragment opens
            setWalkthroughFlag()
            navigateToSajuGuide()
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
        currentDialogueIndex++

        if (currentDialogueIndex < dialogues.size) {
            updateDialogue()
        }
        // On last dialogue, buttons are visible - user must click a button
    }

    private fun updateDialogue() {
        binding.tvDialogue.text = dialogues[currentDialogueIndex]

        // Show arrow indicator except on last dialogue (where buttons are shown)
        val isLastDialogue = currentDialogueIndex == dialogues.size - 1
        binding.tvArrow.visibility = if (isLastDialogue) View.GONE else View.VISIBLE

        // Show action buttons only on last dialogue
        binding.btnLater.visibility = if (isLastDialogue) View.VISIBLE else View.GONE
        binding.btnGoToGuide.visibility = if (isLastDialogue) View.VISIBLE else View.GONE
    }

    private fun markNudgeAsSeen() {
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_HAS_SEEN_NUDGE, true).apply()
        Log.d(TAG, "Saju Guide nudge marked as seen")
    }

    private fun setWalkthroughFlag() {
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("start_saju_walkthrough", true).apply()
        Log.d(TAG, "Walkthrough flag set")
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

    private fun navigateToSajuGuide() {
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

                // Navigate to Saju Guide tab using NavController
                val navHostFragment = mainActivity?.supportFragmentManager
                    ?.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
                navHostFragment?.navController?.navigate(R.id.sajuGuideFragment)

                Log.d(TAG, "Navigated to Saju Guide")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Saju Guide: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SajuGuideNudgeOverlay"
        const val PREF_KEY_HAS_SEEN_NUDGE = "has_seen_saju_guide_nudge"

        fun newInstance(): SajuGuideNudgeOverlayFragment {
            return SajuGuideNudgeOverlayFragment()
        }

        /**
         * Check if the nudge should be shown
         * @param context Context to access SharedPreferences
         * @return true if nudge should be shown (first time after onboarding)
         */
        fun shouldShowNudge(context: Context): Boolean {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val hasSeenNudge = prefs.getBoolean(PREF_KEY_HAS_SEEN_NUDGE, false)
            return !hasSeenNudge
        }
    }
}
