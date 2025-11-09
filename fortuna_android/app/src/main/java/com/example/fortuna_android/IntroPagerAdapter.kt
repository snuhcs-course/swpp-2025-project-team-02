package com.example.fortuna_android

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fortuna_android.databinding.*

class IntroPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = 5

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            position -> position + 1
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
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    class IntroPage1ViewHolder(binding: IntroPage1Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage2ViewHolder(binding: IntroPage2Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage3ViewHolder(binding: IntroPage3Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage4ViewHolder(binding: IntroPage4Binding) : RecyclerView.ViewHolder(binding.root)
    class IntroPage5ViewHolder(binding: IntroPage5Binding) : RecyclerView.ViewHolder(binding.root)
}