package com.example.fortuna_android.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fortuna_android.api.DayData
import com.example.fortuna_android.common.AppColors
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

                // 원소 색상
                val elementColor = getElementColor(data.neededElement)

                // 5개 모두 수집한 경우 체크 표시
                if (data.isCompleted && data.collectedCount >= 5) {
                    // 체크 아이콘 표시
                    binding.checkIcon.visibility = View.VISIBLE
                    binding.checkIcon.setColorFilter(elementColor)

                    // 동그라미와 점들 숨김
                    binding.elementCircle.visibility = View.GONE
                    binding.progressDots.visibility = View.GONE
                } else {
                    // 체크 아이콘 숨김
                    binding.checkIcon.visibility = View.GONE

                    // 동그라미 배경 설정
                    val circleDrawable = android.graphics.drawable.GradientDrawable()
                    circleDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                    circleDrawable.setColor(elementColor)
                    binding.elementCircle.background = circleDrawable
                    binding.elementCircle.visibility = View.VISIBLE

                    // 진행도 점 표시
                    binding.progressDots.visibility = View.VISIBLE
                    updateProgressDots(data.collectedCount, elementColor)
                }

                // 완료된 경우 배경 색상 변경
                if (data.isCompleted) {
                    binding.dayContainer.setBackgroundColor(Color.parseColor(AppColors.BACKGROUND_DARK))
                } else {
                    binding.dayContainer.setBackgroundColor(Color.parseColor(AppColors.BACKGROUND_REGULAR))
                }
            } else {
                // 데이터가 없는 경우
                binding.checkIcon.visibility = View.GONE
                binding.elementCircle.visibility = View.GONE
                binding.progressDots.visibility = View.GONE
                binding.dayContainer.setBackgroundColor(Color.parseColor(AppColors.BACKGROUND_REGULAR))
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

            val emptyColor = Color.parseColor(AppColors.BACKGROUND_EMPTY)

            dots.forEachIndexed { index, dot ->
                val color = if (index < count) elementColor else emptyColor

                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setColor(color)
                dot.background = drawable
            }
        }

        private fun getElementColor(element: String): Int {
            return AppColors.getElementColorByKorean(element)
        }
    }
}