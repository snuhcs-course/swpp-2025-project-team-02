package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UpdateProfileRequest
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentProfileEditBinding
import kotlinx.coroutines.launch

class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!

    private var selectedGender = ""
    private var selectedSolarLunar = ""
    private var currentProfile: UserProfile? = null

    companion object {
        private const val TAG = "ProfileEditFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 하단 네비게이션 바 숨기기
        (activity as? MainActivity)?.hideBottomNavigation()

        setupSpinners()
        setupClickListeners()
        loadUserProfile()
    }

    private fun setupSpinners() {
        val binding = _binding ?: return
        if (!isAdded) return

        val context = context ?: return

        // 년도 (1900-2025)
        val years = (1900..2025).map { it.toString() }
        binding.birthYearSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 월 (1-12)
        val months = (1..12).map { it.toString() + "월" }
        binding.birthMonthSpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 일 (1-31)
        val days = (1..31).map { it.toString() + "일" }
        binding.birthDaySpinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, days).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // 양력/음력 버튼
        binding.solarButton.setOnClickListener {
            selectedSolarLunar = "solar"
            updateSolarLunarButtons()
        }

        binding.lunarButton.setOnClickListener {
            selectedSolarLunar = "lunar"
            updateSolarLunarButtons()
        }

        // 성별 버튼
        binding.maleButton.setOnClickListener {
            selectedGender = "M"
            updateGenderButtons()
        }

        binding.femaleButton.setOnClickListener {
            selectedGender = "F"
            updateGenderButtons()
        }

        // 저장 버튼
        binding.saveButton.setOnClickListener {
            submitProfile()
        }
    }

    private fun loadUserProfile() {
        if (!isAdded) return

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)

        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "No access token found")
            Toast.makeText(requireContext(), "인증이 필요합니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        currentProfile = profile
                        prefillProfileData(profile)
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    Toast.makeText(requireContext(), "프로필을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                Toast.makeText(requireContext(), "프로필 로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prefillProfileData(profile: UserProfile) {
        val binding = _binding ?: return

        // 닉네임
        binding.nicknameEditText.setText(profile.nickname ?: "")

        // 생년월일 파싱 (양력 우선, 없으면 음력)
        val birthDate = profile.birthDateSolar ?: profile.birthDateLunar
        if (!birthDate.isNullOrEmpty()) {
            val parts = birthDate.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toIntOrNull() ?: 1
                val day = parts[2].toIntOrNull() ?: 1

                // 년도 선택
                val years = (1900..2025).map { it.toString() }
                val yearIndex = years.indexOf(year)
                if (yearIndex >= 0) {
                    binding.birthYearSpinner.setSelection(yearIndex)
                }

                // 월 선택 (1-12)
                binding.birthMonthSpinner.setSelection(month - 1)

                // 일 선택 (1-31)
                binding.birthDaySpinner.setSelection(day - 1)
            }
        }

        // 음력/양력 선택
        when (profile.solarOrLunar) {
            "solar" -> {
                selectedSolarLunar = "solar"
            }
            "lunar" -> {
                selectedSolarLunar = "lunar"
            }
        }
        updateSolarLunarButtons()

        // 성별 선택
        selectedGender = when (profile.gender) {
            "남자", "M" -> "M"
            "여자", "F" -> "F"
            else -> ""
        }
        updateGenderButtons()

        // 태어난 시간 선택
        if (!profile.birthTimeUnits.isNullOrEmpty()) {
            val timeArray = resources.getStringArray(R.array.birth_time_units)
            val timeIndex = timeArray.indexOfFirst { it.startsWith(profile.birthTimeUnits) }
            if (timeIndex >= 0) {
                binding.birthTimeSpinner.setSelection(timeIndex)
            }
        }
    }

    private fun updateSolarLunarButtons() {
        val binding = _binding ?: return

        if (selectedSolarLunar == "solar") {
            binding.solarButton.setBackgroundColor(Color.WHITE)
            binding.solarButton.setTextColor(Color.BLACK)
            binding.lunarButton.setBackgroundColor(Color.BLACK)
            binding.lunarButton.setTextColor(Color.WHITE)
        } else if (selectedSolarLunar == "lunar") {
            binding.lunarButton.setBackgroundColor(Color.WHITE)
            binding.lunarButton.setTextColor(Color.BLACK)
            binding.solarButton.setBackgroundColor(Color.BLACK)
            binding.solarButton.setTextColor(Color.WHITE)
        }
    }

    private fun updateGenderButtons() {
        val binding = _binding ?: return

        if (selectedGender == "M") {
            binding.maleButton.setBackgroundColor(Color.WHITE)
            binding.maleButton.setTextColor(Color.BLACK)
            binding.femaleButton.setBackgroundColor(Color.BLACK)
            binding.femaleButton.setTextColor(Color.WHITE)
        } else if (selectedGender == "F") {
            binding.femaleButton.setBackgroundColor(Color.WHITE)
            binding.femaleButton.setTextColor(Color.BLACK)
            binding.maleButton.setBackgroundColor(Color.BLACK)
            binding.maleButton.setTextColor(Color.WHITE)
        }
    }

    private fun submitProfile() {
        val binding = _binding ?: return

        val nickname = binding.nicknameEditText.text.toString().trim()

        val year = binding.birthYearSpinner.selectedItem.toString()
        val month = binding.birthMonthSpinner.selectedItem.toString().replace("월", "").padStart(2, '0')
        val day = binding.birthDaySpinner.selectedItem.toString().replace("일", "").padStart(2, '0')
        val birthDate = "$year-$month-$day"

        val solarOrLunar = selectedSolarLunar
        val birthTimeUnits = extractBirthTimeUnit(binding.birthTimeSpinner.selectedItem.toString())
        val gender = selectedGender

        // 유효성 검사
        if (nickname.isEmpty()) {
            Toast.makeText(requireContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (solarOrLunar.isEmpty()) {
            Toast.makeText(requireContext(), "음력/양력을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender.isEmpty()) {
            Toast.makeText(requireContext(), "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "프로필 업데이트 요청: nickname=$nickname, birthDate=$birthDate, solarOrLunar=$solarOrLunar, birthTimeUnits=$birthTimeUnits, gender=$gender")

        updateProfile(nickname, birthDate, solarOrLunar, birthTimeUnits, gender)
    }

    private fun extractBirthTimeUnit(selectedItem: String): String {
        if (selectedItem.isNullOrBlank()) return ""
        val parts = selectedItem.split(" ")
        return if (parts.isNotEmpty()) parts[0] else ""
    }

    private fun updateProfile(
        nickname: String,
        birthDate: String,
        solarOrLunar: String,
        birthTimeUnits: String,
        gender: String
    ) {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProfileRequest(
            nickname = nickname,
            inputBirthDate = birthDate,
            inputCalendarType = solarOrLunar,
            birthTimeUnits = birthTimeUnits,
            gender = gender
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateUserProfile(request)

                if (response.isSuccessful) {
                    val updatedProfile = response.body()
                    Log.d(TAG, "프로필 업데이트 성공: $updatedProfile")
                    Toast.makeText(requireContext(), "프로필이 성공적으로 업데이트되었습니다!", Toast.LENGTH_SHORT).show()

                    // 설정 화면으로 돌아가기
                    findNavController().navigateUp()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 업데이트 실패: ${response.code()}, $errorBody")
                    Toast.makeText(requireContext(), "프로필 업데이트 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 업데이트 중 오류", e)
                Toast.makeText(requireContext(), "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
