package com.example.fortuna_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding
    private lateinit var introPagerAdapter: IntroPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
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
    }

    private fun setupClickListeners() {
        binding.btnSkip.setOnClickListener {
            finish()
        }

        // Allow manual swipe
        binding.viewPager.isUserInputEnabled = true
    }

    private fun updatePageIndicator(position: Int) {
        binding.pageIndicator.text = "${position + 1} / ${introPagerAdapter.itemCount}"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}