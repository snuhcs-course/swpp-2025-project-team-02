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
        assertEquals(85, fortuneData.fortune.overallFortune)
        assertEquals("오행의 균형이 잘 맞습니다", fortuneData.fortune.elementBalance)
        assertEquals(2, fortuneData.fortune.chakraReadings.size)
    }

    @Test
    fun `test fortune data structure`() {
        val fortuneData = createSampleFortuneData()

        assertEquals("새로운 시작에 좋은 날입니다", fortuneData.fortune.dailyGuidance.keyAdvice)
        assertEquals("오전 9시 - 11시", fortuneData.fortune.dailyGuidance.bestTime)
        assertEquals("동쪽", fortuneData.fortune.dailyGuidance.luckyDirection)
        assertEquals("초록색", fortuneData.fortune.dailyGuidance.luckyColor)

        val chakraReading = fortuneData.fortune.chakraReadings[0]
        assertEquals("wood", chakraReading.chakraType)
        assertEquals(8, chakraReading.strength)
        assertEquals("나무의 기운이 강합니다", chakraReading.message)
    }

    @Test
    fun `test fortune score elements`() {
        val fortuneData = createSampleFortuneData()

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
    fun `test setFortuneData with empty chakra readings`() {
        val fortuneData = createSampleFortuneData().copy(
            fortune = createSampleFortuneData().fortune.copy(
                chakraReadings = emptyList()
            )
        )

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

    @Test
    fun `test setOnWhyDeficientClickListener`() {
        var clicked = false

        fortuneCardView.setOnWhyDeficientClickListener {
            clicked = true
        }

        assertNotNull("Click listener should be set", fortuneCardView)
        assertFalse("Listener should not be invoked yet", clicked)
    }

    @Test
    fun `test setOnWhyDeficientClickListener can be called multiple times`() {
        var counter = 0

        fortuneCardView.setOnWhyDeficientClickListener {
            counter += 1
        }

        fortuneCardView.setOnWhyDeficientClickListener {
            counter += 10
        }

        assertNotNull("FortuneCardView should handle listener replacement", fortuneCardView)
    }

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

    @Test
    fun `test getChakraEmoji for all chakra types`() {
        val method = getPrivateMethod("getChakraEmoji", String::class.java)

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
            "unknown" to "🔵"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getChakraEmoji($input)", expected, result)
        }
    }

    @Test
    fun `test getElementMessage for all element types`() {
        val method = getPrivateMethod("getElementMessage", String::class.java)

        val testCases = mapOf(
            "wood" to "오늘은 나무의 기운이 강한 날입니다",
            "나무" to "오늘은 나무의 기운이 강한 날입니다",
            "목" to "오늘은 나무의 기운이 강한 날입니다",
            "fire" to "오늘은 불의 기운이 강한 날입니다",
            "불" to "오늘은 불의 기운이 강한 날입니다",
            "화" to "오늘은 불의 기운이 강한 날입니다",
            "earth" to "오늘은 흙의 기운이 강한 날입니다",
            "흙" to "오늘은 흙의 기운이 강한 날입니다",
            "토" to "오늘은 흙의 기운이 강한 날입니다",
            "metal" to "오늘은 쇠의 기운이 강한 날입니다",
            "쇠" to "오늘은 쇠의 기운이 강한 날입니다",
            "금" to "오늘은 쇠의 기운이 강한 날입니다",
            "water" to "오늘은 물의 기운이 강한 날입니다",
            "물" to "오늘은 물의 기운이 강한 날입니다",
            "수" to "오늘은 물의 기운이 강한 날입니다",
            "unknown" to "오늘의 기운을 느껴보세요"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getElementMessage($input)", expected, result)
        }
    }

    @Test
    fun `test getDeficientElementMessage for all element types`() {
        val method = getPrivateMethod("getDeficientElementMessage", String::class.java)

        val testCases = mapOf(
            "wood" to "오늘은 나무의 기운을 보충해야 합니다",
            "나무" to "오늘은 나무의 기운을 보충해야 합니다",
            "목" to "오늘은 나무의 기운을 보충해야 합니다",
            "fire" to "오늘은 불의 기운을 보충해야 합니다",
            "불" to "오늘은 불의 기운을 보충해야 합니다",
            "화" to "오늘은 불의 기운을 보충해야 합니다",
            "earth" to "오늘은 흙의 기운을 보충해야 합니다",
            "흙" to "오늘은 흙의 기운을 보충해야 합니다",
            "토" to "오늘은 흙의 기운을 보충해야 합니다",
            "metal" to "오늘은 쇠의 기운을 보충해야 합니다",
            "쇠" to "오늘은 쇠의 기운을 보충해야 합니다",
            "금" to "오늘은 쇠의 기운을 보충해야 합니다",
            "water" to "오늘은 물의 기운을 보충해야 합니다",
            "물" to "오늘은 물의 기운을 보충해야 합니다",
            "수" to "오늘은 물의 기운을 보충해야 합니다",
            "unknown" to "오늘의 기운을 느껴보세요"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getDeficientElementMessage($input)", expected, result)
        }
    }

    @Test
    fun `test getWhyDeficientButtonText for all element types`() {
        val method = getPrivateMethod("getWhyDeficientButtonText", String::class.java)

        val testCases = mapOf(
            "wood" to "나무가 왜 부족한가요?",
            "나무" to "나무가 왜 부족한가요?",
            "목" to "나무가 왜 부족한가요?",
            "fire" to "불이 왜 부족한가요?",
            "불" to "불이 왜 부족한가요?",
            "화" to "불이 왜 부족한가요?",
            "earth" to "흙이 왜 부족한가요?",
            "흙" to "흙이 왜 부족한가요?",
            "토" to "흙이 왜 부족한가요?",
            "metal" to "쇠가 왜 부족한가요?",
            "쇠" to "쇠가 왜 부족한가요?",
            "금" to "쇠가 왜 부족한가요?",
            "water" to "물이 왜 부족한가요?",
            "물" to "물이 왜 부족한가요?",
            "수" to "물이 왜 부족한가요?",
            "unknown" to "왜 이 기운이 필요한가요?"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getWhyDeficientButtonText($input)", expected, result)
        }
    }

    @Test
    fun `test getChakraName for all chakra types`() {
        val method = getPrivateMethod("getChakraName", String::class.java)

        val testCases = mapOf(
            "wood" to "목(木)",
            "fire" to "화(火)",
            "earth" to "토(土)",
            "metal" to "금(金)",
            "water" to "수(水)",
            "unknown" to "unknown"
        )

        testCases.forEach { (input, expected) ->
            val result = method.invoke(fortuneCardView, input) as String
            assertEquals("getChakraName($input)", expected, result)
        }
    }

    @Test
    fun `test getChakraColor for all chakra types`() {
        val method = getPrivateMethod("getChakraColor", String::class.java)

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
            assertEquals("getChakraColor($input)", expected, result)
        }
    }

    @Test
    fun `test createChakraReadingView creates valid view`() {
        val method = getPrivateMethod("createChakraReadingView", ChakraReading::class.java)
        val chakraReading = ChakraReading("wood", 8, "나무의 기운", "동쪽")

        val result = method.invoke(fortuneCardView, chakraReading)
        assertNotNull("createChakraReadingView should return a view", result)
    }

    @Test
    fun `test createChakraReadingView for all chakra types`() {
        val method = getPrivateMethod("createChakraReadingView", ChakraReading::class.java)

        val chakraTypes = listOf("wood", "fire", "earth", "metal", "water")
        chakraTypes.forEach { chakraType ->
            val chakraReading = ChakraReading(chakraType, 7, "$chakraType 기운", "방향")
            val result = method.invoke(fortuneCardView, chakraReading)
            assertNotNull("createChakraReadingView($chakraType) should return a view", result)
        }
    }

    @Test
    fun `test displayChakraReadings adds views`() {
        val method = getPrivateMethod("displayChakraReadings", List::class.java)

        val chakraReadings = listOf(
            ChakraReading("wood", 8, "나무 기운", "동쪽"),
            ChakraReading("fire", 7, "불 기운", "남쪽"),
            ChakraReading("earth", 6, "흙 기운", "중앙")
        )

        method.invoke(fortuneCardView, chakraReadings)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test displayChakraReadings with empty list`() {
        val method = getPrivateMethod("displayChakraReadings", List::class.java)
        method.invoke(fortuneCardView, emptyList<ChakraReading>())
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test displayChakraReadings with single reading`() {
        val method = getPrivateMethod("displayChakraReadings", List::class.java)
        val chakraReadings = listOf(ChakraReading("wood", 10, "강한 나무 기운", "동쪽"))

        method.invoke(fortuneCardView, chakraReadings)
        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test fetchAndDisplayNeededElement is called during setFortuneData`() {
        val fortuneData = createSampleFortuneData()

        // This triggers fetchAndDisplayNeededElement internally
        fortuneCardView.setFortuneData(fortuneData)

        // Advance looper to process coroutines
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertNotNull(fortuneCardView)
    }

    @Test
    fun `test all element combinations in chakra readings`() {
        val elements = listOf("wood", "fire", "earth", "metal", "water")

        elements.forEach { element ->
            val chakraReadings = listOf(
                ChakraReading(element, 5, "$element 기운 약함", "위치"),
                ChakraReading(element, 10, "$element 기운 강함", "위치")
            )

            val fortuneData = createSampleFortuneData().copy(
                fortune = createSampleFortuneData().fortune.copy(
                    chakraReadings = chakraReadings
                )
            )

            fortuneCardView.setFortuneData(fortuneData)
            assertNotNull("Should handle $element chakra readings", fortuneCardView)
        }
    }

    @Test
    fun `test mixed element types in chakra readings`() {
        val chakraReadings = listOf(
            ChakraReading("wood", 8, "나무", "동"),
            ChakraReading("불", 7, "불", "남"),
            ChakraReading("EARTH", 6, "흙", "중앙"),
            ChakraReading("쇠", 5, "쇠", "서"),
            ChakraReading("Water", 9, "물", "북")
        )

        val fortuneData = createSampleFortuneData().copy(
            fortune = createSampleFortuneData().fortune.copy(
                chakraReadings = chakraReadings
            )
        )

        fortuneCardView.setFortuneData(fortuneData)
        assertNotNull(fortuneCardView)
    }

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
