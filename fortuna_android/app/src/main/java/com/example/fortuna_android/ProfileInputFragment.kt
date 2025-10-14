package com.example.fortuna_android

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.fortuna_android.databinding.FragmentProfileInputBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UpdateProfileRequest
import kotlinx.coroutines.launch

class ProfileInputFragment : Fragment() {
    private var _binding: FragmentProfileInputBinding? = null
    private val binding get() = _binding!!

    private var selectedGender = ""
    private var selectedSolarLunar = ""
    private var currentStep = 1

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
        binding.birthYearSpinner.setSelection(years.size - 1)

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
            binding.maleButton.isSelected = true
            binding.femaleButton.isSelected = false
        }

        binding.femaleButton.setOnClickListener {
            selectedGender = "F"
            binding.maleButton.isSelected = false
            binding.femaleButton.isSelected = true
        }

        // Solar/Lunar buttons
        binding.solarButton.setOnClickListener {
            Log.d(TAG, "Solar button clicked")
            selectedSolarLunar = "solar"
            binding.solarButton.isSelected = true
            binding.lunarButton.isSelected = false
            // Also update hidden radio group for compatibility
            binding.solarRadioButton.isChecked = true
        }

        binding.lunarButton.setOnClickListener {
            Log.d(TAG, "Lunar button clicked")
            selectedSolarLunar = "lunar"
            binding.solarButton.isSelected = false
            binding.lunarButton.isSelected = true
            // Also update hidden radio group for compatibility
            binding.lunarRadioButton.isChecked = true
        }
    }



    private fun updateStepUI() {
        val binding = _binding ?: return

        // Update step indicator
        binding.stepIndicator.text = "STEP $currentStep / 4"

        // Update main title based on step
        val titleText = when (currentStep) {
            1 -> "어떤 이름으로\n불러 드릴까요?"
            2 -> "사주 정보를 알려주세요."
            3 -> "태어난 시간\n정보가 필요해요."
            4 -> "성별을 알려주세요."
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

        // Scroll to top to show new input field
        binding.contentScrollView.post {
            binding.contentScrollView.smoothScrollTo(0, 0)
        }
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
            navigateToSignIn()
            return
        }

        val request = UpdateProfileRequest(
            nickname = nickname,
            inputBirthDate = birthDate,
            inputCalendarType = solarOrLunar,
            birthTimeUnits = birthTimeUnits,
            gender = gender
        )

        Log.d(TAG, "Request 내용: $request")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateUserProfile("Bearer $token", request)

                if (response.isSuccessful) {
                    val updatedProfile = response.body()
                    Log.d(TAG, "프로필 업데이트 성공: $updatedProfile")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "프로필이 성공적으로 업데이트되었습니다!", Toast.LENGTH_SHORT).show()
                    }

                    navigateToMain()
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

    private fun navigateToMain() {
        if (!isAdded) return
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun navigateToSignIn() {
        (activity as? AuthContainerActivity)?.showSignInFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileInputFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}
