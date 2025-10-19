package com.example.fortuna_android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.databinding.FragmentArOverlayBinding
import com.example.fortuna_android.util.CustomToast
import com.example.fortuna_android.util.ModelLoader
import io.github.sceneview.ar.node.ArModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AR 오버레이 전용 Fragment
 * ARCore를 사용하여 3D 모델을 실시간으로 오버레이
 * CameraFragment와 독립적으로 작동
 */
class ArOverlayFragment : Fragment() {

    companion object {
        private const val TAG = "ArOverlayFragment"
    }

    private var _binding: FragmentArOverlayBinding? = null
    private val binding get() = _binding!!

    private var modelLoader: ModelLoader? = null
    private var currentModelNode: ArModelNode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()

        // Hide bottom navigation
        if (activity is MainActivity) {
            (activity as? MainActivity)?.hideBottomNavigation()
        }

        // AR 초기화
        initializeAr()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * AR 초기화 및 3D 모델 로딩
     */
    private fun initializeAr() {
        val currentContext = context ?: return

        // ARCore 사용 가능 여부 확인
        try {
            val availability = com.google.ar.core.ArCoreApk.getInstance()
                .checkAvailability(currentContext)

            if (availability != com.google.ar.core.ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                Log.w(TAG, "ARCore not available: $availability")
                showError("ARCore를 사용할 수 없습니다.\nGoogle Play Services for AR를 설치해주세요.")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ARCore availability", e)
            showError("ARCore 확인 실패")
            return
        }

        // 로딩 표시
        showLoading(true, "AR 초기화 중...")

        // 비동기로 AR 초기화
        lifecycleScope.launch {
            try {
                // ModelLoader 초기화 (백그라운드)
                val loader = withContext(Dispatchers.IO) {
                    ModelLoader(currentContext)
                }

                // GLB 파일 존재 여부 확인
                if (!loader.checkAllModelsExist()) {
                    withContext(Dispatchers.Main) {
                        showError("GLB 파일을 찾을 수 없습니다")
                    }
                    return@launch
                }

                modelLoader = loader

                // ArSceneView 초기화 대기
                delay(300)

                // 메인 스레드에서 모델 로드
                withContext(Dispatchers.Main) {
                    loadModel(ModelLoader.Element.FIRE)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during AR initialization", e)
                withContext(Dispatchers.Main) {
                    showError("AR 초기화 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * 3D 모델 로드
     * @param element 로드할 오행 요소
     */
    private fun loadModel(element: ModelLoader.Element) {
        showLoading(true, "${element.displayName} 모델 로딩 중...")

        try {
            // 기존 모델 제거
            currentModelNode?.let { node ->
                binding.arSceneView.removeChild(node)
                currentModelNode = null
            }

            // 새 모델 생성
            currentModelNode = modelLoader?.createModelNode(
                engine = binding.arSceneView.engine,
                element = element
            )

            currentModelNode?.let { node ->
                binding.arSceneView.addChild(node)

                Log.d(TAG, "${element.displayName} model added to AR scene")
                Log.d(TAG, "Model position: ${node.position}")
                Log.d(TAG, "Model scale: ${node.scale}")
                Log.d(TAG, "AR children count: ${binding.arSceneView.children.size}")

                showLoading(false)
                updateModelInfo(element)
                CustomToast.show(requireContext(), "${element.displayName} 모델 로드 완료!")

            } ?: run {
                Log.e(TAG, "Failed to create model node")
                showError("모델 노드 생성 실패")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            showError("모델 로드 실패: ${e.message}")
        }
    }

    /**
     * Object Detection 라벨에 따라 모델 변경
     * (나중에 Object Detection 연동 시 사용)
     */
    fun updateModelForLabel(label: String) {
        val element = modelLoader?.getElementForLabel(label)
        if (element != null) {
            loadModel(element)
            Log.d(TAG, "Model updated for label: $label -> ${element.displayName}")
        } else {
            Log.d(TAG, "No matching element for label: $label")
        }
    }

    /**
     * 직접 Element를 지정하여 모델 변경
     */
    fun changeModel(element: ModelLoader.Element) {
        loadModel(element)
    }

    /**
     * 로딩 상태 표시
     */
    private fun showLoading(show: Boolean, message: String = "") {
        binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        binding.statusText.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.statusText.text = message
        }
    }

    /**
     * 에러 메시지 표시
     */
    private fun showError(message: String) {
        showLoading(false)
        CustomToast.show(requireContext(), "⚠️ $message")
        Log.e(TAG, message)
    }

    /**
     * 현재 모델 정보 표시 (디버그용)
     */
    private fun updateModelInfo(element: ModelLoader.Element) {
        binding.modelInfo.visibility = View.VISIBLE
        binding.modelInfo.text = "Model: ${element.displayName} (${element.fileName})"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show bottom navigation
        if (activity is MainActivity) {
            (activity as MainActivity).showBottomNavigation()
        }

        // AR 리소스 정리
        currentModelNode?.let { node ->
            binding.arSceneView.removeChild(node)
        }
        currentModelNode = null
        modelLoader = null

        _binding = null
    }
}
