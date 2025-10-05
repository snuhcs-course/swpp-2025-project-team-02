package com.example.fortuna_android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fortuneViewModel: FortuneViewModel
    private var lastFortuneResult: String? = null

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

        // Initialize ViewModel (activity-scoped to persist across fragments)
        fortuneViewModel = ViewModelProvider(requireActivity())[FortuneViewModel::class.java]
        Log.d(TAG, "ViewModel initialized")

        // Initialize lastFortuneResult with current ViewModel state to prevent duplicate toasts
        lastFortuneResult = fortuneViewModel.fortuneResult.value

        // Set up button click listeners
        binding.btnHome.setOnClickListener {
            checkPermissions()
        }

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
        fortuneViewModel.getFortune(isTomorrow)
    }

    private fun checkPermissions() {
        // Check if fragment is still attached
        if (!isAdded) {
            return
        }

        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            // All permissions granted - navigate to camera
            Log.d(TAG, "All permissions granted - navigating to camera")
            if (isAdded) {
                Toast.makeText(requireContext(), "All permissions granted! Ready to proceed.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.cameraFragment)
            }
        } else {
            // Some permissions missing - request them via MainActivity
            Log.d(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
            if (isAdded) {
                Toast.makeText(requireContext(), "Some permissions missing. Requesting permissions...", Toast.LENGTH_SHORT).show()
            }
            if (activity is MainActivity) {
                (activity as? MainActivity)?.requestPermissions()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}