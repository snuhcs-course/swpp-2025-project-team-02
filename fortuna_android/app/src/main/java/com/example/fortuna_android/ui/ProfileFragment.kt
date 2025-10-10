package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.MainActivity
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

        binding.editProfileButton.setOnClickListener {
            val profile = currentProfile
            if (profile != null) {
                showEditProfileDialog(profile)
            } else {
                Toast.makeText(requireContext(), "프로필 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutButton.setOnClickListener {
            // Notify parent activity to handle logout
            (activity as? MainActivity)?.logout()
        }
    }

    private fun showEditProfileDialog(profile: UserProfile) {
        val dialog = ProfileEditDialogFragment.newInstance(profile) {
            // Callback when profile is updated
            loadUserProfile()
        }
        dialog.show(childFragmentManager, "ProfileEditDialog")
    }

    fun loadUserProfile() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No token available")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")

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

        val nickname = profile.nickname ?: profile.name
        binding.welcomeTextView.text = "환영합니다, ${nickname}님!"
        binding.sajuViewText.text = "${nickname}님의 사주팔자"

        displaySaju(
            profile.yearlyGanji,
            profile.monthlyGanji,
            profile.dailyGanji,
            profile.hourlyGanji
        )
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
