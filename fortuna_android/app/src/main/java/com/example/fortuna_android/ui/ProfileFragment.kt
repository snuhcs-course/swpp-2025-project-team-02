package com.example.fortuna_android.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
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
import com.example.fortuna_android.BuildConfig
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.common.AppColors
import com.example.fortuna_android.databinding.FragmentProfileBinding
import com.example.fortuna_android.util.CustomToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfile? = null
    private lateinit var mGoogleSignInClient: GoogleSignInClient

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

        setupGoogleSignIn()
        setupClickListeners()
        loadUserProfile()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
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

        // 회원 탈퇴 텍스트 (밑줄 추가)
        val deleteText = binding.btnDeleteAccount.text.toString()
        val spannableString = SpannableString(deleteText)
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            deleteText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.btnDeleteAccount.text = spannableString

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

                    // Google Sign Out first
                    mGoogleSignInClient.signOut().addOnCompleteListener {
                        if (!isAdded) return@addOnCompleteListener

                        // Clear local data
                        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()

                        Log.d(TAG, "로컬 로그아웃 완료 - Google SignOut 및 토큰 제거됨")

                        // Finish activity first to prevent onResume from being called
                        activity?.finish()

                        // Then navigate to sign in activity
                        val intent = Intent(requireContext(), AuthContainerActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
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
                CustomToast.show(requireContext(), "재인증이 필요합니다. 다시 로그인해 주세요.")
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
        var birthDate: String
        if (solarOrLunar == "solar") {
            birthDate = profile.birthDateSolar ?: "미설정"
        } else if (solarOrLunar == "lunar") {
            birthDate = profile.birthDateLunar ?: "미설정"
        } else {
            birthDate = "미설정"
        }
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
                    StyleSpan(Typeface.BOLD),
                    elementStart,
                    elementStart + element.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.profileElementTag.text = spannable
        }

        // 수집한 원소 표시
        updateCollectedElements(profile)
    }

    /**
     * Update collected elements badges with proper colors
     * Order: 목(Wood-Green), 화(Fire-Red), 토(Earth-Orange), 금(Metal-Gray), 수(Water-Blue)
     */
    private fun updateCollectedElements(profile: UserProfile) {
        val binding = _binding ?: return
        val collectionStatus = profile.collectionStatus

        // Element colors from AppColors
        val woodColor = Color.parseColor(AppColors.ELEMENT_WOOD)
        val fireColor = Color.parseColor(AppColors.ELEMENT_FIRE)
        val earthColor = Color.parseColor(AppColors.ELEMENT_EARTH)
        val metalColor = Color.parseColor(AppColors.ELEMENT_METAL)
        val waterColor = Color.parseColor(AppColors.ELEMENT_WATER)

        // Update badge 1: 목 (Wood) - Green
        updateElementBadge(
            badge = binding.elementBadge1,
            count = collectionStatus?.wood ?: 0,
            color = woodColor,
            elementType = "wood",
            elementKr = "목"
        )

        // Update badge 2: 화 (Fire) - Red
        updateElementBadge(
            badge = binding.elementBadge2,
            count = collectionStatus?.fire ?: 0,
            color = fireColor,
            elementType = "fire",
            elementKr = "화"
        )

        // Update badge 3: 토 (Earth) - Orange
        updateElementBadge(
            badge = binding.elementBadge3,
            count = collectionStatus?.earth ?: 0,
            color = earthColor,
            elementType = "earth",
            elementKr = "토"
        )

        // Update badge 4: 금 (Metal) - Gray
        updateElementBadge(
            badge = binding.elementBadge4,
            count = collectionStatus?.metal ?: 0,
            color = metalColor,
            elementType = "metal",
            elementKr = "금"
        )

        // Update badge 5: 수 (Water) - Blue
        updateElementBadge(
            badge = binding.elementBadge5,
            count = collectionStatus?.water ?: 0,
            color = waterColor,
            elementType = "water",
            elementKr = "수"
        )

        Log.d(TAG, "Collected elements updated: 목=${collectionStatus?.wood}, 화=${collectionStatus?.fire}, 토=${collectionStatus?.earth}, 금=${collectionStatus?.metal}, 수=${collectionStatus?.water}")
    }

    /**
     * Update a single element badge with count, color, and click listener
     */
    private fun updateElementBadge(
        badge: TextView,
        count: Int,
        color: Int,
        elementType: String,
        elementKr: String
    ) {
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

        // Make badge clickable
        badge.isClickable = true
        badge.isFocusable = true

        // Add click listener to show history dialog
        badge.setOnClickListener {
            showElementHistoryDialog(elementType, elementKr, color)
        }
    }

    /**
     * Show element history dialog
     */
    private fun showElementHistoryDialog(elementType: String, elementKr: String, color: Int) {
        val dialog = ElementHistoryDialogFragment.newInstance(elementType, elementKr, color)
        dialog.show(childFragmentManager, "ElementHistoryDialog")
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
        return AppColors.getElementColorByStem(cheongan)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
