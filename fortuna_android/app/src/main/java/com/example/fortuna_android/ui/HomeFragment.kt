package com.example.fortuna_android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.IntroActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.CollectElementRequest
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.FragmentHomeBinding
import com.example.fortuna_android.util.CustomToast
import com.example.fortuna_android.util.PendingCollectionManager
import kotlinx.coroutines.launch

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

        // Load today's fortune automatically
        loadTodayFortune()
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 사주 기반 운세 가이드 버튼 (헤더) - IntroActivity로 이동
        binding.root.findViewById<View>(R.id.fortune_guide_button)?.setOnClickListener {
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTodayFortune() {
        Log.d(TAG, "Loading today's fortune...")
        fortuneViewModel.getTodayFortune(requireContext())
    }

    override fun onResume() {
        super.onResume()

        // Process pending collections from AR session
        processPendingCollections()

        // Reload fortune (ViewModel will cache if already loaded)
        loadTodayFortune()
    }

    /**
     * Process any pending element collections from completed AR sessions
     * This handles POST requests after AR session has closed to avoid lifecycle issues
     */
    private fun processPendingCollections() {
        if (!PendingCollectionManager.hasPendingCollection(requireContext())) {
            Log.d(TAG, "No pending collections to process")
            return
        }

        val pendingData = PendingCollectionManager.getPendingCollection(requireContext())
        if (pendingData == null) {
            Log.w(TAG, "Pending collection flag was set but data is invalid")
            PendingCollectionManager.clearPendingCollection(requireContext())
            return
        }

        val (elementEnglish, count) = pendingData
        Log.i(TAG, "Processing pending collection: $elementEnglish x$count")

        lifecycleScope.launch {
            try {
                val request = CollectElementRequest(chakraType = elementEnglish)
                val response = RetrofitClient.instance.collectElement(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.i(TAG, "✅ Pending collection saved successfully!")
                    Log.d(TAG, "Response: ${body.data.message}")

                    // Clear pending collection after successful POST
                    PendingCollectionManager.clearPendingCollection(requireContext())

                    // Show success toast
                    CustomToast.show(requireContext(), "Element collection saved!")

                    // Reload fortune to update collection counts
                    loadTodayFortune()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Failed to save pending collection: ${response.code()}")
                    Log.e(TAG, "Error: $errorBody")

                    // Don't clear pending - will retry on next resume
                    CustomToast.show(requireContext(), "Failed to save collection. Will retry.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving pending collection", e)

                // Don't clear pending - will retry on next resume
                CustomToast.show(requireContext(), "Failed to save collection. Will retry.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
