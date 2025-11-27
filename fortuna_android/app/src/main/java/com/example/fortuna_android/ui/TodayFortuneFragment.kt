package com.example.fortuna_android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fortuna_android.TutorialOverlayFragment
import com.example.fortuna_android.databinding.FragmentTodayFortuneBinding

class TodayFortuneFragment : Fragment() {
    private var _binding: FragmentTodayFortuneBinding? = null
    private val binding get() = _binding!!

    private lateinit var fortuneViewModel: FortuneViewModel

    companion object {
        private const val TAG = "TodayFortuneFragment"
        private const val PREFS_NAME = "fortuna_prefs"

        fun newInstance() = TodayFortuneFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayFortuneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get shared ViewModel from parent activity
        fortuneViewModel = ViewModelProvider(requireActivity())[FortuneViewModel::class.java]

        setupClickListeners()
        setupObservers()
        loadUserProfile()
        loadFortuneData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible again (e.g., after profile edit)
        Log.d(TAG, "onResume - refreshing data")
        refreshData()
    }

    private fun setupClickListeners() {
        // Tutorial replay button click listener
        binding.btnReplayTutorial.setOnClickListener {
            Log.d(TAG, "Tutorial replay button clicked")
            resetTutorials()
            showTutorialOverlay()
        }
    }

    private fun setupObservers() {
        // Observe TodayFortuneData
        fortuneViewModel.fortuneData.observe(viewLifecycleOwner) { fortuneData ->
            val binding = _binding ?: return@observe

            if (fortuneData != null) {
                Log.d(TAG, "TodayFortuneData received, displaying on card")
                // Show fortune card and hide loading/error
                binding.fortuneCardView.visibility = View.VISIBLE
                binding.todaySajuPaljaView.visibility = View.VISIBLE
                binding.loadingContainer.visibility = View.GONE
                binding.tvError.visibility = View.GONE

                // Set fortune data to card view
                binding.fortuneCardView.setFortuneData(fortuneData)

                // Set 오늘의 운기 data (2x4 table)
                val elements = fortuneData.fortuneScore.elements
                binding.todaySajuPaljaView.setTodaySajuData(
                    daeun = elements["대운"],
                    saeun = elements["세운"],
                    wolun = elements["월운"],
                    ilun = elements["일운"]
                )

                // Set up refresh fortune button click listener - Show Tutorial or Navigate to AR
                binding.fortuneCardView.setOnRefreshFortuneClickListener {
                    Log.d(TAG, "Refresh fortune button clicked")
                    checkTutorialStatusAndNavigate()
                }
            }
        }

        // Observe loading state
        fortuneViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val binding = _binding ?: return@observe

            if (isLoading) {
                binding.loadingContainer.visibility = View.VISIBLE
                binding.fortuneCardView.visibility = View.GONE
                binding.todaySajuPaljaView.visibility = View.GONE
                binding.sajuPaljaView.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            } else {
                binding.loadingContainer.visibility = View.GONE

                // Restore views if data already exists
                val currentFortuneData = fortuneViewModel.fortuneData.value
                if (currentFortuneData != null) {
                    binding.fortuneCardView.visibility = View.VISIBLE
                    binding.todaySajuPaljaView.visibility = View.VISIBLE
                }

                // Restore sajuPaljaView if userProfile data already exists
                val currentUserProfile = fortuneViewModel.userProfile.value
                if (currentUserProfile != null) {
                    binding.sajuPaljaView.visibility = View.VISIBLE
                }
            }
        }

        // Observe error messages
        fortuneViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            val binding = _binding ?: return@observe

            if (errorMessage != null) {
                Log.e(TAG, "Error loading fortune: $errorMessage")
                binding.tvError.text = errorMessage
                binding.tvError.visibility = View.VISIBLE
                binding.fortuneCardView.visibility = View.GONE
                binding.todaySajuPaljaView.visibility = View.GONE
                binding.sajuPaljaView.visibility = View.GONE
                binding.loadingContainer.visibility = View.GONE

                // Clear the error after showing it
                fortuneViewModel.clearError()
            }
        }

        // Observe user profile for 사주팔자 data
        fortuneViewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            val binding = _binding ?: return@observe

            if (profile != null) {
                Log.d(TAG, "User profile received, displaying saju data")
                // Show the SajuPaljaView
                binding.sajuPaljaView.visibility = View.VISIBLE

                // Set user's 사주팔자 data
                binding.sajuPaljaView.setSajuData(
                    yearly = profile.yearlyGanji,
                    monthly = profile.monthlyGanji,
                    daily = profile.dailyGanji,
                    hourly = profile.hourlyGanji
                )
            }
        }

        // Observe AI generation status message
        fortuneViewModel.generatingMessage.observe(viewLifecycleOwner) { message ->
            val binding = _binding ?: return@observe

            if (message != null) {
                Log.d(TAG, "AI is generating fortune: $message")
                // Update loading message to show AI generation status
                binding.loadingContainer.visibility = View.VISIBLE
                binding.fortuneCardView.visibility = View.GONE
                binding.todaySajuPaljaView.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
        }
    }

    /**
     * Check tutorial status and decide whether to show tutorial or navigate directly to AR
     */
    private fun checkTutorialStatusAndNavigate() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenHomeTutorial = prefs.getBoolean("has_seen_home_tutorial", false)
        val hasSeenARTutorial = prefs.getBoolean("has_seen_ar_tutorial", false)

        Log.d(TAG, "Tutorial status check - Home: $hasSeenHomeTutorial, AR: $hasSeenARTutorial")

        if (hasSeenHomeTutorial && hasSeenARTutorial) {
            // Both tutorials already seen - skip directly to AR
            Log.i(TAG, "Both tutorials already seen, navigating directly to AR")
            navigateDirectlyToAR()
        } else {
            // Show home tutorial (which will navigate to AR after completion)
            Log.i(TAG, "Showing home tutorial overlay")
            showTutorialOverlay()
        }
    }

    /**
     * Reset tutorial flags to replay tutorials
     * Triggered by clicking the "튜토리얼 다시보기" button
     */
    private fun resetTutorials() {
        if (!isAdded) return

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("has_seen_home_tutorial")
            remove("has_seen_ar_tutorial")
            apply()
        }

        Log.i(TAG, "🔄 Tutorial flags reset - both tutorials will show again")
    }

    /**
     * Navigate directly to AR screen without showing tutorial
     */
    private fun navigateDirectlyToAR() {
        val intent = Intent(requireContext(), com.example.fortuna_android.MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun showTutorialOverlay() {
        // Show tutorial overlay fragment over the current screen
        val overlayFragment = TutorialOverlayFragment.newInstance()

        // Use requireActivity's supportFragmentManager to add to the activity's root view
        requireActivity().supportFragmentManager.beginTransaction()
            .add(android.R.id.content, overlayFragment, TutorialOverlayFragment.TAG)
            .addToBackStack(TutorialOverlayFragment.TAG)
            .commit()
    }

    private fun loadUserProfile() {
        // Use ViewModel to load user profile (with caching)
        fortuneViewModel.loadUserProfile()
    }

    private fun loadFortuneData() {
        // Load today's fortune data on initial load
        Log.d(TAG, "Loading fortune data...")
        fortuneViewModel.getTodayFortune(requireContext())
    }

    private fun refreshData() {
        // Refresh both user profile and fortune data
        // This will force reload from server if data has changed
        Log.d(TAG, "Refreshing all data from server...")
        fortuneViewModel.refreshUserProfile()
        fortuneViewModel.refreshFortuneData(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
