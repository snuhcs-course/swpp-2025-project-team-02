package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.example.fortuna_android.SajuGuideWalkthroughOverlayFragment
import com.example.fortuna_android.databinding.FragmentSajuGuideBinding

class SajuGuideFragment : Fragment() {

    companion object {
        private const val TAG = "SajuGuideFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val PREF_KEY_START_WALKTHROUGH = "start_saju_walkthrough"
    }
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

        // Check if walkthrough should be shown
        checkAndShowWalkthrough()
    }

    /**
     * Check if walkthrough should be shown and display it
     */
    private fun checkAndShowWalkthrough() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shouldStartWalkthrough = prefs.getBoolean(PREF_KEY_START_WALKTHROUGH, false)

        if (shouldStartWalkthrough) {
            // Clear the flag
            prefs.edit().putBoolean(PREF_KEY_START_WALKTHROUGH, false).apply()

            // Show walkthrough overlay if not completed yet
            if (SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(requireContext())) {
                Log.d(TAG, "Starting Saju Guide walkthrough")
                showWalkthroughOverlay()
            }
        }
    }

    /**
     * Show the walkthrough overlay
     */
    private fun showWalkthroughOverlay() {
        // Check if overlay is already showing
        val existingOverlay = requireActivity().supportFragmentManager
            .findFragmentByTag(SajuGuideWalkthroughOverlayFragment.TAG)
        if (existingOverlay != null) {
            Log.d(TAG, "Walkthrough overlay already showing")
            return
        }

        val walkthroughFragment = SajuGuideWalkthroughOverlayFragment.newInstance()

        requireActivity().supportFragmentManager.beginTransaction()
            .add(android.R.id.content, walkthroughFragment, SajuGuideWalkthroughOverlayFragment.TAG)
            .addToBackStack(SajuGuideWalkthroughOverlayFragment.TAG)
            .commit()

        Log.d(TAG, "Walkthrough overlay shown")
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