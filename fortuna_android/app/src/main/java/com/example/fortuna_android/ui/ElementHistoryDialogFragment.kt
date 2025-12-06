package com.example.fortuna_android.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fortuna_android.R
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.common.AppColors
import com.example.fortuna_android.databinding.DialogElementHistoryBinding
import com.example.fortuna_android.util.CustomToast
import kotlinx.coroutines.launch

class ElementHistoryDialogFragment : DialogFragment() {

    private var _binding: DialogElementHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ElementHistoryAdapter
    private lateinit var elementType: String  // wood, fire, earth, metal, water

    companion object {
        private const val TAG = "ElementHistoryDialog"
        private const val ARG_ELEMENT_TYPE = "element_type"
        private const val ARG_ELEMENT_KR = "element_kr"
        private const val ARG_ELEMENT_COLOR = "element_color"

        fun newInstance(elementType: String, elementKr: String, elementColor: Int): ElementHistoryDialogFragment {
            val fragment = ElementHistoryDialogFragment()
            val args = Bundle()
            args.putString(ARG_ELEMENT_TYPE, elementType)
            args.putString(ARG_ELEMENT_KR, elementKr)
            args.putInt(ARG_ELEMENT_COLOR, elementColor)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use dialog style instead of activity theme to enable dimming
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogElementHistoryBinding.inflate(inflater, container, false)

        // Remove default dialog background
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        elementType = arguments?.getString(ARG_ELEMENT_TYPE) ?: "wood"
        val elementKr = arguments?.getString(ARG_ELEMENT_KR) ?: "목"
        val elementColor = arguments?.getInt(ARG_ELEMENT_COLOR) ?: Color.parseColor(AppColors.ELEMENT_WOOD)

        setupUI(elementKr, elementColor)
        setupRecyclerView()
        setupClickListeners()
        loadElementHistory()
    }

    private fun setupUI(elementKr: String, elementColor: Int) {
        // Set element character
        val elementCharacter = when (elementKr) {
            "목" -> "木"
            "화" -> "火"
            "토" -> "土"
            "금" -> "金"
            "수" -> "水"
            else -> "?"
        }
        binding.elementCharacter.text = elementCharacter
        binding.elementCharacter.setTextColor(elementColor)

        // Set title
        binding.elementTitle.text = "$elementKr 수집 기록"
    }

    private fun setupRecyclerView() {
        adapter = ElementHistoryAdapter()
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun loadElementHistory() {
        showLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getElementHistory(elementType)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null && data.status == "success") {
                        val historyData = data.data
                        binding.totalCount.text = "총 ${historyData.totalCount}개 수집"

                        if (historyData.history.isEmpty()) {
                            showEmptyState()
                        } else {
                            adapter.submitList(historyData.history)
                            showContent()
                        }

                        Log.d(TAG, "Element history loaded: ${historyData.history.size} records")
                    } else {
                        showError("데이터를 불러올 수 없습니다")
                    }
                } else {
                    Log.e(TAG, "Failed to load history: ${response.code()}")
                    showError("데이터를 불러오는데 실패했습니다")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading history", e)
                showError("오류가 발생했습니다: ${e.message}")
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.historyRecyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        if (isAdded) {
            CustomToast.show(requireContext(), message)
        }
        // Avoid state loss crash if fragment is already destroyed
        if (isAdded && !parentFragmentManager.isStateSaved) {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Add dim effect to background
        dialog?.window?.apply {
            // Enable dim flag
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Set dim amount (0.0 = no dim, 1.0 = full dim)
            setDimAmount(0.7f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
