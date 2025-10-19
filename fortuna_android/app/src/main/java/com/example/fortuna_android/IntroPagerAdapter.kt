package com.example.fortuna_android

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fortuna_android.databinding.*

class IntroPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = 7 // 7 intro pages

    override fun getItemViewType(position: Int): Int {
        // 새로운 순서: 구조 → 천간지지 → 오행 → 일주 → 음양 → 십성 → 운세
        return when (position) {
            0 -> 1  // 사주의 구조 (page1)
            1 -> 2  // 천간과 지지 (page2)
            2 -> 4  // 오행 (page4)
            3 -> 3  // 일주 (page3)
            4 -> 5  // 음양 (page5)
            5 -> 6  // 십성 (page6)
            6 -> 7  // 오늘의 운세 (page7)
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> IntroPage1ViewHolder(IntroPage1Binding.inflate(inflater, parent, false))
            2 -> IntroPage2ViewHolder(IntroPage2Binding.inflate(inflater, parent, false))
            3 -> IntroPage3ViewHolder(IntroPage3Binding.inflate(inflater, parent, false))
            4 -> IntroPage4ViewHolder(IntroPage4Binding.inflate(inflater, parent, false))
            5 -> IntroPage5ViewHolder(IntroPage5Binding.inflate(inflater, parent, false))
            6 -> IntroPage6ViewHolder(IntroPage6Binding.inflate(inflater, parent, false))
            7 -> IntroPage7ViewHolder(IntroPage7Binding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Static pages, no binding needed
    }

    class IntroPage1ViewHolder(binding: IntroPage1Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage2ViewHolder(binding: IntroPage2Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage3ViewHolder(binding: IntroPage3Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage4ViewHolder(binding: IntroPage4Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage5ViewHolder(binding: IntroPage5Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage6ViewHolder(binding: IntroPage6Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage7ViewHolder(binding: IntroPage7Binding) : RecyclerView.ViewHolder(binding.root)
}