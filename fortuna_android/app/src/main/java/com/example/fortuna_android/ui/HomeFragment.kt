package com.example.fortuna_android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Mock data: 업로드된 이미지 개수 (나중에 ViewModel로 관리)
    private var uploadedPhotoCount = 0

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: HomeFragment view created")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up HomeFragment")

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // 날짜 및 시간 표시
        updateDateTime()

        // Progress 업데이트
        updateProgress()
    }

    private fun updateDateTime() {
        val binding = _binding ?: return

        val calendar = Calendar.getInstance()

        // 날짜 포맷: "오늘은 2025년 09월 19일 정유일(🔥)"
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
        val dateString = dateFormat.format(calendar.time)

        // TODO: 간지 정보는 나중에 API 연동 시 추가
        binding.tvDate.text = "오늘은 $dateString"

        // 시간 포맷: "지금은 14시 30분(미시☁️)"
        val timeFormat = SimpleDateFormat("HH시 mm분", Locale.KOREAN)
        val timeString = timeFormat.format(calendar.time)

        // TODO: 시진(時辰) 정보는 나중에 API 연동 시 추가
        binding.tvTime.text = "지금은 $timeString"
    }

    private fun updateProgress() {
        val binding = _binding ?: return
        binding.tvProgress.text = "$uploadedPhotoCount / 4"
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // 카메라 카드 클릭 - 하단 네비게이션의 카메라 버튼과 동일한 동작
        binding.cardCamera.setOnClickListener {
            navigateToCamera()
        }

        // [개발용] Fortune View로 이동하는 임시 버튼
        binding.btnGoToFortune.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_fortune)
        }

        // 첫 번째 이미지 카드 클릭 (나중에 확대 보기 등 기능 추가 가능)
        binding.cardImage1.setOnClickListener {
            Log.d(TAG, "Image card 1 clicked")
            // TODO: 이미지 확대 보기 또는 다른 동작
        }
    }

    private fun navigateToCamera() {
        // MainActivity의 카메라 네비게이션 로직 재사용
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            // MainActivity의 카메라 버튼 클릭과 동일하게 권한 체크 후 이동
            findNavController().navigate(R.id.cameraFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 다시 보일 때 날짜/시간 업데이트
        updateDateTime()
        // TODO: 나중에 서버에서 오늘 업로드한 사진 개수 가져오기
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
