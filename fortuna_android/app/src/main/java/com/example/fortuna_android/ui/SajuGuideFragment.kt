package com.example.fortuna_android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.IntroPagerAdapter
import com.example.fortuna_android.databinding.FragmentSajuGuideBinding

class SajuGuideFragment : Fragment() {
    private var _binding: FragmentSajuGuideBinding? = null
    private val binding get() = _binding!!

    private lateinit var introPagerAdapter: IntroPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSajuGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    private fun setupViewPager() {
        introPagerAdapter = IntroPagerAdapter()
        binding.viewPager.adapter = introPagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position)
            }
        })

        // Allow manual swipe
        binding.viewPager.isUserInputEnabled = true
    }

    private fun updatePageIndicator(position: Int) {
        binding.pageIndicator.text = "${position + 1} / ${introPagerAdapter.itemCount}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}