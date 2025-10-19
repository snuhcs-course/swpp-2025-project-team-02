package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.fortuna_android.api.ElementPillar
import com.example.fortuna_android.databinding.ViewTodaySajuPaljaBinding

class TodaySajuPaljaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTodaySajuPaljaBinding

    init {
        binding = ViewTodaySajuPaljaBinding.inflate(LayoutInflater.from(context), this, true)
    }

    /**
     * 오늘의 사주 팔자 데이터 설정
     * @param daeun 대운
     * @param saeun 세운
     * @param wolun 월운
     * @param ilun 일운
     */
    fun setTodaySajuData(
        daeun: ElementPillar?,
        saeun: ElementPillar?,
        wolun: ElementPillar?,
        ilun: ElementPillar?
    ) {
        // 대운
        daeun?.let {
            setUnCard(
                binding.daeunStemCard,
                binding.daeunStem,
                binding.daeunStemElement,
                it.stem.koreanName,
                it.stem.element
            )
            setUnCard(
                binding.daeunBranchCard,
                binding.daeunBranch,
                binding.daeunBranchElement,
                it.branch.koreanName,
                it.branch.element
            )
        }

        // 세운
        saeun?.let {
            setUnCard(
                binding.saeunStemCard,
                binding.saeunStem,
                binding.saeunStemElement,
                it.stem.koreanName,
                it.stem.element
            )
            setUnCard(
                binding.saeunBranchCard,
                binding.saeunBranch,
                binding.saeunBranchElement,
                it.branch.koreanName,
                it.branch.element
            )
        }

        // 월운
        wolun?.let {
            setUnCard(
                binding.wolunStemCard,
                binding.wolunStem,
                binding.wolunStemElement,
                it.stem.koreanName,
                it.stem.element
            )
            setUnCard(
                binding.wolunBranchCard,
                binding.wolunBranch,
                binding.wolunBranchElement,
                it.branch.koreanName,
                it.branch.element
            )
        }

        // 일운
        ilun?.let {
            setUnCard(
                binding.ilunStemCard,
                binding.ilunStem,
                binding.ilunStemElement,
                it.stem.koreanName,
                it.stem.element
            )
            setUnCard(
                binding.ilunBranchCard,
                binding.ilunBranch,
                binding.ilunBranchElement,
                it.branch.koreanName,
                it.branch.element
            )
        }
    }

    /**
     * 타이틀 설정
     */
    fun setTitle(title: String) {
        binding.todaySajuTitle.text = title
    }

    private fun setUnCard(
        cardLayout: LinearLayout,
        ganjiTextView: TextView,
        elementTextView: TextView,
        koreanName: String,
        element: String
    ) {
        // 한글 간지 표시
        ganjiTextView.text = koreanName

        // 오행 정보 표시
        val elementInfo = getElementInfo(element)
        elementTextView.text = elementInfo

        // 배경색 설정 with rounded corners
        val color = getElementColor(element)
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.cornerRadius = 12f * context.resources.displayMetrics.density
        cardLayout.background = drawable
    }

    private fun getElementInfo(element: String): String {
        return when (element.lowercase()) {
            "목", "wood" -> "木, 나무"
            "화", "fire" -> "火, 불"
            "토", "earth" -> "土, 흙"
            "금", "metal" -> "金, 쇠"
            "수", "water" -> "水, 물"
            else -> "?, ?"
        }
    }

    private fun getElementColor(element: String): Int {
        return when (element.lowercase()) {
            "목", "wood" -> Color.parseColor("#0BEFA0")  // 초록
            "화", "fire" -> Color.parseColor("#F93E3E")   // 빨강
            "토", "earth" -> Color.parseColor("#FF9500")  // 노랑
            "금", "metal" -> Color.parseColor("#C1BFBF")  // 흰색
            "수", "water" -> Color.parseColor("#2BB3FC")  // 파랑
            else -> Color.parseColor("#CCCCCC")
        }
    }
}
