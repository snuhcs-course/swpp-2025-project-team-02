package com.example.fortuna_android.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.FragmentAtomBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AtomFragment : Fragment() {
    private var _binding: FragmentAtomBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private var currentCalendar: Calendar = Calendar.getInstance()

    companion object {
        private const val TAG = "AtomFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadMonthData()
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter(emptyList())
        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
    }

    private fun setupButtons() {
        binding.btnPreviousMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            loadMonthData()
        }

        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            loadMonthData()
        }
    }

    private fun loadMonthData() {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthString = dateFormat.format(currentCalendar.time)

        // 월 표시 업데이트
        val displayFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)
        binding.tvCurrentMonth.text = displayFormat.format(currentCalendar.time)

        // API 호출
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMonthlyHistory(monthString)
                if (response.isSuccessful && response.body() != null) {
                    val historyData = response.body()!!.data
                    displayCalendar(historyData)
                    displaySummary(historyData.summary)
                } else {
                    Log.e(TAG, "Failed to load monthly history: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading monthly history", e)
            }
        }
    }

    private fun displayCalendar(historyData: com.example.fortuna_android.api.MonthlyHistoryData) {
        val calendar = currentCalendar.clone() as Calendar

        // 해당 월의 1일로 설정
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // 해당 월의 첫 날의 요일 (일요일 = 1, 토요일 = 7)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 해당 월의 마지막 날짜
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<CalendarDay>()

        // 이전 달의 빈 칸 추가
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, null, false))
        }

        // 현재 달의 날짜 추가
        for (day in 1..lastDayOfMonth) {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                    set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, day)
                }.time
            )

            // 해당 날짜의 데이터 찾기
            val dayData = historyData.days.find { it.date == dateString }

            days.add(CalendarDay(day, dayData, true))
        }

        // 남은 칸을 채우기 (6주 = 42칸)
        while (days.size < 42) {
            days.add(CalendarDay(0, null, false))
        }

        calendarAdapter.updateDays(days)
    }

    private fun displaySummary(summary: com.example.fortuna_android.api.SummaryData) {
        binding.tvCompletedDays.text = "${summary.completedDays} / ${summary.totalDays}"
        binding.tvCompletionRate.text = "${summary.completionRate}%"
        binding.tvTotalCollected.text = summary.totalCollected.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}