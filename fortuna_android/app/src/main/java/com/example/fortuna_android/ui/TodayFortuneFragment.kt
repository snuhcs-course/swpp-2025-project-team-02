package com.example.fortuna_android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fortuna_android.IntroActivity
import com.example.fortuna_android.databinding.FragmentTodayFortuneBinding

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
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 사주 설명 버튼
        binding.btnSajuExplanation.setOnClickListener {
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
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
                binding.loadingContainer.visibility = View.GONE
                binding.tvError.visibility = View.GONE

                // Set fortune data to card view
                binding.fortuneCardView.setFortuneData(fortuneData)
            }
        }

        // Observe loading state
        fortuneViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val binding = _binding ?: return@observe

            if (isLoading) {
                binding.loadingContainer.visibility = View.VISIBLE
                binding.fortuneCardView.visibility = View.GONE
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
                binding.loadingContainer.visibility = View.GONE

                // Clear the error after showing it
                fortuneViewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
