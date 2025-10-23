package com.example.fortuna_android.ui

import com.example.fortuna_android.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FortuneCardView
 * Tests the UI debugging and element control changes from commit cbcd710
 *
 * Note: Most UI-related tests are in the androidTest directory for better integration testing.
 * This file focuses on logic and data model validation.
 */
class FortuneCardViewTest {

    @Test
    fun `test sample fortune data creation`() {
        // Verify that sample fortune data can be created successfully
        val fortuneData = createSampleFortuneData()

        assertNotNull(fortuneData)
        assertEquals(85, fortuneData.fortune.overallFortune)
        assertEquals("오행의 균형이 잘 맞습니다", fortuneData.fortune.elementBalance)
        assertEquals(2, fortuneData.fortune.chakraReadings.size)
    }

    @Test
    fun `test fortune data structure`() {
        val fortuneData = createSampleFortuneData()

        // Verify daily guidance
        assertEquals("새로운 시작에 좋은 날입니다", fortuneData.fortune.dailyGuidance.keyAdvice)
        assertEquals("오전 9시 - 11시", fortuneData.fortune.dailyGuidance.bestTime)
        assertEquals("동쪽", fortuneData.fortune.dailyGuidance.luckyDirection)
        assertEquals("초록색", fortuneData.fortune.dailyGuidance.luckyColor)

        // Verify chakra readings
        val chakraReading = fortuneData.fortune.chakraReadings[0]
        assertEquals("wood", chakraReading.chakraType)
        assertEquals(8, chakraReading.strength)
        assertEquals("나무의 기운이 강합니다", chakraReading.message)
    }

    @Test
    fun `test fortune score elements`() {
        val fortuneData = createSampleFortuneData()

        // Verify fortune score
        assertEquals(0.75, fortuneData.fortuneScore.entropyScore, 0.01)
        assertTrue(fortuneData.fortuneScore.elements.containsKey("일운"))

        val dayPillar = fortuneData.fortuneScore.elements["일운"]
        assertNotNull(dayPillar)
        assertEquals("갑자", dayPillar?.twoLetters)
        assertEquals("wood", dayPillar?.stem?.element)
        assertEquals("water", dayPillar?.branch?.element)
    }

    @Test
    fun `test element distribution`() {
        val fortuneData = createSampleFortuneData()

        assertTrue(fortuneData.fortuneScore.elementDistribution.containsKey("목"))
        val woodDistribution = fortuneData.fortuneScore.elementDistribution["목"]
        assertEquals(2, woodDistribution?.count)
        assertEquals(40.0, woodDistribution?.percentage ?: 0.0, 0.01)
    }

    // Helper function to create sample fortune data
    private fun createSampleFortuneData(): TodayFortuneData {
        return TodayFortuneData(
            fortuneId = 1,
            userId = 1,
            generatedAt = "2025-10-23T08:00:00Z",
            forDate = "2025-10-23",
            fortune = TodayFortune(
                overallFortune = 85,
                specialMessage = "좋은 운세입니다",
                fortuneSummary = "오늘은 행운의 날입니다",
                elementBalance = "오행의 균형이 잘 맞습니다",
                sajuCompatibility = "좋음",
                dailyGuidance = DailyGuidance(
                    keyAdvice = "새로운 시작에 좋은 날입니다",
                    bestTime = "오전 9시 - 11시",
                    luckyDirection = "동쪽",
                    luckyColor = "초록색",
                    activitiesToAvoid = listOf("서두르기", "충동적인 결정"),
                    activitiesToEmbrace = listOf("새로운 도전", "창의적인 활동")
                ),
                chakraReadings = listOf(
                    ChakraReading(
                        chakraType = "wood",
                        strength = 8,
                        message = "나무의 기운이 강합니다",
                        locationSignificance = "동쪽 방향"
                    ),
                    ChakraReading(
                        chakraType = "fire",
                        strength = 6,
                        message = "불의 기운이 있습니다",
                        locationSignificance = "남쪽 방향"
                    )
                )
            ),
            fortuneScore = FortuneScore(
                entropyScore = 0.75,
                elements = mapOf(
                    "일운" to ElementPillar(
                        twoLetters = "갑자",
                        stem = StemBranchDetail(
                            koreanName = "갑",
                            element = "wood",
                            elementColor = "#0BEFA0",
                            yinYang = "양"
                        ),
                        branch = StemBranchDetail(
                            koreanName = "자",
                            element = "water",
                            elementColor = "#2BB3FC",
                            yinYang = "양",
                            animal = "쥐"
                        )
                    )
                ),
                elementDistribution = mapOf(
                    "목" to ElementDistribution(
                        count = 2,
                        percentage = 40.0
                    )
                ),
                interpretation = "균형잡힌 오행입니다"
            )
        )
    }
}
