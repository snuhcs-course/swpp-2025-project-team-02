package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.fortuna_android.common.AppColors
import com.example.fortuna_android.databinding.ViewSajuPaljaBinding

class SajuPaljaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewSajuPaljaBinding

    init {
        binding = ViewSajuPaljaBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * 사주팔자 데이터 설정
     * @param yearly 연주 (예: "을해")
     * @param monthly 월주 (예: "기묘")
     * @param daily 일주 (예: "무신")
     * @param hourly 시주 (예: "정오")
     */
    fun setSajuData(yearly: String?, monthly: String?, daily: String?, hourly: String?) {
        // 각 간지를 천간(첫글자)과 지지(둘째글자)로 분리하여 표시
        if (yearly != null && yearly.length == 2) {
            setGanjiCard(binding.yearlyGanji1, binding.yearlyGanji1Element, yearly[0].toString())
            setGanjiCard(binding.yearlyGanji2, binding.yearlyGanji2Element, yearly[1].toString())
        }

        if (monthly != null && monthly.length == 2) {
            setGanjiCard(binding.monthlyGanji1, binding.monthlyGanji1Element, monthly[0].toString())
            setGanjiCard(binding.monthlyGanji2, binding.monthlyGanji2Element, monthly[1].toString())
        }

        if (daily != null && daily.length == 2) {
            setGanjiCard(binding.dailyGanji1, binding.dailyGanji1Element, daily[0].toString())
            setGanjiCard(binding.dailyGanji2, binding.dailyGanji2Element, daily[1].toString())
        }

        if (hourly != null && hourly.length == 2) {
            setGanjiCard(binding.hourlyGanji1, binding.hourlyGanji1Element, hourly[0].toString())
            setGanjiCard(binding.hourlyGanji2, binding.hourlyGanji2Element, hourly[1].toString())
        }
    }

    /**
     * 타이틀 설정 (기본: "당신의 사주팔자")
     */
    fun setTitle(title: String) {
        binding.sajuTitle.text = title
    }

    private fun setGanjiCard(ganjiTextView: TextView, elementTextView: TextView, ganji: String) {
        // 한글 간지 표시
        ganjiTextView.text = ganji

        // 오행 정보 표시 (한자, 한글)
        val elementInfo = getElementInfo(ganji)
        elementTextView.text = elementInfo

        // 배경색 설정 with rounded corners
        val color = getGanjiColor(ganji)
        ganjiTextView.parent?.let { parent ->
            if (parent is LinearLayout) {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.setColor(color)
                drawable.cornerRadius = 12f * context.resources.displayMetrics.density // 12dp to pixels
                parent.background = drawable
            }
        }
    }

    private fun getElementInfo(ganji: String): String {
        return when (ganji) {
            // 천간 - 목(木)
            "갑", "을" -> "木, 나무"
            // 천간 - 화(火)
            "병", "정" -> "火, 불"
            // 천간 - 토(土)
            "무", "기" -> "土, 흙"
            // 천간 - 금(金)
            "경", "신" -> "金, 쇠"
            // 천간 - 수(水)
            "임", "계" -> "水, 물"
            // 지지 - 목(木)
            "인", "묘" -> "木, 나무"
            // 지지 - 화(火)
            "사", "오" -> "火, 불"
            // 지지 - 토(土)
            "진", "술", "축", "미" -> "土, 흙"
            // 지지 - 금(金)
            "신", "유" -> "金, 쇠"
            // 지지 - 수(水)
            "해", "자" -> "水, 물"
            else -> "?, ?"
        }
    }

    private fun getGanjiColor(ganji: String): Int {
        return AppColors.getElementColorByBranch(ganji)
    }
}
