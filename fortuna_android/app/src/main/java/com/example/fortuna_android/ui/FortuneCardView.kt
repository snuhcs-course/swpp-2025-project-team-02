package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.fortuna_android.api.ChakraReading
import com.example.fortuna_android.api.FortuneData
import com.example.fortuna_android.databinding.CardFortuneBinding
import java.text.SimpleDateFormat
import java.util.*

class FortuneCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding: CardFortuneBinding
    private var isDetailVisible = false

    init {
        binding = CardFortuneBinding.inflate(LayoutInflater.from(context), this, true)
        setupToggleButton()
    }

    /**
     * 상세보기 토글 버튼 설정
     */
    private fun setupToggleButton() {
        binding.btnToggleDetails.setOnClickListener {
            isDetailVisible = !isDetailVisible
            binding.llDetailedInfo.visibility = if (isDetailVisible) View.VISIBLE else View.GONE
            binding.btnToggleDetails.text = if (isDetailVisible) "접기" else "운세 상세보기"
        }
    }

    /**
     * FortuneData를 받아서 카드에 표시
     */
    fun setFortuneData(fortuneData: FortuneData) {
        // 날짜 표시
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fortuneData.fortune.tomorrowDate)
            binding.tvFortuneDate.text = dateFormat.format(date ?: Date())
        } catch (e: Exception) {
            binding.tvFortuneDate.text = fortuneData.fortune.tomorrowDate
        }

        // 간지 정보 표시
        val ganjiElement = getElementEmoji(fortuneData.tomorrowGapja.element)
        binding.tvTomorrowGanji.text = "${fortuneData.tomorrowGapja.name}($ganjiElement)"

        // 중앙 오행 원소 한자 표시
        val elementChar = getElementCharacter(fortuneData.tomorrowGapja.element)
        val elementColor = getElementColorFromString(fortuneData.tomorrowGapja.element)
        binding.tvElementCharacter.text = elementChar
        binding.tvElementCharacter.setTextColor(elementColor)

        // 전체 운세 점수
        binding.tvOverallFortune.text = fortuneData.fortune.overallFortune.toString()

        // 특별 메시지 & 운세 요약
        binding.tvSpecialMessage.text = fortuneData.fortune.specialMessage
        binding.tvFortuneSummary.text = fortuneData.fortune.fortuneSummary

        // 행운의 키워드 (keyAdvice 기반)
        val guidance = fortuneData.fortune.dailyGuidance
        binding.tvKeywords.text = generateKeywords(guidance.keyAdvice)

        // 오행 균형
        binding.tvElementBalance.text = fortuneData.fortune.elementBalance

        // 일일 가이던스
        binding.tvKeyAdvice.text = "💡 ${guidance.keyAdvice}"
        binding.tvBestTime.text = "⏰ ${guidance.bestTime}"
        binding.tvLuckyDirection.text = "🧭 ${guidance.luckyDirection}"
        binding.tvLuckyColor.text = "🎨 ${guidance.luckyColor}"

        // 차크라 리딩
        displayChakraReadings(fortuneData.fortune.chakraReadings)

        // 사주 궁합
        binding.tvSajuCompatibility.text = fortuneData.fortune.sajuCompatibility
    }

    /**
     * 키워드 생성 (keyAdvice 기반)
     */
    private fun generateKeywords(keyAdvice: String): String {
        // 간단한 키워드 추출 (첫 몇 단어)
        val words = keyAdvice.split(" ").take(2)
        return words.joinToString(" ") { "#$it" }
    }

    /**
     * 차크라 리딩 동적 표시
     */
    private fun displayChakraReadings(chakraReadings: List<ChakraReading>) {
        binding.llChakraReadings.removeAllViews()

        chakraReadings.forEach { reading ->
            val chakraView = createChakraReadingView(reading)
            binding.llChakraReadings.addView(chakraView)
        }
    }

    /**
     * 개별 차크라 리딩 뷰 생성
     */
    private fun createChakraReadingView(reading: ChakraReading): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
            setPadding(20, 16, 20, 16)
            setBackgroundColor(Color.parseColor("#3A3668"))
        }

        // 차크라 타입 및 강도
        val headerText = TextView(context).apply {
            val chakraEmoji = getChakraEmoji(reading.chakraType)
            text = "$chakraEmoji ${getChakraName(reading.chakraType)} - 강도: ${reading.strength}/10"
            textSize = 14f
            setTextColor(getChakraColor(reading.chakraType))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 10)
            }
        }

        // 메시지
        val messageText = TextView(context).apply {
            text = reading.message
            textSize = 13f
            setTextColor(Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 6)
            }
        }

        // 위치 의미
        val locationText = TextView(context).apply {
            text = "📍 ${reading.locationSignificance}"
            textSize = 12f
            setTextColor(Color.parseColor("#AAAAAA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(headerText)
        layout.addView(messageText)
        layout.addView(locationText)

        return layout
    }

    /**
     * 오행 원소에 따른 이모지 반환
     */
    private fun getElementEmoji(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "🌳"
            "fire", "불", "화" -> "🔥"
            "earth", "흙", "토" -> "🌏"
            "metal", "쇠", "금" -> "⚔️"
            "water", "물", "수" -> "💧"
            else -> "⭐"
        }
    }

    /**
     * 오행 원소 한자 반환
     */
    private fun getElementCharacter(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "木"
            "fire", "불", "화" -> "火"
            "earth", "흙", "토" -> "土"
            "metal", "쇠", "금" -> "金"
            "water", "물", "수" -> "水"
            else -> "☆"
        }
    }

    /**
     * 오행 원소 색상 반환
     */
    private fun getElementColorFromString(element: String): Int {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> Color.parseColor("#0BEFA0")  // 초록
            "fire", "불", "화" -> Color.parseColor("#F93E3E")     // 빨강
            "earth", "흙", "토" -> Color.parseColor("#FF9500")    // 노랑
            "metal", "쇠", "금" -> Color.parseColor("#C0C0C0")    // 은색
            "water", "물", "수" -> Color.parseColor("#2BB3FC")    // 파랑
            else -> Color.parseColor("#FFFFFF")
        }
    }

    /**
     * 차크라 타입에 따른 이모지 반환
     */
    private fun getChakraEmoji(chakraType: String): String {
        return when (chakraType.lowercase()) {
            "wood", "나무", "목" -> "🌳"
            "fire", "불", "화" -> "🔥"
            "earth", "흙", "토" -> "🌏"
            "metal", "쇠", "금" -> "⚔️"
            "water", "물", "수" -> "💧"
            else -> "🔵"
        }
    }

    /**
     * 차크라 타입 한글 이름 반환
     */
    private fun getChakraName(chakraType: String): String {
        return when (chakraType.lowercase()) {
            "wood" -> "목(木)"
            "fire" -> "화(火)"
            "earth" -> "토(土)"
            "metal" -> "금(金)"
            "water" -> "수(水)"
            else -> chakraType
        }
    }

    /**
     * 차크라 타입에 따른 색상 반환
     */
    private fun getChakraColor(chakraType: String): Int {
        return when (chakraType.lowercase()) {
            "wood", "나무", "목" -> Color.parseColor("#0BEFA0")  // 초록
            "fire", "불", "화" -> Color.parseColor("#F93E3E")     // 빨강
            "earth", "흙", "토" -> Color.parseColor("#FF9500")    // 노랑
            "metal", "쇠", "금" -> Color.parseColor("#C0C0C0")    // 은색
            "water", "물", "수" -> Color.parseColor("#2BB3FC")    // 파랑
            else -> Color.parseColor("#FFFFFF")
        }
    }
}
