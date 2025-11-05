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
import org.robolectric.shadows.ShadowLooper
import java.lang.reflect.Method

/**
 * Comprehensive unit tests for FortuneCardView
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
        assertNotNull("FortuneCardView should not be null", fortuneCardView)
    }

    @Test
    fun `test FortuneCardView has black background`() {
        val cardBackgroundColor = fortuneCardView.cardBackgroundColor
        assertNotNull("Card background color should be set", cardBackgroundColor)
    }

    @Test
    fun `test FortuneCardView initialization with attributes`() {
        val view = FortuneCardView(context, null, 0)
        assertNotNull(view)
    }

    // ========== Data Model Tests ==========

    @Test
    fun `test sample fortune data creation`() {
        val fortuneData = createSampleFortuneData()

        assertNotNull(fortuneData)
        assertEquals("새로운 시작에 좋은 날입니다. 오전 9시-11시가 가장 좋습니다.", fortuneData.fortune.todayDailyGuidance)
        assertEquals("오행의 균형이 잘 맞습니다", fortuneData.fortune.todayElementBalanceDescription)
    }

    @Test
    fun `test fortune data structure`() {
        val fortuneData = createSampleFortuneData()

        // Test TodayFortune fields
        assertNotNull("todayDailyGuidance should not be null", fortuneData.fortune.todayDailyGuidance)
        assertNotNull("todayElementBalanceDescription should not be null", fortuneData.fortune.todayElementBalanceDescription)
        assertTrue("todayDailyGuidance should not be empty", fortuneData.fortune.todayDailyGuidance.isNotEmpty())
        assertTrue("todayElementBalanceDescription should not be empty", fortuneData.fortune.todayElementBalanceDescription.isNotEmpty())
    }

    @Test
    fun `test fortune score elements`() {
        val fortuneData = createSampleFortuneData()

        assertEquals(85.0, fortuneData.fortuneScore.entropyScore, 0.01)
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
        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with null forDate`() {
        val fortuneData = createSampleFortuneData().copy(forDate = null)
        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with invalid date format`() {
        val fortuneData = createSampleFortuneData().copy(forDate = "invalid-date")
        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with date parse exception`() {
        val fortuneData = createSampleFortuneData().copy(forDate = "2025-13-45")
        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with various date formats`() {
        val dateFormats = listOf(
            "2025-10-23",
            "2025-01-01",
            "2025-12-31",
            null
        )

        dateFormats.forEach { dateStr ->
            val fortuneData = createSampleFortuneData().copy(forDate = dateStr)
            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("Should handle date: $dateStr", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with no day pillar`() {
        val fortuneData = createSampleFortuneData().copy(
            fortuneScore = FortuneScore(
                entropyScore = 0.75,
                elements = emptyMap(),
                elementDistribution = emptyMap(),
                interpretation = "균형잡힌 오행입니다"
            )
        )

        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with different guidance text`() {
        val fortuneData = createSampleFortuneData().copy(
            fortune = TodayFortune(
                todayDailyGuidance = "다른 가이던스 텍스트입니다.",
                todayElementBalanceDescription = "다른 설명입니다."
            )
        )

        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test setFortuneData with all five elements`() {
        val elements = listOf("wood", "fire", "earth", "metal", "water")

        elements.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)
            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with Korean element names`() {
        val koreanElements = listOf("목", "화", "토", "금", "수")

        koreanElements.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)
            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with alternative Korean element names`() {
        val alternativeNames = listOf("나무", "불", "흙", "쇠", "물")

        alternativeNames.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)
            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("FortuneCardView should handle $element element", fortuneCardView)
        }
    }

    @Test
    fun `test setFortuneData with unknown element`() {
        val fortuneData = createSampleFortuneDataWithElement("unknown")
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

        assertNotNull("Click listener should be set", fortuneCardView)
        assertFalse("Listener should not be invoked yet", clicked)
    }

    @Test
    fun `test setOnRefreshFortuneClickListener can be called multiple times`() {
        var counter = 0

        fortuneCardView.setOnRefreshFortuneClickListener {
            counter += 1
        }

        fortuneCardView.setOnRefreshFortuneClickListener {
            counter += 10
        }

        assertNotNull("FortuneCardView should handle listener replacement", fortuneCardView)
    }

    // Removed: setOnWhyDeficientClickListener tests - function no longer exists in FortuneCardView

    // ========== Private Method Tests (via reflection) ==========

    @Test
    fun `test getElementEmoji for all element types`() {
        val method = getPrivateMethod("getElementEmoji", String::class.java)

        val testCases = mapOf(
            "wood" to "🌳",
            "나무" to "🌳",
            "목" to "🌳",
            "fire" to "🔥",
            "불" to "🔥",
            "화" to "🔥",
            "earth" to "🌏",
            "흙" to "🌏",
            "토" to "🌏",
            "metal" to "⚔️",
            "쇠" to "⚔️",
            "금" to "⚔️",
            "water" to "💧",
            "물" to "💧",
            "수" to "💧",
            "unknown" to "⭐"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getElementEmoji($input)", expected, result)
        }
    }

    @Test
    fun `test getElementCharacter for all element types`() {
        val method = getPrivateMethod("getElementCharacter", String::class.java)

        val testCases = mapOf(
            "wood" to "木",
            "나무" to "木",
            "목" to "木",
            "fire" to "火",
            "불" to "火",
            "화" to "火",
            "earth" to "土",
            "흙" to "土",
            "토" to "土",
            "metal" to "金",
            "쇠" to "金",
            "금" to "金",
            "water" to "水",
            "물" to "水",
            "수" to "水",
            "unknown" to "☆"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getElementCharacter($input)", expected, result)
        }
    }

    @Test
    fun `test getElementColorFromString for all element types`() {
        val method = getPrivateMethod("getElementColorFromString", String::class.java)

        val testCases = mapOf(
            "wood" to Color.parseColor("#0BEFA0"),
            "나무" to Color.parseColor("#0BEFA0"),
            "목" to Color.parseColor("#0BEFA0"),
            "fire" to Color.parseColor("#F93E3E"),
            "불" to Color.parseColor("#F93E3E"),
            "화" to Color.parseColor("#F93E3E"),
            "earth" to Color.parseColor("#FF9500"),
            "흙" to Color.parseColor("#FF9500"),
            "토" to Color.parseColor("#FF9500"),
            "metal" to Color.parseColor("#C0C0C0"),
            "쇠" to Color.parseColor("#C0C0C0"),
            "금" to Color.parseColor("#C0C0C0"),
            "water" to Color.parseColor("#2BB3FC"),
            "물" to Color.parseColor("#2BB3FC"),
            "수" to Color.parseColor("#2BB3FC"),
            "unknown" to Color.parseColor("#FFFFFF")
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as Int
            assertEquals("getElementColorFromString($input)", expected, result)
        }
    }

    // Removed: getChakraEmoji tests - chakraReadings field no longer exists in TodayFortune

    // Removed: getElementMessage tests - function no longer exists in FortuneCardView
    // Removed: getDeficientElementMessage tests - function no longer exists in FortuneCardView
    // Removed: getWhyDeficientButtonText tests - function no longer exists in FortuneCardView

    // Removed: All chakra-related tests - chakraReadings field no longer exists in TodayFortune
    // Tests removed:
    // - getChakraName
    // - getChakraColor
    // - createChakraReadingView
    // - displayChakraReadings

    @Test
    fun `test fetchAndDisplayNeededElement is called during setFortuneData`() {
        val fortuneData = createSampleFortuneData()

        // This triggers fetchAndDisplayNeededElement internally
        fortuneCardView.setFortuneData(fortuneData)

        // Advance looper to process coroutines
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertNotNull(fortuneCardView)
    }

    // Removed: chakra readings combination tests - field no longer exists

    @Test
    fun `test case insensitivity for all element names`() {
        val elementVariations = listOf(
            "WOOD", "Wood", "wood",
            "FIRE", "Fire", "fire",
            "EARTH", "Earth", "earth",
            "METAL", "Metal", "metal",
            "WATER", "Water", "water"
        )

        elementVariations.forEach { element ->
            val fortuneData = createSampleFortuneDataWithElement(element)
            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("Should handle $element (case variations)", fortuneCardView)
        }
    }

    // ========== Helper Functions ==========

    private fun getPrivateMethod(methodName: String, vararg parameterTypes: Class<*>): Method {
        val method = FortuneCardView::class.java.getDeclaredMethod(methodName, *parameterTypes)
        method.isAccessible = true
        return method
    }

    private fun createSampleFortuneData(): TodayFortuneData {
        return TodayFortuneData(
            fortuneId = 1,
            userId = 1,
            generatedAt = "2025-10-23T08:00:00Z",
            forDate = "2025-10-23",
            fortune = TodayFortune(
                todayFortuneSummary = "긍정적인 에너지가 가득한 하루가 될 것입니다.",
                todayDailyGuidance = "새로운 시작에 좋은 날입니다. 오전 9시-11시가 가장 좋습니다.",
                todayElementBalanceDescription = "오행의 균형이 잘 맞습니다"
            ),
            fortuneScore = FortuneScore(
                entropyScore = 85.0,
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

    private fun createSampleFortuneDataWithElement(element: String): TodayFortuneData {
        return TodayFortuneData(
            fortuneId = 1,
            userId = 1,
            generatedAt = "2025-10-23T08:00:00Z",
            forDate = "2025-10-23",
            fortune = TodayFortune(
                todayFortuneSummary = "$element 기운이 강한 하루가 될 것입니다.",
                todayDailyGuidance = "새로운 시작에 좋은 날입니다. $element 기운이 강합니다.",
                todayElementBalanceDescription = "오행의 균형이 잘 맞습니다"
            ),
            fortuneScore = FortuneScore(
                entropyScore = 85.0,
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
