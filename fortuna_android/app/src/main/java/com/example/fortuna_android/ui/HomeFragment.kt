package com.example.fortuna_android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

        // Load today's fortune automatically
        loadTodayFortune()
    }

    private fun loadTodayFortune() {
        Log.d(TAG, "Loading today's fortune...")
        fortuneViewModel.getTodayFortune(requireContext())
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
