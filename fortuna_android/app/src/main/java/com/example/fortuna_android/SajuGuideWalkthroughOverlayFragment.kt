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
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.databinding.FragmentSajuGuideNudgeOverlayBinding

/**
 * Saju Guide Walkthrough overlay that guides users through each page of the Saju Guide
 * Shows Pokemon-style dialogue for each page and advances ViewPager when tapped
 * After completing all pages, navigates back to Home and shows Element Collection nudge
 */
class SajuGuideWalkthroughOverlayFragment : Fragment() {
    private var _binding: FragmentSajuGuideNudgeOverlayBinding? = null
    private val binding get() = _binding!!

    @VisibleForTesting
    internal var currentPageIndex = 0

    @VisibleForTesting
    internal val totalPages = 6

    // Dialogues for each page of the Saju Guide
    @VisibleForTesting
    internal val dialogues = listOf(
        "사주는 태어난 연, 월, 일, 시를\n2행 4열로 나타낸 것이에요.",           // Page 1
        "일주(日柱)가 바로 '나'를 나타내요.\n일간이 나의 속성을 결정해요!",        // Page 2
        "천간 10개와 지지 12개가\n조합되어 60갑자가 만들어져요.",             // Page 3
        "오행은 목, 화, 토, 금, 수\n5가지 속성의 순환이에요.",               // Page 4
        "사주팔자와 오늘의 운기를 조합해\n당신만의 운세가 계산돼요!",            // Page 5
        "개운은 부족한 오행을 보충해\n운세의 균형을 맞추는 거예요!"             // Page 6
    )

    // Reference to the ViewPager in SajuGuideFragment
    private var viewPager: ViewPager2? = null

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

        // Make overlay transparent so user can read actual guide content
        binding.dimOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        // Hide mascot during walkthrough - only show dialogue box
        binding.mascotContainer.visibility = View.GONE

        // Find the ViewPager from SajuGuideFragment
        findViewPager()

        setupClickListeners()
        updateDialogue()

