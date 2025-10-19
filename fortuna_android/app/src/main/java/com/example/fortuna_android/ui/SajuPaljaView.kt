package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
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
            setGanjiText(binding.yearlyGanji1, yearly[0].toString())
            setGanjiText(binding.yearlyGanji2, yearly[1].toString())
        }

        if (monthly != null && monthly.length == 2) {
            setGanjiText(binding.monthlyGanji1, monthly[0].toString())
            setGanjiText(binding.monthlyGanji2, monthly[1].toString())
        }

        if (daily != null && daily.length == 2) {
            setGanjiText(binding.dailyGanji1, daily[0].toString())
            setGanjiText(binding.dailyGanji2, daily[1].toString())
        }

        if (hourly != null && hourly.length == 2) {
            setGanjiText(binding.hourlyGanji1, hourly[0].toString())
            setGanjiText(binding.hourlyGanji2, hourly[1].toString())
        }
    }

    /**
     * 타이틀 설정 (기본: "당신의 사주팔자")
     */
    fun setTitle(title: String) {
        binding.sajuTitle.text = title
    }

    private fun setGanjiText(textView: TextView, text: String) {
        textView.text = text
        textView.setBackgroundColor(getGanjiColor(text))
    }

    private fun getGanjiColor(ganji: String): Int {
        return when (ganji) {
            // 목(木) - 초록색
            "갑", "을", "인", "묘" -> Color.parseColor("#0BEFA0")
            // 화(火) - 빨간색
            "병", "정", "사", "오" -> Color.parseColor("#F93E3E")
            // 토(土) - 노란색
            "무", "기", "술", "미", "축", "진" -> Color.parseColor("#FF9500")
            // 금(金) - 흰색
            "경", "신", "유" -> Color.parseColor("#C1BFBF")
            // 수(水) - 회색
            "임", "계", "자", "해" -> Color.parseColor("#2BB3FC")
            else -> Color.parseColor("#CCCCCC")
        }
    }
}
