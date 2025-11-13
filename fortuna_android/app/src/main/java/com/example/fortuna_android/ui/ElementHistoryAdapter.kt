package com.example.fortuna_android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fortuna_android.api.ElementHistoryDay
import com.example.fortuna_android.databinding.ItemElementHistoryBinding

class ElementHistoryAdapter : RecyclerView.Adapter<ElementHistoryAdapter.ViewHolder>() {

    private var history: List<ElementHistoryDay> = emptyList()

    fun submitList(newHistory: List<ElementHistoryDay>) {
        history = newHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemElementHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount(): Int = history.size

    class ViewHolder(private val binding: ItemElementHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: ElementHistoryDay) {
            // Format date (YYYY-MM-DD -> MM월 DD일)
            val dateParts = day.date.split("-")
            if (dateParts.size == 3) {
                val month = dateParts[1].toIntOrNull() ?: 0
                val dayNum = dateParts[2].toIntOrNull() ?: 0
                binding.dateText.text = "${month}월 ${dayNum}일"
            } else {
                binding.dateText.text = day.date
            }

            binding.countText.text = "${day.collectedCount}개"
        }
    }
}