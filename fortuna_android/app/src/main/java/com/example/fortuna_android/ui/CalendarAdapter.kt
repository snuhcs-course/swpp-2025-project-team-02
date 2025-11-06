package com.example.fortuna_android.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fortuna_android.api.DayData
import com.example.fortuna_android.databinding.ItemCalendarDayBinding

data class CalendarDay(
    val dayNumber: Int,
    val dayData: DayData?,
    val isCurrentMonth: Boolean
)

class CalendarAdapter(private var days: List<CalendarDay>) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    class CalendarViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay) {
            // 날짜 표시
            if (day.isCurrentMonth) {
                binding.tvDayNumber.text = day.dayNumber.toString()
                binding.tvDayNumber.alpha = 1.0f
                binding.dayContainer.alpha = 1.0f
            } else {
                // 다른 달의 날짜는 투명하게
                binding.tvDayNumber.text = ""
                binding.dayContainer.alpha = 0.3f
            }

            // 데이터가 있는 경우
            if (day.dayData != null && day.isCurrentMonth) {
                val data = day.dayData

                // 원소 색상 동그라미 표시
                val elementColor = getElementColor(data.neededElement)

                // 동그라미 배경 설정
                val circleDrawable = android.graphics.drawable.GradientDrawable()
                circleDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                circleDrawable.setColor(elementColor)
                binding.elementCircle.background = circleDrawable
                binding.elementCircle.visibility = View.VISIBLE

                // 진행도 점 표시
                binding.progressDots.visibility = View.VISIBLE
                updateProgressDots(data.collectedCount, elementColor)

                // 완료된 경우 배경 색상 변경
                if (data.isCompleted) {
                    binding.dayContainer.setBackgroundColor(Color.parseColor("#2A2A2A"))
                } else {
                    binding.dayContainer.setBackgroundColor(Color.parseColor("#1E1E1E"))
                }
            } else {
                // 데이터가 없는 경우
                binding.elementCircle.visibility = View.GONE
                binding.progressDots.visibility = View.GONE
                binding.dayContainer.setBackgroundColor(Color.parseColor("#1E1E1E"))
            }
        }

        private fun updateProgressDots(count: Int, elementColor: Int) {
            val dots = listOf(
                binding.dot1,
                binding.dot2,
                binding.dot3,
                binding.dot4,
                binding.dot5
            )

            val emptyColor = Color.parseColor("#3A3A3A")

            dots.forEachIndexed { index, dot ->
                val color = if (index < count) elementColor else emptyColor

                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setColor(color)
                dot.background = drawable
            }
        }

        private fun getElementColor(element: String): Int {
            return when (element.lowercase()) {
                "wood", "나무", "목" -> Color.parseColor("#0BEFA0")  // 초록
                "fire", "불", "화" -> Color.parseColor("#F93E3E")     // 빨강
                "earth", "흙", "토" -> Color.parseColor("#FF9500")    // 노랑
                "metal", "쇠", "금" -> Color.parseColor("#C0C0C0")    // 은색
                "water", "물", "수" -> Color.parseColor("#2BB3FC")    // 파랑
                else -> Color.parseColor("#FFFFFF")
            }
        }
    }
}