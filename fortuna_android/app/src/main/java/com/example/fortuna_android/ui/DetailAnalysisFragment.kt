package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentDetailAnalysisBinding
import com.example.fortuna_android.util.CustomToast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailAnalysisFragment : Fragment() {
    private var _binding: FragmentDetailAnalysisBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "DetailAnalysisFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"

        fun newInstance() = DetailAnalysisFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()
        loadTodayFortune()
    }

    private fun loadUserProfile() {
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
                CustomToast.show(requireContext(), "로그인이 필요합니다.")
            }
            return
        }

        Log.d(TAG, "Loading user profile for saju data...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        updateUI(profile)
                        hideLoading()
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    hideLoading()
                    if (isAdded) {
                        CustomToast.show(requireContext(), "사주 정보를 불러올 수 없습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                hideLoading()
                if (isAdded) {
                    CustomToast.show(requireContext(), "오류: ${e.message}")
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

    private fun loadTodayFortune() {
        if (!isAdded) return

        // Get JWT token from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)

        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "No access token found for today fortune")
            return
        }

        Log.d(TAG, "Loading today's fortune for saju data...")

        lifecycleScope.launch {
            try {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val response = RetrofitClient.instance.getTodayFortune(todayDate)

                if (response.isSuccessful) {
                    val fortuneResponse = response.body()
                    if (fortuneResponse != null) {
                        updateTodaySajuUI(fortuneResponse.data)
                    }
                } else {
                    Log.e(TAG, "오늘의 운세 로드 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "오늘의 운세 로드 중 오류", e)
            }
        }
    }

    private fun updateTodaySajuUI(fortuneData: com.example.fortuna_android.api.TodayFortuneData) {
        val binding = _binding ?: return

        // 오늘의 사주팔자 표시
        val elements = fortuneData.fortuneScore.elements
        binding.todaySajuPaljaView.setTodaySajuData(
            daeun = elements["대운"],
            saeun = elements["세운"],
            wolun = elements["월운"],
            ilun = elements["일운"]
        )
    }

    private fun updateUI(profile: UserProfile) {
        val binding = _binding ?: return

        // 당신의 사주팔자 표시
        binding.sajuPaljaView.setSajuData(
            yearly = profile.yearlyGanji,
            monthly = profile.monthlyGanji,
            daily = profile.dailyGanji,
            hourly = profile.hourlyGanji
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
