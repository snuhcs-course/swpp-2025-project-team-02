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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 업로드된 이미지 개수
    private var uploadedPhotoCount = 0

    companion object {
        private const val TAG = "HomeFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
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

        // Load today's images from API
        loadTodayImages()
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

        // [개발용] Fortune View로 이동하는 임시 버튼
        binding.btnGoToFortune.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_fortune)
        }

        // 모든 카드에 클릭 리스너 (카메라 또는 이미지 확대)
        binding.cardImage1.setOnClickListener {
            handleCardClick(1)
        }
        binding.cardImage2.setOnClickListener {
            handleCardClick(2)
        }
        binding.cardImage3.setOnClickListener {
            handleCardClick(3)
        }
        binding.cardImage4.setOnClickListener {
            handleCardClick(4)
        }
    }

    private fun handleCardClick(cardNumber: Int) {
        val binding = _binding ?: return

        // 카메라 버튼이 어느 위치에 있는지 확인
        val cameraPosition = uploadedPhotoCount + 1

        if (cardNumber == cameraPosition && cameraPosition <= 4) {
            // 카메라 버튼 클릭
            navigateToCamera()
        } else {
            // 이미지 카드 클릭
            Log.d(TAG, "Image card $cardNumber clicked")
            // TODO: 이미지 확대 보기
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

    private fun loadTodayImages() {
        val binding = _binding ?: return

        // Get current date in yyyy-MM-dd format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Get JWT token
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)

        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "Authentication required")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getImages(currentDate)

                val currentBinding = _binding ?: return@launch

                if (response.isSuccessful && response.body() != null) {
                    val imageResponse = response.body()!!

                    if (imageResponse.status == "success" && imageResponse.data.images.isNotEmpty()) {
                        // Update photo count
                        uploadedPhotoCount = imageResponse.data.count
                        updateProgress()

                        // Load images into card views
                        loadImagesIntoCards(imageResponse.data.images)

                        Log.d(TAG, "Successfully loaded $uploadedPhotoCount images")
                    } else {
                        Log.d(TAG, "No images available for today")
                        uploadedPhotoCount = 0
                        updateProgress()
                        loadImagesIntoCards(emptyList())
                    }
                } else {
                    Log.e(TAG, "Failed to load images: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading images", e)
            }
        }
    }

    private fun loadImagesIntoCards(images: List<com.example.fortuna_android.api.ImageItem>) {
        val binding = _binding ?: return

        // 카메라 버튼 위치 = 업로드된 사진 개수 + 1 (1~4)
        val cameraPosition = uploadedPhotoCount + 1

        // 카드 1
        if (images.isNotEmpty()) {
            showImage(binding.ivPhoto1, binding.tvEmpty1, binding.tvElement1, images[0].url)
        } else {
            showCamera(binding.layoutCamera, binding.ivPhoto2, binding.tvElement2)
            hideLocked(binding.tvLocked3, binding.ivPhoto3, binding.tvElement3)
            hideLocked(binding.tvLocked4, binding.ivPhoto4, binding.tvElement4)
            return
        }

        // 카드 2
        if (images.size >= 2) {
            showImage(binding.ivPhoto2, binding.layoutCamera, binding.tvElement2, images[1].url)
        } else {
            // 카메라 버튼
            showCamera(binding.layoutCamera, binding.ivPhoto2, binding.tvElement2)
            hideLocked(binding.tvLocked3, binding.ivPhoto3, binding.tvElement3)
            hideLocked(binding.tvLocked4, binding.ivPhoto4, binding.tvElement4)
            return
        }

        // 카드 3
        if (images.size >= 3) {
            showImage(binding.ivPhoto3, binding.tvLocked3, binding.tvElement3, images[2].url)
        } else {
            // 카메라 버튼
            showCameraInCard3(binding.layoutCamera, binding.ivPhoto2, binding.tvLocked3, binding.ivPhoto3, binding.tvElement3)
            hideLocked(binding.tvLocked4, binding.ivPhoto4, binding.tvElement4)
            return
        }

        // 카드 4
        if (images.size >= 4) {
            showImage(binding.ivPhoto4, binding.tvLocked4, binding.tvElement4, images[3].url)
        } else {
            // 카메라 버튼을 4번 카드에
            showCameraInCard4(binding.layoutCamera, binding.ivPhoto2, binding.tvLocked4, binding.ivPhoto4, binding.tvElement4)
        }
    }

    private fun showImage(imageView: android.widget.ImageView, hideView: View, elementView: View, imageUrl: String) {
        imageView.visibility = View.VISIBLE
        hideView.visibility = View.GONE
        elementView.visibility = View.VISIBLE
        loadImageWithGlide(imageView, imageUrl)
    }

    private fun showCamera(cameraLayout: View, imageView: View, elementView: View) {
        cameraLayout.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        elementView.visibility = View.GONE
    }

    private fun showCameraInCard3(cameraLayout: View, ivPhoto2: View, tvLocked3: View, ivPhoto3: View, tvElement3: View) {
        // 2번 카드에서 카메라 숨기고, 3번 카드를 카메라처럼 보이게
        cameraLayout.visibility = View.GONE
        ivPhoto2.visibility = View.GONE

        // 3번 카드에 카메라 이모지
        if (tvLocked3 is android.widget.TextView) {
            tvLocked3.visibility = View.VISIBLE
            tvLocked3.text = "📷"
            tvLocked3.textSize = 56f
        }
        ivPhoto3.visibility = View.GONE
        tvElement3.visibility = View.GONE
    }

    private fun showCameraInCard4(cameraLayout: View, ivPhoto2: View, tvLocked4: View, ivPhoto4: View, tvElement4: View) {
        // 2번 카드에서 카메라 숨기고, 4번 카드를 카메라처럼 보이게
        cameraLayout.visibility = View.GONE
        ivPhoto2.visibility = View.GONE

        // 4번 카드에 카메라 이모지
        if (tvLocked4 is android.widget.TextView) {
            tvLocked4.visibility = View.VISIBLE
            tvLocked4.text = "📷"
            tvLocked4.textSize = 56f
        }
        ivPhoto4.visibility = View.GONE
        tvElement4.visibility = View.GONE
    }

    private fun hideLocked(tvLocked: android.widget.TextView, ivPhoto: View, tvElement: View) {
        tvLocked.visibility = View.VISIBLE
        tvLocked.text = "?"
        tvLocked.textSize = 42f
        ivPhoto.visibility = View.GONE
        tvElement.visibility = View.GONE
    }

    private fun loadImageWithGlide(imageView: android.widget.ImageView, imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_close_clear_cancel)
            .into(imageView)
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 다시 보일 때 날짜/시간 업데이트
        updateDateTime()
        // Reload images
        loadTodayImages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
