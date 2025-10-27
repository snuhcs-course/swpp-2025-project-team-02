package com.example.fortuna_android.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
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
    private lateinit var pagerAdapter: HomePagerAdapter

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

        setupViewPager()
        setupClickListeners()

        // Load today's fortune automatically
        loadTodayFortune()
    }

    private fun setupViewPager() {
        // ViewPager2 어댑터 설정
        pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true // 스와이프 가능

        // ViewPager2 페이지 변경 리스너
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabUI(position)
            }
        })
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 프로필 버튼 (헤더에 포함된 버튼)
        binding.root.findViewById<View>(R.id.profile_button)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }

        // 오늘의 운세 탭
        binding.tabTodayFortune.setOnClickListener {
            binding.viewPager.currentItem = 0
        }

        // 상세 분석 탭
        binding.tabDetailAnalysis.setOnClickListener {
            binding.viewPager.currentItem = 1
        }
    }

    private fun updateTabUI(position: Int) {
        when (position) {
            0 -> {
                // 오늘의 운세 탭 활성화
                binding.tabTodayFortune.setTextColor(Color.parseColor("#FFFFFF"))
                binding.tabTodayFortune.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.tabDetailAnalysis.setTextColor(Color.parseColor("#888888"))
                binding.tabDetailAnalysis.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            1 -> {
                // 상세 분석 탭 활성화
                binding.tabTodayFortune.setTextColor(Color.parseColor("#888888"))
                binding.tabTodayFortune.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.tabDetailAnalysis.setTextColor(Color.parseColor("#FFFFFF"))
                binding.tabDetailAnalysis.setTypeface(null, android.graphics.Typeface.BOLD)
            }
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
