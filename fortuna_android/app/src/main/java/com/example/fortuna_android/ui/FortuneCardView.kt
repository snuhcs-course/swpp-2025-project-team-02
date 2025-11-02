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

        // 전체 운세 점수 (entropyScore / 100)
        val fortuneScore = (fortuneData.fortuneScore.entropyScore / 100.0).toInt()
        binding.tvOverallFortune.text = fortuneScore.toString()

        // 새로운 섹션: 오행 균형 설명
        binding.tvElementBalanceDescription.text = fortuneData.fortune.todayElementBalanceDescription

        // 새로운 섹션: 일일 가이던스
        binding.tvDailyGuidance.text = fortuneData.fortune.todayDailyGuidance
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

                        // Update "왜 부족한가요?" title dynamically
                        val whyDeficientTitle = getWhyDeficientTitle(neededElementKorean)
                        binding.tvWhyDeficientTitle.text = whyDeficientTitle
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
     * 부족한 원소에 따라 제목을 동적으로 생성
     */
    private fun getWhyDeficientTitle(element: String): String {
        return when (element.lowercase()) {
            "wood", "나무", "목" -> "나무가 왜 부족한가요?"
            "fire", "불", "화" -> "불이 왜 부족한가요?"
            "earth", "흙", "토" -> "흙이 왜 부족한가요?"
            "metal", "쇠", "금" -> "쇠가 왜 부족한가요?"
            "water", "물", "수" -> "물이 왜 부족한가요?"
            else -> "왜 이 기운이 필요한가요?"
        }
    }

}
