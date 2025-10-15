package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfile? = null

    companion object {
        private const val TAG = "ProfileFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserProfile()
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
    }

    fun loadUserProfile() {
        if (!isAdded) return

        // Get JWT token from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)

        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "No access token found")
            if (isAdded) {
                Toast.makeText(requireContext(), "Authentication required. Please log in again.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        Log.d(TAG, "Access token found: ${accessToken}")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $accessToken")

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        currentProfile = profile
                        updateUI(profile)
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "프로필을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "프로필 로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(profile: UserProfile) {
        val binding = _binding ?: return

        // 닉네임 표시
        val nickname = profile.nickname ?: profile.name ?: "사용자"
        binding.profileName.text = nickname

        // 양력/음력 태그 설정
        val solarOrLunar = profile.solarOrLunar
        if (solarOrLunar == "solar") {
            binding.profileCalendarTag.text = "양력"
            binding.profileCalendarTag.setBackgroundResource(R.drawable.tag_solar)
        } else if (solarOrLunar == "lunar") {
            binding.profileCalendarTag.text = "음력"
            binding.profileCalendarTag.setBackgroundResource(R.drawable.tag_lunar)
        }

        // 생년월일과 시간 통합 표시
        val birthDate = profile.birthDateSolar ?: profile.birthDateLunar ?: "미설정"
        val birthTime = profile.birthTimeUnits ?: "미설정"
        binding.profileBirthInfo.text = "$birthDate, $birthTime"

        // 오행 태그 설정 (일주의 천간 기준)
        val dailyGanji = profile.dailyGanji
        if (dailyGanji != null && dailyGanji.isNotEmpty()) {
            val cheongan = dailyGanji[0].toString()
            val element = getElementFromCheongan(cheongan)
            val elementCharacter = getElementCharacter(cheongan)
            val elementColor = getElementColor(cheongan)

            // 한자 표시 (색상 적용)
            binding.profileElementCharacter.text = elementCharacter
            binding.profileElementCharacter.setTextColor(elementColor)

            // 오행 태그 표시
            val fullText = "당신의 오행은 $element"
            val spannable = SpannableString(fullText)
            val elementStart = fullText.indexOf(element)
            if (elementStart >= 0) {
                spannable.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    elementStart,
                    elementStart + element.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.profileElementTag.text = spannable
        }

        // 사주팔자 타이틀
        binding.sajuViewText.text = "당신의 사주팔자"

        displaySaju(
            profile.yearlyGanji,
            profile.monthlyGanji,
            profile.dailyGanji,
            profile.hourlyGanji
        )
    }

    private fun getElementFromCheongan(cheongan: String): String {
        return when (cheongan) {
            "갑", "을" -> "나무"
            "병", "정" -> "불"
            "무", "기" -> "흙"
            "경", "신" -> "쇠"
            "임", "계" -> "물"
            else -> "미정"
        }
    }

    private fun getElementCharacter(cheongan: String): String {
        return when (cheongan) {
            "갑", "을" -> "木"
            "병", "정" -> "火"
            "무", "기" -> "土"
            "경", "신" -> "金"
            "임", "계" -> "水"
            else -> "?"
        }
    }

    private fun getElementColor(cheongan: String): Int {
        return when (cheongan) {
            "갑", "을" -> Color.parseColor("#0BEFA0")  // 나무 - 초록
            "병", "정" -> Color.parseColor("#F93E3E")  // 불 - 빨강
            "무", "기" -> Color.parseColor("#8B4513")  // 흙 - 갈색
            "경", "신" -> Color.parseColor("#C0C0C0")  // 쇠 - 은색
            "임", "계" -> Color.parseColor("#2BB3FC")  // 물 - 파랑
            else -> Color.WHITE
        }
    }

    private fun displaySaju(yearly: String?, monthly: String?, daily: String?, hourly: String?) {
        val binding = _binding ?: return

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
