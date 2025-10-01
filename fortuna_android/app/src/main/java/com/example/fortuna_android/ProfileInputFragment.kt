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
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.data.Api.RetrofitClient
import com.example.fortuna_android.data.Api.UpdateProfileRequest
import kotlinx.coroutines.launch

class ProfileInputFragment : Fragment() {

    private lateinit var nicknameEditText: EditText
    private lateinit var birthYearSpinner: Spinner
    private lateinit var birthMonthSpinner: Spinner
    private lateinit var birthDaySpinner: Spinner
    private lateinit var solarLunarRadioGroup: RadioGroup
    private lateinit var solarRadioButton: RadioButton
    private lateinit var lunarRadioButton: RadioButton
    private lateinit var birthTimeSpinner: Spinner
    private lateinit var maleButton: Button
    private lateinit var femaleButton: Button
    private lateinit var submitButton: Button

    private var selectedGender = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        nicknameEditText = view.findViewById(R.id.nickname_edit_text)
        birthYearSpinner = view.findViewById(R.id.birth_year_spinner)
        birthMonthSpinner = view.findViewById(R.id.birth_month_spinner)
        birthDaySpinner = view.findViewById(R.id.birth_day_spinner)
        solarLunarRadioGroup = view.findViewById(R.id.solar_lunar_radio_group)
        solarRadioButton = view.findViewById(R.id.solar_radio_button)
        lunarRadioButton = view.findViewById(R.id.lunar_radio_button)
        birthTimeSpinner = view.findViewById(R.id.birth_time_spinner)
        maleButton = view.findViewById(R.id.male_button)
        femaleButton = view.findViewById(R.id.female_button)
        submitButton = view.findViewById(R.id.submit_button)

        setupSpinners()
    }

    private fun setupSpinners() {
        // 년도 (1900-2025)
        val years = (1900..2025).map { it.toString() }
        birthYearSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        birthYearSpinner.setSelection(years.size - 1)

        // 월 (1-12)
        val months = (1..12).map { it.toString() + "월" }
        birthMonthSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 일 (1-31)
        val days = (1..31).map { it.toString() + "일" }
        birthDaySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupClickListeners() {
        maleButton.setOnClickListener {
            selectedGender = "M"
            updateGenderButtons()
        }

        femaleButton.setOnClickListener {
            selectedGender = "F"
            updateGenderButtons()
        }

        submitButton.setOnClickListener { submitProfile() }
    }

    private fun updateGenderButtons() {
        if (selectedGender == "M") {
            maleButton.setBackgroundResource(R.drawable.button_selected)
            maleButton.setTextColor(Color.BLACK)
            femaleButton.setBackgroundResource(R.drawable.button_default)
            femaleButton.setTextColor(Color.WHITE)
        } else if (selectedGender == "F") {
            femaleButton.setBackgroundResource(R.drawable.button_selected)
            femaleButton.setTextColor(Color.BLACK)
            maleButton.setBackgroundResource(R.drawable.button_default)
            maleButton.setTextColor(Color.WHITE)
        }
    }

    private fun submitProfile() {
        val nickname = nicknameEditText.text.toString().trim()

        val year = birthYearSpinner.selectedItem.toString()
        val month = birthMonthSpinner.selectedItem.toString().replace("월", "").padStart(2, '0')
        val day = birthDaySpinner.selectedItem.toString().replace("일", "").padStart(2, '0')
        val birthDate = "$year-$month-$day"

        val solarOrLunar = when (solarLunarRadioGroup.checkedRadioButtonId) {
            R.id.solar_radio_button -> "solar"
            R.id.lunar_radio_button -> "lunar"
            else -> ""
        }
        val birthTimeUnits = extractBirthTimeUnit(birthTimeSpinner.selectedItem.toString())
        val gender = selectedGender

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
        return selectedItem.split(" ")[0]
    }

    private fun updateProfile(
        nickname: String,
        birthDate: String,
        solarOrLunar: String,
        birthTimeUnits: String,
        gender: String
    ) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "인증 토큰이 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "프로필이 성공적으로 업데이트되었습니다!", Toast.LENGTH_SHORT).show()

                    navigateToMain()
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

    private fun navigateToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToSignIn() {
        (activity as? AuthContainerActivity)?.showSignInFragment()
    }

    companion object {
        private const val TAG = "ProfileInputFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}
