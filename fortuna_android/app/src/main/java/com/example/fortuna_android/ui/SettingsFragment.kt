package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfile? = null

    companion object {
        private const val TAG = "SettingsFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 하단 네비게이션 바 숨기기
        (activity as? MainActivity)?.hideBottomNavigation()

        setupClickListeners()
        loadUserProfile()
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // 프로필 카드 클릭 → 프로필 편집 Fragment로 이동
        binding.profileCard.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_profile_edit)
        }

        // 알림 클릭
        binding.notificationItem.setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정 (추후 구현 예정)", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃 클릭
        binding.logoutItem.setOnClickListener {
            (activity as? MainActivity)?.logout()
        }

        // 탈퇴하기 클릭
        binding.deleteAccountItem.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        if (!isAdded) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("회원 탈퇴")
            .setMessage("정말로 탈퇴하시겠습니까?\n\n탈퇴 시 모든 사주 정보와 운세 기록이 삭제되며, 복구할 수 없습니다.")
            .setPositiveButton("탈퇴하기") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteAccount() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No token available for account deletion")
            Toast.makeText(requireContext(), "인증 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteAccount("Bearer $token")

                if (response.isSuccessful) {
                    if (isAdded) {
                        // Clear tokens first
                        prefs.edit().clear().apply()

                        // Show toast and logout after a short delay to avoid system UI error
                        Toast.makeText(requireContext(), "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()

                        // Delay logout to ensure toast is displayed
                        view?.postDelayed({
                            (activity as? MainActivity)?.logout()
                        }, 500)
                    }
                } else {
                    Log.e(TAG, "Account deletion failed: ${response.code()}")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "회원 탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting account", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserProfile() {
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
        binding.profileName.text = nickname
    }

    override fun onResume() {
        super.onResume()
        // 프로필 편집 후 돌아왔을 때 프로필 다시 로드
        loadUserProfile()
        // 하단 네비게이션 바 숨기기
        (activity as? MainActivity)?.hideBottomNavigation()
    }

    override fun onPause() {
        super.onPause()
        // 다른 Fragment로 이동할 때는 네비게이션 바 다시 보이기
        // (ProfileEditFragment에서 다시 숨길 것임)
        (activity as? MainActivity)?.showBottomNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

   
}
