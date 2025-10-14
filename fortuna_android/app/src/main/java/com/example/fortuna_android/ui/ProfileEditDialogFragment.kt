package com.example.fortuna_android.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UpdateProfileRequest
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.databinding.FragmentProfileInputBinding
import kotlinx.coroutines.launch

class ProfileEditDialogFragment : DialogFragment() {
    private var _binding: FragmentProfileInputBinding? = null
    private val binding get() = _binding!!

    private var selectedGender = ""
    private var selectedSolarLunar = ""
    private var currentProfile: UserProfile? = null
    private var onProfileUpdated: (() -> Unit)? = null
    private var currentStep = 1

    companion object {
        private const val TAG = "ProfileEditDialog"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val ARG_PROFILE = "profile"

        fun newInstance(profile: UserProfile, onProfileUpdated: () -> Unit): ProfileEditDialogFragment {
            return ProfileEditDialogFragment().apply {
                this.currentProfile = profile
                this.onProfileUpdated = onProfileUpdated
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Make dialog full screen
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        prefillProfileData()
        updateStepUI()
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

        // Next button
        binding.nextButton.setOnClickListener {
            Log.d(TAG, "Next button clicked, currentStep=$currentStep")
            handleNextButton()
        }

        // Gender buttons
        binding.maleButton.setOnClickListener {
            selectedGender = "M"
            updateGenderButtons()
        }

        binding.femaleButton.setOnClickListener {
            selectedGender = "F"
            updateGenderButtons()
        }

        // Solar/Lunar buttons
        binding.solarButton.setOnClickListener {
            selectedSolarLunar = "solar"
            updateSolarLunarButtons()
            // Also update hidden radio group for compatibility
            binding.solarRadioButton.isChecked = true
        }

        binding.lunarButton.setOnClickListener {
            selectedSolarLunar = "lunar"
            updateSolarLunarButtons()
            // Also update hidden radio group for compatibility
            binding.lunarRadioButton.isChecked = true
        }
    }

    private fun prefillProfileData() {
        val binding = _binding ?: return
        val profile = currentProfile ?: return

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
                binding.solarRadioButton.isChecked = true
            }
            "lunar" -> {
                selectedSolarLunar = "lunar"
                binding.lunarRadioButton.isChecked = true
            }
        }
        updateSolarLunarButtons()

        // 성별 선택 - 기존 성별을 기본값으로 설정
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

    private fun updateGenderButtons() {
        val binding = _binding ?: return

        if (selectedGender == "M") {
            binding.maleButton.setBackgroundResource(R.drawable.button_selected)
            binding.maleButton.setTextColor(Color.BLACK)
            binding.femaleButton.setBackgroundResource(R.drawable.button_default)
            binding.femaleButton.setTextColor(Color.WHITE)
        } else if (selectedGender == "F") {
            binding.femaleButton.setBackgroundResource(R.drawable.button_selected)
            binding.femaleButton.setTextColor(Color.BLACK)
            binding.maleButton.setBackgroundResource(R.drawable.button_default)
            binding.maleButton.setTextColor(Color.WHITE)
        }
    }

    private fun updateSolarLunarButtons() {
        val binding = _binding ?: return

        if (selectedSolarLunar == "solar") {
            binding.solarButton.setBackgroundResource(R.drawable.button_selected)
            binding.solarButton.setTextColor(Color.BLACK)
            binding.lunarButton.setBackgroundResource(R.drawable.button_default)
            binding.lunarButton.setTextColor(Color.WHITE)
        } else if (selectedSolarLunar == "lunar") {
            binding.lunarButton.setBackgroundResource(R.drawable.button_selected)
            binding.lunarButton.setTextColor(Color.BLACK)
            binding.solarButton.setBackgroundResource(R.drawable.button_default)
            binding.solarButton.setTextColor(Color.WHITE)
        }
    }

    private fun updateStepUI() {
        val binding = _binding ?: return

        // Update step indicator
        binding.stepIndicator.text = "STEP $currentStep / 4"

        // Update main title based on step
        val titleText = when (currentStep) {
            1 -> "어떤 이름으로\n불러 드릴까요?"
            2 -> "감장아님의\n사주정보를 알려주세요."
            3 -> "태어난 시간\n정보가 필요해요."
            4 -> "감장아님의\n성별을 알려주세요."
            else -> "어떤 이름으로\n불러 드릴까요?"
        }
        binding.mainTitle.text = titleText

        // Update button text
        binding.nextButton.text = if (currentStep == 4) "프로필 저장" else "다음"

        // Show/hide step containers
        binding.step1Container.visibility = if (currentStep >= 1) View.VISIBLE else View.GONE
        binding.step2Container.visibility = if (currentStep >= 2) View.VISIBLE else View.GONE
        binding.step3Container.visibility = if (currentStep >= 3) View.VISIBLE else View.GONE
        binding.step4Container.visibility = if (currentStep >= 4) View.VISIBLE else View.GONE
    }

    private fun handleNextButton() {
        Log.d(TAG, "handleNextButton called, currentStep=$currentStep")
        when (currentStep) {
            1 -> {
                // Validate nickname
                val nickname = binding.nicknameEditText.text.toString().trim()
                Log.d(TAG, "Step 1: nickname=$nickname")
                if (nickname.isEmpty()) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                currentStep = 2
                Log.d(TAG, "Moving to step 2")
                updateStepUI()
            }
            2 -> {
                // Validate birth date and solar/lunar
                Log.d(TAG, "Step 2: selectedSolarLunar=$selectedSolarLunar")
                if (selectedSolarLunar.isEmpty()) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "음력/양력을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                currentStep = 3
                Log.d(TAG, "Moving to step 3")
                updateStepUI()
            }
            3 -> {
                // Birth time is selected from spinner, always valid
                Log.d(TAG, "Step 3: moving to step 4")
                currentStep = 4
                updateStepUI()
            }
            4 -> {
                // Validate gender and submit
                Log.d(TAG, "Step 4: selectedGender=$selectedGender, submitting profile")
                if (selectedGender.isEmpty()) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                submitProfile()
            }
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

        if (nickname.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (solarOrLunar.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "음력/양력을 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (gender.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
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
            if (isAdded) {
                Toast.makeText(requireContext(), "인증 토큰이 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            }
            dismiss()
            return
        }

        val request = UpdateProfileRequest(
            nickname = nickname,
            birthDate = birthDate,
            solarOrLunar = solarOrLunar,
            birthTimeUnits = birthTimeUnits,
            gender = gender
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateUserProfile("Bearer $token", request)

                if (response.isSuccessful) {
                    val updatedProfile = response.body()
                    Log.d(TAG, "프로필 업데이트 성공: $updatedProfile")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "프로필이 성공적으로 업데이트되었습니다!", Toast.LENGTH_SHORT).show()
                    }

                    // Notify parent fragment to refresh
                    onProfileUpdated?.invoke()
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 업데이트 실패: ${response.code()}, $errorBody")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "프로필 업데이트 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 업데이트 중 오류", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
