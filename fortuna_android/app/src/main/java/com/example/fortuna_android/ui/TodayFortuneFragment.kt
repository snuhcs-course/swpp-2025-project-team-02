package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.FragmentTodayFortuneBinding
import kotlinx.coroutines.launch

class TodayFortuneFragment : Fragment() {
    private var _binding: FragmentTodayFortuneBinding? = null
    private val binding get() = _binding!!

    private lateinit var fortuneViewModel: FortuneViewModel

    companion object {
        private const val TAG = "TodayFortuneFragment"

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
    }

    private fun setupClickListeners() {
        // No click listeners needed - all functionality handled by FortuneCardView
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

                // Set up refresh fortune button click listener
                binding.fortuneCardView.setOnRefreshFortuneClickListener {
                    findNavController().navigate(R.id.arFragment)
                }

                // Set up why deficient button click listener - navigate to detail analysis tab
                binding.fortuneCardView.setOnWhyDeficientClickListener {
                    // Access parent fragment's ViewPager to switch to detail analysis tab
                    (parentFragment as? HomeFragment)?.let { homeFragment ->
                        homeFragment.view?.findViewById<androidx.viewpager2.widget.ViewPager2>(
                            R.id.viewPager
                        )?.currentItem = 1
                    }
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
    }

    private fun loadUserProfile() {
        if (!isAdded) return

        Log.d(TAG, "Loading user profile for saju data...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null && isAdded) {
                        val binding = _binding ?: return@launch

                        // Show the SajuPaljaView
                        binding.sajuPaljaView.visibility = View.VISIBLE

                        // Set user's 사주팔자 data
                        binding.sajuPaljaView.setSajuData(
                            yearly = profile.yearlyGanji,
                            monthly = profile.monthlyGanji,
                            daily = profile.dailyGanji,
                            hourly = profile.hourlyGanji
                        )

                        Log.d(TAG, "User saju data loaded successfully")
                    }
                } else {
                    Log.e(TAG, "Failed to load user profile: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
