package com.example.fortuna_android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fortuna_android.databinding.FragmentFortuneBinding

class FortuneFragment : Fragment() {
    private var _binding: FragmentFortuneBinding? = null
    private val binding get() = _binding!!

    private lateinit var fortuneViewModel: FortuneViewModel
    private var lastFortuneResult: String? = null

    companion object {
        private const val TAG = "FortuneFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: FortuneFragment view created")
        _binding = FragmentFortuneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up FortuneFragment")

        // Initialize ViewModel (activity-scoped to persist across fragments)
        fortuneViewModel = ViewModelProvider(requireActivity())[FortuneViewModel::class.java]
        Log.d(TAG, "ViewModel initialized")

        // Initialize lastFortuneResult with current ViewModel state to prevent duplicate toasts
        lastFortuneResult = fortuneViewModel.fortuneResult.value

        // Set up button click listeners
        binding.btnFortune.setOnClickListener {
            getFortune()
        }

        // Observe ViewModel data
        setupObservers()
    }

    private fun setupObservers() {
        // Observe fortune result
        fortuneViewModel.fortuneResult.observe(viewLifecycleOwner) { fortune ->
            Log.d(TAG, "Fortune result received: ${if (fortune != null) "Success" else "Null"}")
            updateFortuneText(fortune)

            // Show success toast only for new fortune results, not when returning to fragment
            if (fortune != null && fortune != lastFortuneResult && isAdded) {
                Toast.makeText(requireContext(), "Fortune generated successfully!", Toast.LENGTH_SHORT).show()
                lastFortuneResult = fortune
            }
        }

        // Observe loading state
        fortuneViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Observe error messages
        fortuneViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                val binding = _binding ?: return@let
                binding.tvFortuneResult.text = it
                binding.btnFortune.isEnabled = true
                binding.btnFortune.text = "Fortune Button"

                if (isAdded) {
                    Toast.makeText(requireContext(), "Network error - check logs", Toast.LENGTH_LONG).show()
                }

                // Clear the error after showing it
                fortuneViewModel.clearError()
            }
        }
    }

    private fun updateFortuneText(fortune: String?) {
        val binding = _binding ?: return
        // Only update text if we have a fortune result
        if (fortune != null) {
            binding.tvFortuneResult.text = fortune
            binding.tvFortuneResult.visibility = View.VISIBLE
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        val binding = _binding ?: return
        if (isLoading) {
            binding.tvFortuneResult.text = "Generating your fortune... This may take up to 30 seconds."
            binding.tvFortuneResult.visibility = View.VISIBLE
            binding.btnFortune.isEnabled = false
            binding.btnFortune.text = "Generating..."
        } else {
            binding.btnFortune.isEnabled = true
            binding.btnFortune.text = "Fortune Button"
            Log.d(TAG, "Loading UI state cleared - button enabled")
            // Don't change the text when loading stops - let fortune result handle it
        }
    }

    private fun getFortune() {
        val binding = _binding ?: return
        // Get the include photos setting
        val isTomorrow = binding.switchIncludePhotos.isChecked

        // Use ViewModel to generate fortune
        fortuneViewModel.getFortune(requireContext(), isTomorrow)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
