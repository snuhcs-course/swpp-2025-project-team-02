package com.example.fortuna_android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fortuna_android.databinding.FragmentDetailAnalysisBinding

class DetailAnalysisFragment : Fragment() {
    private var _binding: FragmentDetailAnalysisBinding? = null
    private val binding get() = _binding!!

    companion object {
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
        // TODO: 상세 분석 내용 추가
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
