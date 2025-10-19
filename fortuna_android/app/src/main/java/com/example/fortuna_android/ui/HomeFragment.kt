package com.example.fortuna_android.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.R
import com.example.fortuna_android.databinding.FragmentHomeBinding

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

        // 개운하기 버튼 (FloatingActionButton) - 카메라로 이동
        binding.fabRefreshFortune.setOnClickListener {
            findNavController().navigate(R.id.arFragment)
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
        // Reload fortune (ViewModel will cache if already loaded)
        loadTodayFortune()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
