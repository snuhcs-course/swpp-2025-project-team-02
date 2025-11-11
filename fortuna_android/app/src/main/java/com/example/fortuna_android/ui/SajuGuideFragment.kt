package com.example.fortuna_android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.IntroPagerAdapter
import com.example.fortuna_android.R
import com.example.fortuna_android.databinding.FragmentSajuGuideBinding

class SajuGuideFragment : Fragment() {
    private var _binding: FragmentSajuGuideBinding? = null
    private val binding get() = _binding!!

    private lateinit var introPagerAdapter: IntroPagerAdapter
    private val dots = mutableListOf<ImageView>()

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
        setupDots()
    }

    private fun setupViewPager() {
        introPagerAdapter = IntroPagerAdapter()
        binding.viewPager.adapter = introPagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
            }
        })

        // Allow manual swipe
        binding.viewPager.isUserInputEnabled = true
    }

    private fun setupDots() {
        val dotsCount = introPagerAdapter.itemCount
        dots.clear()
        binding.dotsIndicator.removeAllViews()

        for (i in 0 until dotsCount) {
            val dot = ImageView(requireContext()).apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        if (i == 0) R.drawable.dot_selected else R.drawable.dot_unselected
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i != 0) setMargins(8.dpToPx(), 0, 0, 0)
                }
            }
            dots.add(dot)
            binding.dotsIndicator.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        dots.forEachIndexed { index, imageView ->
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (index == position) R.drawable.dot_selected else R.drawable.dot_unselected
                )
            )
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}