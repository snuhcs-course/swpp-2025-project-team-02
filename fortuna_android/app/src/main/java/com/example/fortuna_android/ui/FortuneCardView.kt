package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.fortuna_android.api.ChakraReading
import com.example.fortuna_android.api.TodayFortuneData
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.CardFortuneBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FortuneCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val binding: CardFortuneBinding

    private var onRefreshFortuneClickListener: (() -> Unit)? = null
    private var onWhyDeficientClickListener: (() -> Unit)? = null

    init {
        // Set CardView background to black to prevent white corners
        setCardBackgroundColor(Color.parseColor("#000000"))

        binding = CardFortuneBinding.inflate(LayoutInflater.from(context), this, true)

        // 상세 정보를 기본으로 표시
        binding.llDetailedInfo.visibility = View.VISIBLE

        // 토글 버튼 숨기기
        binding.btnToggleDetails.visibility = View.GONE

        // 오늘의 기운 보충하러가기 버튼 클릭 리스너
        binding.btnRefreshFortune.setOnClickListener {
            onRefreshFortuneClickListener?.invoke()
        }

        // 왜 부족한지 설명 버튼 클릭 리스너
        binding.btnWhyDeficient.setOnClickListener {
            onWhyDeficientClickListener?.invoke()
        }
    }

    /**
     * 오늘의 기운 보충하러가기 버튼 클릭 리스너 설정
     */
    fun setOnRefreshFortuneClickListener(listener: () -> Unit) {
        onRefreshFortuneClickListener = listener
    }

    /**
     * 왜 부족한지 설명 버튼 클릭 리스너 설정
     */
    fun setOnWhyDeficientClickListener(listener: () -> Unit) {
        onWhyDeficientClickListener = listener
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
        } else {
            binding.tvTomorrowGanji.text = "오늘의 운세"
        }

        // Fetch needed element (deficient element) from API and display
        fetchAndDisplayNeededElement()

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

        // 일일 가이던스
        binding.tvKeyAdvice.text = "💡 ${guidance.keyAdvice}"
        binding.tvBestTime.text = "⏰ ${guidance.bestTime}"
        binding.tvLuckyDirection.text = "🧭 ${guidance.luckyDirection}"
        binding.tvLuckyColor.text = "🎨 ${guidance.luckyColor}"

        // 차크라 리딩
        displayChakraReadings(fortuneData.fortune.chakraReadings)
    }

    /**
     * Fetch needed element (deficient element) from API and display it
     */
    private fun fetchAndDisplayNeededElement() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getNeededElement()
                if (response.isSuccessful && response.body() != null) {
                    val neededElementKorean = response.body()!!.data.neededElement

                    withContext(Dispatchers.Main) {
                        // Display the needed element (deficient element)
                        val elementChar = getElementCharacter(neededElementKorean)
                        val elementColor = getElementColorFromString(neededElementKorean)
                        binding.tvElementCharacter.text = elementChar
                        binding.tvElementCharacter.setTextColor(elementColor)

                        // Update message to indicate this is the deficient element
                        val elementMessage = getDeficientElementMessage(neededElementKorean)
                        binding.tvElementMessage.text = elementMessage

                        // Update button text dynamically based on deficient element
                        val buttonText = getWhyDeficientButtonText(neededElementKorean)
                        binding.btnWhyDeficient.text = buttonText
                    }

                    Log.d("FortuneCardView", "Needed element displayed: $neededElementKorean")
                } else {
                    Log.w("FortuneCardView", "Failed to fetch needed element: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        // Show default if API fails
                        binding.tvElementCharacter.text = "運"
                        binding.tvElementCharacter.setTextColor(Color.parseColor("#FFD700"))
                        binding.tvElementMessage.text = "오늘의 기운을 느껴보세요"
                        binding.btnWhyDeficient.text = "왜 이 기운이 필요한가요?"
                    }
                }
            } catch (e: Exception) {
                Log.e("FortuneCardView", "Error fetching needed element", e)
                withContext(Dispatchers.Main) {
                    // Show default if error occurs
                    binding.tvElementCharacter.text = "運"
                    binding.tvElementCharacter.setTextColor(Color.parseColor("#FFD700"))
                    binding.tvElementMessage.text = "오늘의 기운을 느껴보세요"
                    binding.btnWhyDeficient.text = "왜 이 기운이 필요한가요?"
                }
            }
        }
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
     * 부족한 오행 원소 메시지 반환 (deficient element message)
     */
    private fun getDeficientElementMessage(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "오늘은 나무의 기운을 보충해야 합니다"
            "fire", "불", "화" -> "오늘은 불의 기운을 보충해야 합니다"
            "earth", "흙", "토" -> "오늘은 흙의 기운을 보충해야 합니다"
            "metal", "쇠", "금" -> "오늘은 쇠의 기운을 보충해야 합니다"
            "water", "물", "수" -> "오늘은 물의 기운을 보충해야 합니다"
            else -> "오늘의 기운을 느껴보세요"
        }
    }

    /**
     * 부족한 원소에 따라 버튼 텍스트를 동적으로 생성
     */
    private fun getWhyDeficientButtonText(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "나무가 왜 부족한가요?"
            "fire", "불", "화" -> "불이 왜 부족한가요?"
            "earth", "흙", "토" -> "흙이 왜 부족한가요?"
            "metal", "쇠", "금" -> "쇠가 왜 부족한가요?"
            "water", "물", "수" -> "물이 왜 부족한가요?"
            else -> "왜 이 기운이 필요한가요?"
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
