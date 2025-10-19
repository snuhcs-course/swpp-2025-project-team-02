package com.example.fortuna_android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.IntroActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fortuneViewModel: FortuneViewModel

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: HomeFragment view created")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up HomeFragment")

        // Initialize ViewModel
        fortuneViewModel = ViewModelProvider(requireActivity())[FortuneViewModel::class.java]

        setupClickListeners()
        setupObservers()

        // Load today's fortune automatically
        loadTodayFortune()
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        
        // 사주 설명 버튼
        binding.btnSajuExplanation.setOnClickListener {
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
        }

        // 프로필 버튼 (헤더에 포함된 버튼)
        binding.root.findViewById<View>(R.id.profile_button)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
    }

    private fun setupObservers() {
        // Observe FortuneData
        fortuneViewModel.fortuneData.observe(viewLifecycleOwner) { fortuneData ->
            val binding = _binding ?: return@observe

            if (fortuneData != null) {
                Log.d(TAG, "FortuneData received, displaying on card")
                // Show fortune card and hide loading/error
                binding.fortuneCardView.visibility = View.VISIBLE
                binding.tvLoading.visibility = View.GONE
                binding.tvError.visibility = View.GONE

                // Set fortune data to card view
                binding.fortuneCardView.setFortuneData(fortuneData)
            }
        }

        // Observe loading state
        fortuneViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val binding = _binding ?: return@observe

            if (isLoading) {
                binding.tvLoading.visibility = View.VISIBLE
                binding.fortuneCardView.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            } else {
                binding.tvLoading.visibility = View.GONE
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
                binding.tvLoading.visibility = View.GONE

                // Clear the error after showing it
                fortuneViewModel.clearError()
            }
        }
    }

    private fun loadTodayFortune() {
        Log.d(TAG, "Loading today's fortune...")
        // isTomorrow = true means today (confusing naming from original code)
        fortuneViewModel.getFortune(requireContext(), isTomorrow = true)
    }

    override fun onResume() {
        super.onResume()
        // Reload fortune (ViewModel will cache if already loaded)
        loadTodayFortune()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
