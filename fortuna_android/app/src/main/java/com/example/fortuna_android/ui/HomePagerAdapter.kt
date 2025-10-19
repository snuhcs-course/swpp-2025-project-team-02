package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TodayFortuneFragment.newInstance()
            1 -> DetailAnalysisFragment.newInstance()
            else -> TodayFortuneFragment.newInstance()
        }
    }
}
