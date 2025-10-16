package com.example.fortuna_android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.api.FortuneData
import com.example.fortuna_android.databinding.FragmentFortuneDetailBinding
import com.google.gson.Gson

class FortuneDetailFragment : Fragment() {

    private var _binding: FragmentFortuneDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFortuneDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get FortuneData from arguments
        val fortuneDataJson = arguments?.getString("fortuneData")
        if (fortuneDataJson != null) {
            val fortuneData = Gson().fromJson(fortuneDataJson, FortuneData::class.java)
            binding.fortuneCardView.setFortuneData(fortuneData)
        }

        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
