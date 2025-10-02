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
    }


    private fun setupSpinners() {
        val binding = _binding ?: return
        if (!isAdded) return

        // 년도 (1900-2025)
        val years = (1900..2025).map { it.toString() }
        val context = context ?: return
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

        binding.maleButton.setOnClickListener {
            selectedGender = "M"
            updateGenderButtons()
        }

        binding.femaleButton.setOnClickListener {
            selectedGender = "F"
            updateGenderButtons()
        }

        binding.submitButton.setOnClickListener { submitProfile() }
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

    private fun submitProfile() {
        val binding = _binding ?: return

        val nickname = binding.nicknameEditText.text.toString().trim()

        val year = binding.birthYearSpinner.selectedItem.toString()
        val month = binding.birthMonthSpinner.selectedItem.toString().replace("월", "").padStart(2, '0')
        val day = binding.birthDaySpinner.selectedItem.toString().replace("일", "").padStart(2, '0')
        val birthDate = "$year-$month-$day"

        val solarOrLunar = when (binding.solarLunarRadioGroup.checkedRadioButtonId) {
            R.id.solar_radio_button -> "solar"
            R.id.lunar_radio_button -> "lunar"
            else -> ""
        }
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
