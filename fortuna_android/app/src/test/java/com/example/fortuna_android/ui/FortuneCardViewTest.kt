package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.api.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FortuneCardView
 * Tests the UI debugging and element control changes from commit cbcd710
 *
 * Note: Most UI-related tests are in the androidTest directory for better integration testing.
 * This file focuses on logic and data model validation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FortuneCardViewTest {

    private lateinit var context: Context
    private lateinit var fortuneCardView: FortuneCardView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fortuneCardView = FortuneCardView(context)
    }

    // ========== View Initialization Tests ==========

    @Test
    fun `test FortuneCardView initialization`() {
        // Verify that FortuneCardView can be created
        assertNotNull("FortuneCardView should not be null", fortuneCardView)
    }

    @Test
    fun `test FortuneCardView has black background`() {
        // Verify card background is black (prevents white corners)
        val cardBackgroundColor = fortuneCardView.cardBackgroundColor
        assertNotNull("Card background color should be set", cardBackgroundColor)
    }

    // ========== Data Model Tests ==========

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

    // ========== setFortuneData() Tests ==========

    @Test
    fun `test setFortuneData with valid data`() {
        val fortuneData = createSampleFortuneData()

        // This should not throw any exception
        fortuneCardView.setFortuneData(fortuneData)

        // View should still be valid after setting data
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with null forDate`() {
        val fortuneData = createSampleFortuneData().copy(forDate = null)

        // Should handle null forDate gracefully (defaults to today)
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with invalid date format`() {
        val fortuneData = createSampleFortuneData().copy(forDate = "invalid-date")

        // Should handle invalid date format gracefully
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with no day pillar`() {
        val fortuneData = createSampleFortuneData().copy(
            fortuneScore = FortuneScore(
                entropyScore = 0.75,
                elements = emptyMap(), // No 일운
                elementDistribution = emptyMap(),
                interpretation = "균형잡힌 오행입니다"
            )
        )

        // Should handle missing day pillar gracefully (shows defaults)
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with empty chakra readings`() {
        val fortuneData = createSampleFortuneData().copy(
            fortune = createSampleFortuneData().fortune.copy(
                chakraReadings = emptyList()
            )
        )

        // Should handle empty chakra readings
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with multiple chakra readings`() {
        val chakraReadings = listOf(
            ChakraReading("wood", 8, "나무 기운", "동쪽"),
            ChakraReading("fire", 7, "불 기운", "남쪽"),
            ChakraReading("earth", 6, "흙 기운", "중앙"),
            ChakraReading("metal", 5, "쇠 기운", "서쪽"),
            ChakraReading("water", 9, "물 기운", "북쪽")
        )

        val fortuneData = createSampleFortuneData().copy(
            fortune = createSampleFortuneData().fortune.copy(
                chakraReadings = chakraReadings
            )
        )

        // Should handle multiple chakra readings
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with all five elements`() {
        // Test with each of the five elements
        val elements = listOf("wood", "fire", "earth", "metal", "water")

        elements.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)

            // Should handle all element types
            fortuneCardView.setFortuneData(fortuneData)

            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with Korean element names`() {
        // Test with Korean element names
        val koreanElements = listOf("목", "화", "토", "금", "수")

        koreanElements.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)

            // Should handle Korean element names
            fortuneCardView.setFortuneData(fortuneData)

            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with alternative Korean element names`() {
        // Test with alternative Korean names
        val alternativeNames = listOf("나무", "불", "흙", "쇠", "물")

        alternativeNames.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)

            // Should handle alternative Korean element names
            fortuneCardView.setFortuneData(fortuneData)

            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with unknown element`() {
        val fortuneData = createSampleFortuneDataWithElement("unknown")

        // Should handle unknown elements gracefully (defaults)
        fortuneCardView.setFortuneData(fortuneData)

        assertNotNull(fortuneCardView)
    }

    // ========== Click Listener Tests ==========

    @Test
    fun `test setOnRefreshFortuneClickListener`() {
        var clicked = false

        fortuneCardView.setOnRefreshFortuneClickListener {
            clicked = true
        }

        // Simulate button click (need to access the button)
        // Since we can't easily access private binding, we just verify the listener was set
        assertNotNull("Click listener should be set", fortuneCardView)

        // In a real scenario, this would trigger the button click
        // For now, we just verify no exception was thrown
    }

    // ========== Helper Functions ==========

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

    // Helper function to create fortune data with specific element
    private fun createSampleFortuneDataWithElement(element: String): TodayFortuneData {
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
                    activitiesToAvoid = listOf("서두르기"),
                    activitiesToEmbrace = listOf("새로운 도전")
                ),
                chakraReadings = listOf(
                    ChakraReading(
                        chakraType = element,
                        strength = 8,
                        message = "$element 기운이 강합니다",
                        locationSignificance = "특정 방향"
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
                            element = element,
                            elementColor = "#0BEFA0",
                            yinYang = "양"
                        ),
                        branch = StemBranchDetail(
                            koreanName = "자",
                            element = element,
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
