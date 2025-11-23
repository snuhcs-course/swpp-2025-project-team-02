package com.example.fortuna_android.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.fortuna_android.R
import com.example.fortuna_android.api.ChakraReading
import com.example.fortuna_android.api.TodayFortuneData
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.BuildConfig
import com.example.fortuna_android.databinding.CardFortuneBinding
import com.example.fortuna_android.tts.AndroidTtsAdapter
import com.example.fortuna_android.tts.FortuneTtsManager
import com.example.fortuna_android.tts.OpenAiRealtimeTtsAdapter
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
    private var onRefreshFortuneLongClickListener: (() -> Unit)? = null

    // Store base entropy score for bonus calculation
    private var baseEntropyScore: Int = 0

    // TTS manager for reading fortune text
    // Toggle between Android TTS (fast, free) and OpenAI Realtime TTS (high quality, paid)
    private val ttsManager: FortuneTtsManager by lazy {
        val useOpenAiTts = BuildConfig.OPENAI_API_KEY.isNotEmpty()

        val adapter = if (useOpenAiTts) {
            Log.d("FortuneCardView", "Using OpenAI Realtime TTS - HILARIOUS MODE")
            OpenAiRealtimeTtsAdapter(
                apiKey = BuildConfig.OPENAI_API_KEY,
                voice = "verse" // Shimmer: warm, energetic female - PERFECT for crazy energetic fortune god!
                // Other options: alloy, echo, fable, onyx, nova
            )
        } else {
            Log.d("FortuneCardView", "Using Android native TTS - CHIPMUNK MODE")
            AndroidTtsAdapter(
                context,
                pitch = 1.5f,      // Higher pitch = chipmunk voice (1.0 = normal)
                speechRate = 1.3f  // Faster speech (1.0 = normal)
            )
        }

        FortuneTtsManager(adapter)
    }

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

        // 오늘의 기운 보충하러가기 버튼 롱 클릭 리스너 (3초)
        binding.btnRefreshFortune.setOnLongClickListener {
            onRefreshFortuneLongClickListener?.invoke()
            true // Consume the event
        }

        // 버튼 애니메이션 시작
        startButtonAnimations()

        // Set up TTS click listeners for fortune text sections
        setupTtsClickListeners()
    }

    /**
     * Set up click listeners for TTS on fortune text sections
     */
    private fun setupTtsClickListeners() {
        // 오행 균형 설명 클릭 시 TTS 재생/중단
        binding.tvElementBalanceDescription.setOnClickListener {
            ttsManager.handleTextClick(
                textView = binding.tvElementBalanceDescription,
                glowBackground = R.drawable.text_glow_active,
                normalBackground = R.drawable.text_normal
            )
        }

        // 사주를 좋게 하는 방법 클릭 시 TTS 재생/중단
        binding.tvDailyGuidance.setOnClickListener {
            ttsManager.handleTextClick(
                textView = binding.tvDailyGuidance,
                glowBackground = R.drawable.text_glow_active,
                normalBackground = R.drawable.text_normal
            )
        }
    }

    /**
     * 버튼에 반짝임 애니메이션 시작
     */
    private fun startButtonAnimations() {
        // 버튼에 반짝이는 효과 (알파와 스케일)
        val shimmerAnimation = AnimationUtils.loadAnimation(context, R.anim.shimmer_pulse)
        binding.btnRefreshFortune.startAnimation(shimmerAnimation)
    }

    /**
     * 오늘의 기운 보충하러가기 버튼 클릭 리스너 설정
     */
    fun setOnRefreshFortuneClickListener(listener: () -> Unit) {
        onRefreshFortuneClickListener = listener
    }

    /**
     * 오늘의 기운 보충하러가기 버튼 롱 클릭 리스너 설정 (hidden feature)
     */
    fun setOnRefreshFortuneLongClickListener(listener: () -> Unit) {
        onRefreshFortuneLongClickListener = listener
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

        // Store base entropy score
        baseEntropyScore = fortuneData.fortuneScore.entropyScore.toInt()

        // Fetch today's progress (needed element + collection status) from API and display
        fetchTodayProgressAndDisplay()

        // 오늘의 운세 요약을 elementMessage에 표시
        binding.tvElementMessage.text = fortuneData.fortune.todayFortuneSummary

        // 새로운 섹션: 오행 균형 설명
        binding.tvElementBalanceDescription.text = fortuneData.fortune.todayElementBalanceDescription

        // 새로운 섹션: 일일 가이던스
        binding.tvDailyGuidance.text = fortuneData.fortune.todayDailyGuidance

        // Load fortune image if available
        loadFortuneImage(fortuneData.fortuneImageUrl)
    }

    /**
     * Load fortune image from URL using Glide
     * Backend provides presigned URL with temporary access
     */
    private fun loadFortuneImage(imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            // Hide image view if no URL provided
            binding.ivFortuneImage.visibility = View.GONE
            return
        }

        // Show image view
        binding.ivFortuneImage.visibility = View.VISIBLE

        // Load image with Glide (presigned URL from backend)
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.ivFortuneImage)
    }

    /**
     * Fetch today's progress (needed element + collection status) from API and display it
     */
    private fun fetchTodayProgressAndDisplay() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTodayProgress()
                if (response.isSuccessful && response.body() != null) {
                    val progressData = response.body()!!.data
                    val neededElementKorean = progressData.neededElement
                    val currentCount = progressData.currentCount

                    withContext(Dispatchers.Main) {
                        // Display the needed element (deficient element)
                        val elementChar = getElementCharacter(neededElementKorean)
                        val elementColor = getElementColorFromString(neededElementKorean)
                        binding.tvElementCharacter.text = elementChar
                        binding.tvElementCharacter.setTextColor(elementColor)

                        // Update collection progress dots
                        updateProgressDots(currentCount, elementColor)

                        // Calculate and display bonus score
                        updateScoreWithBonus(currentCount)
                    }

                    Log.d("FortuneCardView", "Today's progress: $currentCount/5 - $neededElementKorean")
                } else {
                    Log.w("FortuneCardView", "Failed to fetch today's progress: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        // Show default if API fails
                        binding.tvElementCharacter.text = "運"
                        binding.tvElementCharacter.setTextColor(Color.parseColor("#FFD700"))
                        updateProgressDots(0, Color.parseColor("#FFD700"))
                        updateScoreWithBonus(0)
                    }
                }
            } catch (e: Exception) {
                Log.e("FortuneCardView", "Error fetching today's progress", e)
                withContext(Dispatchers.Main) {
                    // Show default if error occurs
                    binding.tvElementCharacter.text = "運"
                    binding.tvElementCharacter.setTextColor(Color.parseColor("#FFD700"))
                    updateProgressDots(0, Color.parseColor("#FFD700"))
                    updateScoreWithBonus(0)
                }
            }
        }
    }

    /**
     * Update score display with collection bonus
     */
    private fun updateScoreWithBonus(currentCount: Int) {
        val bonusPerElement = 4
        val bonus = currentCount * bonusPerElement
        val totalScore = baseEntropyScore + bonus

        // Update total score display
        binding.tvOverallFortune.text = totalScore.toString()

        // Update bonus display
        if (currentCount > 0) {
            binding.tvBonusScore.text = "+$bonus↗"
            binding.tvBonusScore.visibility = View.VISIBLE
        } else {
            binding.tvBonusScore.visibility = View.GONE
        }
    }

    /**
     * Update the 5 progress dots based on current collection count
     */
    private fun updateProgressDots(currentCount: Int, elementColor: Int) {
        val dots = listOf(
            binding.dotProgress1,
            binding.dotProgress2,
            binding.dotProgress3,
            binding.dotProgress4,
            binding.dotProgress5
        )

        val emptyColor = Color.parseColor("#3A3A3A")

        // Check if completed (5/5)
        if (currentCount >= 5) {
            // Show completion state with celebration
            showCompletionCelebration(elementColor)
        } else {
            // Normal state
            binding.tvCollectionLabel.text = "오늘 보충한 기운"
            binding.tvCollectionLabel.setTextColor(Color.parseColor("#888888"))

            dots.forEachIndexed { index, dot ->
                val color = if (index < currentCount) elementColor else emptyColor

                // Create circular drawable with the appropriate color
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setColor(color)
                dot.background = drawable
            }
        }
    }

    /**
     * Show celebration effect when all 5 elements are collected
     */
    private fun showCompletionCelebration(elementColor: Int) {
        val dots = listOf(
            binding.dotProgress1,
            binding.dotProgress2,
            binding.dotProgress3,
            binding.dotProgress4,
            binding.dotProgress5
        )

        // Update label with celebration text
        binding.tvCollectionLabel.text = "✨기운을 모두 채웠어요!✨"
        binding.tvCollectionLabel.setTextColor(Color.parseColor("#FFD700")) // Gold color

        // Make all dots filled with element color
        dots.forEach { dot ->
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(elementColor)

            // Add golden stroke for celebration effect
            drawable.setStroke(2, Color.parseColor("#FFD700"))

            dot.background = drawable
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
     * Stop TTS when view is detached from window
     * This ensures TTS stops when user navigates away or view becomes invisible
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d("FortuneCardView", "View detached, stopping TTS")
        ttsManager.onViewDetached(R.drawable.text_normal)
    }

    /**
     * Release TTS resources when view is finalized
     */
    @Suppress("deprecation")
    protected fun finalize() {
        ttsManager.release()
    }

}
