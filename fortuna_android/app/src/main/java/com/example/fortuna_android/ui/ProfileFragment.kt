package com.example.fortuna_android.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.AuthContainerActivity
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentProfileBinding
import com.example.fortuna_android.util.CustomToast
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

        // 프로필 수정하기 버튼
        binding.btnEditProfile.setOnClickListener {
            // ProfileEditFragment로 이동
            val navController = findNavController()
            navController.navigate(R.id.profileEditFragment)
        }

        // 로그아웃 버튼
        binding.btnLogout.setOnClickListener {
            (activity as? MainActivity)?.logout()
        }

        // 회원 탈퇴 버튼
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원 탈퇴")
            .setMessage("정말로 회원 탈퇴하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.")
            .setPositiveButton("탈퇴") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteAccount() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteAccount()

                if (response.isSuccessful) {
                    Log.d(TAG, "회원 탈퇴 성공")
                    if (isAdded) {
                        CustomToast.show(requireContext(), "회원 탈퇴가 완료되었습니다.")
                    }

                    // Clear local data and navigate to sign in
                    val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    // Navigate to sign in activity
                    val intent = Intent(requireContext(), AuthContainerActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                } else {
                    Log.e(TAG, "회원 탈퇴 실패: ${response.code()}")
                    if (isAdded) {
                        CustomToast.show(requireContext(), "회원 탈퇴에 실패했습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "회원 탈퇴 중 오류", e)
                if (isAdded) {
                    CustomToast.show(requireContext(), "오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }

    fun loadUserProfile() {
        if (!isAdded) return

        // 로딩 시작
        showLoading()

        // Get JWT token from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)

        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "No access token found")
            hideLoading()
            if (isAdded) {
                CustomToast.show(requireContext(), "Authentication required. Please log in again.")
            }
            return
        }

        Log.d(TAG, "Access token found: ${accessToken}")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        currentProfile = profile
                        updateUI(profile)
                        hideLoading()
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    hideLoading()
                    if (isAdded) {
                        CustomToast.show(requireContext(), "프로필을 불러올 수 없습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                hideLoading()
                if (isAdded) {
                    CustomToast.show(requireContext(), "프로필 로드 오류: ${e.message}")
                }
            }
        }
    }

    private fun showLoading() {
        val binding = _binding ?: return
        binding.loadingContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        val binding = _binding ?: return
        binding.loadingContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    private fun updateUI(profile: UserProfile) {
        val binding = _binding ?: return

        // 닉네임 표시
        val displayName = profile.nickname?.takeIf { it.isNotBlank() } ?: "사용자"
        binding.profileName.text = displayName

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

        // 수집한 원소 표시 (색상과 카운트 적용)
        updateCollectedElements(profile)
    }

    /**
     * Update collected elements badges with proper colors
     * Order: 목(Wood-Green), 화(Fire-Red), 토(Earth-Orange), 금(Metal-Gray), 수(Water-Blue)
     */
    private fun updateCollectedElements(profile: UserProfile) {
        val binding = _binding ?: return
        val collectionStatus = profile.collectionStatus

        // Element colors matching SajuPaljaView
        val woodColor = Color.parseColor("#0BEFA0")   // Green
        val fireColor = Color.parseColor("#F93E3E")   // Red
        val earthColor = Color.parseColor("#FF9500")  // Orange
        val metalColor = Color.parseColor("#C1BFBF")  // Gray
        val waterColor = Color.parseColor("#2BB3FC")  // Blue

        // Update badge 1: 목 (Wood) - Green
        updateElementBadge(
            badge = binding.elementBadge1,
            count = collectionStatus?.wood ?: 0,
            color = woodColor
        )

        // Update badge 2: 화 (Fire) - Red
        updateElementBadge(
            badge = binding.elementBadge2,
            count = collectionStatus?.fire ?: 0,
            color = fireColor
        )

        // Update badge 3: 토 (Earth) - Orange
        updateElementBadge(
            badge = binding.elementBadge3,
            count = collectionStatus?.earth ?: 0,
            color = earthColor
        )

        // Update badge 4: 금 (Metal) - Gray
        updateElementBadge(
            badge = binding.elementBadge4,
            count = collectionStatus?.metal ?: 0,
            color = metalColor
        )

        // Update badge 5: 수 (Water) - Blue
        updateElementBadge(
            badge = binding.elementBadge5,
            count = collectionStatus?.water ?: 0,
            color = waterColor
        )

        Log.d(TAG, "Collected elements updated: 목=${collectionStatus?.wood}, 화=${collectionStatus?.fire}, 토=${collectionStatus?.earth}, 금=${collectionStatus?.metal}, 수=${collectionStatus?.water}")

        // Setup click listeners for badges
        setupElementBadgeClickListeners()
    }

    /**
     * Update a single element badge with count and color
     */
    private fun updateElementBadge(badge: TextView, count: Int, color: Int) {
        badge.text = count.toString()

        // Create rounded rectangle background with element color
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 8f
        }
        badge.background = background

        // Set text color to white for better visibility
        badge.setTextColor(Color.WHITE)
    }

    private fun setupElementBadgeClickListeners() {
        val binding = _binding ?: return

        // 목 (Wood)
        binding.elementBadge1.setOnClickListener {
            showCollectionHistory("wood", "목")
        }

        // 화 (Fire)
        binding.elementBadge2.setOnClickListener {
            showCollectionHistory("fire", "화")
        }

        // 토 (Earth)
        binding.elementBadge3.setOnClickListener {
            showCollectionHistory("earth", "토")
        }

        // 금 (Metal)
        binding.elementBadge4.setOnClickListener {
            showCollectionHistory("metal", "금")
        }

        // 수 (Water)
        binding.elementBadge5.setOnClickListener {
            showCollectionHistory("water", "수")
        }
    }

    private fun showCollectionHistory(chakraType: String, koreanName: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getCollectionHistory(chakraType)

                if (response.isSuccessful && response.body() != null) {
                    val historyData = response.body()!!.data

                    if (historyData.collections.isEmpty()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("$koreanName 원소 수집 내역")
                            .setMessage("수집한 원소가 없습니다.")
                            .setPositiveButton("확인", null)
                            .show()
                    } else {
                        // Format dates for display
                        val dateList = historyData.collections.map { item ->
                            val dateTime = try {
                                // Parse ISO timestamp and format to readable date
                                val instant = java.time.Instant.parse(item.collectedAt)
                                val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                                "${localDateTime.year}년 ${localDateTime.monthValue}월 ${localDateTime.dayOfMonth}일 ${localDateTime.hour}시 ${localDateTime.minute}분"
                            } catch (e: Exception) {
                                // Fallback to date field if timestamp parsing fails
                                item.date
                            }
                            dateTime
                        }.toTypedArray()

                        AlertDialog.Builder(requireContext())
                            .setTitle("$koreanName 원소 수집 내역 (총 ${historyData.totalCount}개)")
                            .setItems(dateList, null)
                            .setPositiveButton("확인", null)
                            .show()
                    }
                } else {
                    Log.e(TAG, "Failed to load collection history: ${response.code()}")
                    if (isAdded) {
                        CustomToast.show(requireContext(), "수집 내역을 불러올 수 없습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading collection history", e)
                if (isAdded) {
                    CustomToast.show(requireContext(), "오류가 발생했습니다: ${e.message}")
                }
            }
        }
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
