package com.example.fortuna_android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fortuna_android.util.CustomToast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.AuthContainerActivity
import com.example.fortuna_android.BuildConfig
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentSettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var currentProfile: UserProfile? = null
    private lateinit var mGoogleSignInClient: GoogleSignInClient

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
            CustomToast.show(requireContext(), "알림 설정 (추후 구현 예정)")
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
            CustomToast.show(requireContext(), "인증 정보를 찾을 수 없습니다.")
            return
        }

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
                    Log.e(TAG, "Account deletion failed: ${response.code()}")
                    if (isAdded) {
                        CustomToast.show(requireContext(), "회원 탈퇴에 실패했습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting account", e)
                if (isAdded) {
                    CustomToast.show(requireContext(), "오류가 발생했습니다: ${e.message}")
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
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        currentProfile = profile
                        updateUI(profile)
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    if (isAdded) {
                        CustomToast.show(requireContext(), "프로필을 불러올 수 없습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                if (isAdded) {
                    CustomToast.show(requireContext(), "프로필 로드 오류: ${e.message}")
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
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

   
}
