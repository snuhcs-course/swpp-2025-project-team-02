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
import com.example.fortuna_android.api.TodayFortuneData
import com.example.fortuna_android.databinding.CardFortuneBinding
// Temporarily disabled due to JitPack server issues
// import com.github.mikephil.charting.data.PieData
// import com.github.mikephil.charting.data.PieDataSet
// import com.github.mikephil.charting.data.PieEntry
// import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.*

class FortuneCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding: CardFortuneBinding

    init {
        // Set CardView background to black to prevent white corners
        setCardBackgroundColor(Color.parseColor("#000000"))

        binding = CardFortuneBinding.inflate(LayoutInflater.from(context), this, true)

        // 상세 정보를 기본으로 표시
        binding.llDetailedInfo.visibility = View.VISIBLE

        // 토글 버튼 숨기기
        binding.btnToggleDetails.visibility = View.GONE
    }

    /**
     * TodayFortuneData를 받아서 카드에 표시
     */
    fun setFortuneData(fortuneData: TodayFortuneData) {
        // 날짜 표시
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
        if (fortuneData.forDate != null) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fortuneData.forDate)
                binding.tvFortuneDate.text = dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                binding.tvFortuneDate.text = fortuneData.forDate
            }
        } else {
            // forDate가 null이면 오늘 날짜 표시
            binding.tvFortuneDate.text = dateFormat.format(Date())
        }

        // 간지 정보 표시 (fortuneScore의 elements에서 일운 추출)
        val dayPillar = fortuneData.fortuneScore.elements["일운"]
        if (dayPillar != null) {
            val ganjiElement = getElementEmoji(dayPillar.stem.element)
            binding.tvTomorrowGanji.text = "${dayPillar.twoLetters}일($ganjiElement)"

            // 중앙 오행 원소 한자 표시 (일간의 오행 사용)
            val elementChar = getElementCharacter(dayPillar.stem.element)
            val elementColor = getElementColorFromString(dayPillar.stem.element)
            binding.tvElementCharacter.text = elementChar
            binding.tvElementCharacter.setTextColor(elementColor)

            // 오행 기운 메시지
            val elementMessage = getElementMessage(dayPillar.stem.element)
            binding.tvElementMessage.text = elementMessage
        } else {
            // 데이터가 없을 경우 기본값
            binding.tvTomorrowGanji.text = "오늘의 운세"
            binding.tvElementCharacter.text = "運"
            binding.tvElementCharacter.setTextColor(Color.parseColor("#FFD700"))
            binding.tvElementMessage.text = "오늘의 기운을 느껴보세요"
        }

        // 전체 운세 점수
        binding.tvOverallFortune.text = fortuneData.fortune.overallFortune.toString()

        // 특별 메시지 & 운세 요약
//        binding.tvSpecialMessage.text = fortuneData.fortune.specialMessage
//        binding.tvFortuneSummary.text = fortuneData.fortune.fortuneSummary

        // 행운의 키워드 (keyAdvice 기반)
        val guidance = fortuneData.fortune.dailyGuidance
//        binding.tvKeywords.text = generateKeywords(guidance.keyAdvice)

        // 오행 균형
        binding.tvElementBalance.text = fortuneData.fortune.elementBalance

        // 오행 분포 표시
        // Temporarily disabled due to JitPack server issues
        // displayElementDistribution(fortuneData.fortuneScore.elementDistribution)

        // 해석
        binding.tvInterpretation.text = fortuneData.fortuneScore.interpretation

        // 일일 가이던스
        binding.tvKeyAdvice.text = "💡 ${guidance.keyAdvice}"
        binding.tvBestTime.text = "⏰ ${guidance.bestTime}"
        binding.tvLuckyDirection.text = "🧭 ${guidance.luckyDirection}"
        binding.tvLuckyColor.text = "🎨 ${guidance.luckyColor}"

        // 차크라 리딩
        displayChakraReadings(fortuneData.fortune.chakraReadings)
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
     * 오행 분포 파이 차트 표시
     * Temporarily disabled due to JitPack server issues
     */
    /*
    private fun displayElementDistribution(elementDistribution: Map<String, com.example.fortuna_android.api.ElementDistribution>) {
        val pieChart = binding.pieChartElementDistribution

        // 오행 순서: 목, 화, 토, 금, 수
        val elementOrder = listOf("목", "화", "토", "금", "수")
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        elementOrder.forEach { element ->
            elementDistribution[element]?.let { distribution ->
                val emoji = getElementEmoji(element)
                entries.add(PieEntry(distribution.percentage.toFloat(), "$emoji $element"))
                colors.add(getElementColorFromString(element))
            }
        }

        // 데이터 세트 설정
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = Color.WHITE
            valueTextSize = 14f
            sliceSpace = 2f
            valueFormatter = PercentFormatter(pieChart)
        }

        // 파이 데이터 설정
        val data = PieData(dataSet).apply {
            setValueTextColor(Color.WHITE)
            setValueTextSize(12f)
        }

        // 차트 설정
        pieChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = Color.parseColor("#CCCCCC")
            legend.textSize = 12f
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            isRotationEnabled = true
            setHoleColor(Color.parseColor("#1E1E1E"))
            transparentCircleRadius = 58f
            holeRadius = 50f
            centerText = "오행 분포"
            setCenterTextColor(Color.parseColor("#FFFFFF"))
            setCenterTextSize(14f)
            animateY(1000)
            invalidate()
        }
    }
    */

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
            setBackgroundColor(Color.parseColor("#2A2A2A"))
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
     * 오행에 따른 기운 메시지 반환
     */
    private fun getElementMessage(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "오늘은 나무의 기운이 강한 날입니다"
            "fire", "불", "화" -> "오늘은 불의 기운이 강한 날입니다"
            "earth", "흙", "토" -> "오늘은 흙의 기운이 강한 날입니다"
            "metal", "쇠", "금" -> "오늘은 쇠의 기운이 강한 날입니다"
            "water", "물", "수" -> "오늘은 물의 기운이 강한 날입니다"
            else -> "오늘의 기운을 느껴보세요"
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