        // Hide action buttons (we'll show them only on last page)
        binding.btnLater.visibility = View.GONE
        binding.btnGoToGuide.visibility = View.GONE
    }

    private fun findViewPager() {
        try {
            val mainActivity = requireActivity() as? MainActivity
            if (mainActivity != null) {
                // Find ViewPager by ID from the nav host fragment
                val navHostFragment = mainActivity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)

                // Find SajuGuideFragment within nav host
                val sajuGuideFragment = navHostFragment?.childFragmentManager?.fragments?.find {
                    it is com.example.fortuna_android.ui.SajuGuideFragment
                }

                if (sajuGuideFragment != null) {
                    // Find ViewPager2 within SajuGuideFragment
                    viewPager = sajuGuideFragment.view?.findViewById(R.id.viewPager)
                    Log.d(TAG, "ViewPager found: ${viewPager != null}")

                    // Set up page change callback to sync dialogue with page
                    viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            if (position != currentPageIndex && position < totalPages) {
                                currentPageIndex = position
                                updateDialogue()
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding ViewPager", e)
        }
    }

    private fun setupClickListeners() {
        // Close button - dismiss overlay and go back to Home
        binding.btnClose.setOnClickListener {
            markWalkthroughAsSeen()
            navigateBackToHome()
        }

        // Dialogue box click - advance to next page
        binding.dialogueBox.setOnClickListener {
            advanceToNextPage()
        }

        // Dim overlay click - advance to next page
        binding.dimOverlay.setOnClickListener {
            advanceToNextPage()
        }

        // "나중에" button - no longer used in walkthrough
        binding.btnLater.setOnClickListener {
            markWalkthroughAsSeen()
            navigateBackToHome()
        }

        // "확인" button (shown on last page) - just go back to Home
        // User is guided to use "오늘의 기운 보충하러 가기" button on home screen
        binding.btnGoToGuide.setOnClickListener {
            markWalkthroughAsSeen()
            navigateBackToHome()
        }
    }

    private fun advanceToNextPage() {
        val result = calculateNextPageState(currentPageIndex, totalPages)
        if (result.shouldAdvance) {
            currentPageIndex = result.nextPageIndex
            viewPager?.setCurrentItem(currentPageIndex, true)
            updateDialogue()
        } else {
            showCompletionButtons()
        }
    }

    /**
     * Pure function to calculate next page state - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun calculateNextPageState(currentIndex: Int, total: Int): PageAdvanceResult {
        return if (currentIndex < total - 1) {
            PageAdvanceResult(shouldAdvance = true, nextPageIndex = currentIndex + 1)
        } else {
            PageAdvanceResult(shouldAdvance = false, nextPageIndex = currentIndex)
        }
    }

    /**
     * Data class for page advance calculation result
     */
    @VisibleForTesting
    internal data class PageAdvanceResult(
        val shouldAdvance: Boolean,
        val nextPageIndex: Int
    )

    private fun updateDialogue() {
        if (currentPageIndex < dialogues.size) {
            binding.tvDialogue.text = dialogues[currentPageIndex]
        }

        val uiState = calculateDialogueUIState(currentPageIndex, totalPages)
        binding.tvArrow.visibility = if (uiState.showArrow) View.VISIBLE else View.GONE

        if (!uiState.isLastPage) {
            binding.btnLater.visibility = View.GONE
            binding.btnGoToGuide.visibility = View.GONE
        }
    }

    /**
     * Pure function to calculate UI state for dialogue - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun calculateDialogueUIState(currentIndex: Int, total: Int): DialogueUIState {
        val isLastPage = currentIndex == total - 1
        return DialogueUIState(
            isLastPage = isLastPage,
            showArrow = !isLastPage
        )
    }

    /**
     * Data class for dialogue UI state
     */
    @VisibleForTesting
    internal data class DialogueUIState(
        val isLastPage: Boolean,
        val showArrow: Boolean
    )

    private fun showCompletionButtons() {
        // Change dialogue to completion message - guide user to use button on home screen
        binding.tvDialogue.text = getCompletionMessage()

        // Show only confirm button (hide "나중에" button)
        binding.tvArrow.visibility = View.GONE
        binding.btnLater.visibility = View.GONE
        binding.btnGoToGuide.visibility = View.VISIBLE
        binding.btnGoToGuide.text = "확인"
    }

    /**
     * Get completion message text - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun getCompletionMessage(): String {
        return "사주 가이드를 모두 둘러봤어요!\n운세가 준비되면, 홈에서\n'오늘의 기운 보충하러 가기' 버튼을\n눌러 개운을 시작해보세요!"
    }

    /**
     * Get the dialogue text for a given page index - testable without Android dependencies
     */
    @VisibleForTesting
    internal fun getDialogueForPage(pageIndex: Int): String? {
        return if (pageIndex in dialogues.indices) dialogues[pageIndex] else null
    }

    private fun markWalkthroughAsSeen() {
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true).apply()
        Log.d(TAG, "Saju Guide walkthrough marked as completed")
    }

    private fun markReadyForElementNudge() {
        // Mark that user should see Element Collection nudge when returning to Home
        val prefs = requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true).apply()
        Log.d(TAG, "Element nudge flag set for Home return")
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
        }
    }

    private fun navigateBackToHome() {
        val mainActivity = requireActivity() as? MainActivity

        try {
            dismissOverlay()

            // Navigate to Home tab
            val navHostFragment = mainActivity?.supportFragmentManager
                ?.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
            navHostFragment?.navController?.navigate(R.id.homeFragment)

            Log.d(TAG, "Navigated back to Home")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Home: ${e.message}", e)
        }
    }

    private fun navigateToAR() {
        val mainActivity = requireActivity() as? MainActivity

        try {
            dismissOverlay()

            // Navigate to AR screen
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                putExtra("navigate_to_ar", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            mainActivity?.startActivity(intent)

            Log.d(TAG, "Navigated to AR screen")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to AR: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SajuGuideWalkthrough"
        const val PREF_KEY_HAS_COMPLETED_WALKTHROUGH = "has_completed_saju_guide_walkthrough"
        const val PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME = "show_element_nudge_on_home"

        fun newInstance(): SajuGuideWalkthroughOverlayFragment {
            return SajuGuideWalkthroughOverlayFragment()
        }

        /**
         * Check if walkthrough should be shown
         */
        fun shouldShowWalkthrough(context: Context): Boolean {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            return !prefs.getBoolean(PREF_KEY_HAS_COMPLETED_WALKTHROUGH, false)
        }

        /**
         * Check if Element Collection nudge should be shown on Home
         */
        fun shouldShowElementNudgeOnHome(context: Context): Boolean {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, false)
        }

        /**
         * Clear the Element Collection nudge flag
         */
        fun clearElementNudgeFlag(context: Context) {
            val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, false).apply()
        }
    }
}
