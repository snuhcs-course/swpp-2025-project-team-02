package com.example.fortuna_android.ui

import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.R
import com.example.fortuna_android.api.ElementPillar
import com.example.fortuna_android.api.StemBranchDetail
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TodaySajuPaljaView
 * Tests today's Saju Palja display functionality with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TodaySajuPaljaViewTest {

    private lateinit var todaySajuPaljaView: TodaySajuPaljaView

    @Before
    fun setUp() {
        todaySajuPaljaView = TodaySajuPaljaView(ApplicationProvider.getApplicationContext())
    }

    // ========== Constructor Tests ==========

    @Test
    fun `test view is created successfully`() {
        assertNotNull("TodaySajuPaljaView should be created", todaySajuPaljaView)
        assertTrue("Should be instance of LinearLayout", todaySajuPaljaView is LinearLayout)
    }

    // ========== setTitle Tests ==========

    @Test
    fun `test setTitle updates title text`() {
        // Arrange
        val testTitle = "오늘의 운세"

        // Act
        todaySajuPaljaView.setTitle(testTitle)

        // Assert
        val titleView = todaySajuPaljaView.findViewById<TextView>(R.id.today_saju_title)
        assertEquals("Title should be updated", testTitle, titleView.text.toString())
    }

    @Test
    fun `test setTitle with empty string`() {
        // Act
        todaySajuPaljaView.setTitle("")

        // Assert
        val titleView = todaySajuPaljaView.findViewById<TextView>(R.id.today_saju_title)
        assertEquals("Title should be empty", "", titleView.text.toString())
    }

    @Test
    fun `test setTitle with special characters`() {
        // Arrange
        val specialTitle = "오늘의 운세 🌟✨"

        // Act
        todaySajuPaljaView.setTitle(specialTitle)

        // Assert
        val titleView = todaySajuPaljaView.findViewById<TextView>(R.id.today_saju_title)
        assertEquals("Title should handle special chars", specialTitle, titleView.text.toString())
    }

    // ========== Helper Methods ==========

    private fun createMockElementPillar(stemName: String, stemElement: String, branchName: String, branchElement: String): ElementPillar {
        val stem = StemBranchDetail(
            koreanName = stemName,
            element = stemElement,
            elementColor = "#0BEFA0",
            yinYang = "yang"
        )
        val branch = StemBranchDetail(
            koreanName = branchName,
            element = branchElement,
            elementColor = "#F93E3E",
            yinYang = "yin",
            animal = "Rat"
        )
        return ElementPillar(
            stem = stem,
            branch = branch,
            twoLetters = stemName + branchName
        )
    }

    // ========== setTodaySajuData Tests ==========

    @Test
    fun `test setTodaySajuData with daeun only`() {
        // Arrange
        val daeun = createMockElementPillar("갑", "木", "자", "水")

        // Act
        todaySajuPaljaView.setTodaySajuData(daeun, null, null, null)

        // Assert
        val daeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem)
        val daeunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch)
        assertEquals("Daeun stem should be set", "갑", daeunStem.text.toString())
        assertEquals("Daeun branch should be set", "자", daeunBranch.text.toString())
    }

    @Test
    fun `test setTodaySajuData with all four un`() {
        // Arrange
        val daeun = createMockElementPillar("갑", "木", "자", "水")
        val saeun = createMockElementPillar("을", "木", "축", "土")
        val wolun = createMockElementPillar("병", "火", "인", "木")
        val ilun = createMockElementPillar("정", "火", "묘", "木")

        // Act
        todaySajuPaljaView.setTodaySajuData(daeun, saeun, wolun, ilun)

        // Assert - Daeun
        val daeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem)
        val daeunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch)
        assertEquals("Daeun stem", "갑", daeunStem.text.toString())
        assertEquals("Daeun branch", "자", daeunBranch.text.toString())

        // Assert - Saeun
        val saeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_stem)
        val saeunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_branch)
        assertEquals("Saeun stem", "을", saeunStem.text.toString())
        assertEquals("Saeun branch", "축", saeunBranch.text.toString())

        // Assert - Wolun
        val wolunStem = todaySajuPaljaView.findViewById<TextView>(R.id.wolun_stem)
        val wolunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.wolun_branch)
        assertEquals("Wolun stem", "병", wolunStem.text.toString())
        assertEquals("Wolun branch", "인", wolunBranch.text.toString())

        // Assert - Ilun
        val ilunStem = todaySajuPaljaView.findViewById<TextView>(R.id.ilun_stem)
        val ilunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.ilun_branch)
        assertEquals("Ilun stem", "정", ilunStem.text.toString())
        assertEquals("Ilun branch", "묘", ilunBranch.text.toString())
    }

    @Test
    fun `test setTodaySajuData with null values`() {
        // Act - Should not crash with null values
        todaySajuPaljaView.setTodaySajuData(null, null, null, null)

        // Assert
        assertNotNull("View should not be null", todaySajuPaljaView)
    }

    @Test
    fun `test setTodaySajuData with saeun only`() {
        // Arrange
        val saeun = createMockElementPillar("을", "木", "축", "土")

        // Act
        todaySajuPaljaView.setTodaySajuData(null, saeun, null, null)

        // Assert
        val saeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_stem)
        val saeunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_branch)
        assertEquals("Saeun stem should be set", "을", saeunStem.text.toString())
        assertEquals("Saeun branch should be set", "축", saeunBranch.text.toString())
    }

    @Test
    fun `test setTodaySajuData with wolun only`() {
        // Arrange
        val wolun = createMockElementPillar("병", "Fire", "인", "Wood")

        // Act
        todaySajuPaljaView.setTodaySajuData(null, null, wolun, null)

        // Assert
        val wolunStem = todaySajuPaljaView.findViewById<TextView>(R.id.wolun_stem)
        val wolunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.wolun_branch)
        assertEquals("Wolun stem should be set", "병", wolunStem.text.toString())
        assertEquals("Wolun branch should be set", "인", wolunBranch.text.toString())
    }

    @Test
    fun `test setTodaySajuData with ilun only`() {
        // Arrange
        val ilun = createMockElementPillar("정", "Fire", "묘", "Wood")

        // Act
        todaySajuPaljaView.setTodaySajuData(null, null, null, ilun)

        // Assert
        val ilunStem = todaySajuPaljaView.findViewById<TextView>(R.id.ilun_stem)
        val ilunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.ilun_branch)
        assertEquals("Ilun stem should be set", "정", ilunStem.text.toString())
        assertEquals("Ilun branch should be set", "묘", ilunBranch.text.toString())
    }

    // ========== Element Info Tests ==========

    @Test
    fun `test element info for wood in Korean`() {
        // Arrange
        val pillar = createMockElementPillar("갑", "목", "인", "목")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("목 should be Wood", "木, 나무", stemElement.text.toString())
        assertEquals("목 should be Wood", "木, 나무", branchElement.text.toString())
    }

    @Test
    fun `test element info for wood in English`() {
        // Arrange
        val pillar = createMockElementPillar("갑", "Wood", "인", "WOOD")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Wood should map correctly", "木, 나무", stemElement.text.toString())
        assertEquals("WOOD (uppercase) should map correctly", "木, 나무", branchElement.text.toString())
    }

    @Test
    fun `test element info for fire`() {
        // Arrange
        val pillar = createMockElementPillar("병", "Fire", "오", "화")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Fire should be 火", "火, 불", stemElement.text.toString())
        assertEquals("화 should be 火", "火, 불", branchElement.text.toString())
    }

    @Test
    fun `test element info for earth`() {
        // Arrange
        val pillar = createMockElementPillar("무", "Earth", "진", "토")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Earth should be 土", "土, 흙", stemElement.text.toString())
        assertEquals("토 should be 土", "土, 흙", branchElement.text.toString())
    }

    @Test
    fun `test element info for metal`() {
        // Arrange
        val pillar = createMockElementPillar("경", "Metal", "유", "금")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Metal should be 金", "金, 쇠", stemElement.text.toString())
        assertEquals("금 should be 金", "金, 쇠", branchElement.text.toString())
    }

    @Test
    fun `test element info for water`() {
        // Arrange
        val pillar = createMockElementPillar("임", "Water", "자", "수")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Water should be 水", "水, 물", stemElement.text.toString())
        assertEquals("수 should be 水", "水, 물", branchElement.text.toString())
    }

    @Test
    fun `test element info for unknown element`() {
        // Arrange
        val pillar = createMockElementPillar("갑", "Unknown", "자", "???")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert
        val stemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val branchElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch_element)
        assertEquals("Unknown should return default", "?, ?", stemElement.text.toString())
        assertEquals("??? should return default", "?, ?", branchElement.text.toString())
    }

    // ========== Multiple Calls Tests ==========

    @Test
    fun `test setTodaySajuData can be called multiple times`() {
        // Arrange
        val pillar1 = createMockElementPillar("갑", "木", "자", "水")
        val pillar2 = createMockElementPillar("을", "木", "축", "土")

        // Act
        todaySajuPaljaView.setTodaySajuData(pillar1, null, null, null)
        todaySajuPaljaView.setTodaySajuData(pillar2, null, null, null)

        // Assert - Should have second pillar values
        val daeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem)
        val daeunBranch = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_branch)
        assertEquals("Should have second pillar stem", "을", daeunStem.text.toString())
        assertEquals("Should have second pillar branch", "축", daeunBranch.text.toString())
    }

    @Test
    fun `test setTitle and setTodaySajuData work together`() {
        // Arrange
        val testTitle = "테스트 타이틀"
        val pillar = createMockElementPillar("갑", "木", "자", "水")

        // Act
        todaySajuPaljaView.setTitle(testTitle)
        todaySajuPaljaView.setTodaySajuData(pillar, null, null, null)

        // Assert - Both should be set
        val titleView = todaySajuPaljaView.findViewById<TextView>(R.id.today_saju_title)
        val daeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem)
        assertEquals("Title should be set", testTitle, titleView.text.toString())
        assertEquals("Daeun should be set", "갑", daeunStem.text.toString())
    }

    @Test
    fun `test view maintains state after multiple operations`() {
        // Arrange
        val daeun = createMockElementPillar("갑", "Wood", "자", "Water")
        val saeun = createMockElementPillar("을", "Wood", "축", "Earth")

        // Act
        todaySajuPaljaView.setTitle("Initial Title")
        todaySajuPaljaView.setTodaySajuData(daeun, saeun, null, null)
        todaySajuPaljaView.setTitle("Updated Title")

        // Assert - All data should be maintained
        val titleView = todaySajuPaljaView.findViewById<TextView>(R.id.today_saju_title)
        val daeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem)
        val saeunStem = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_stem)

        assertEquals("Title should be updated", "Updated Title", titleView.text.toString())
        assertEquals("Daeun should still be set", "갑", daeunStem.text.toString())
        assertEquals("Saeun should still be set", "을", saeunStem.text.toString())
    }

    @Test
    fun `test all element types with mixed case`() {
        // Arrange - Test case insensitivity
        val daeun = createMockElementPillar("갑", "WoOd", "자", "WATER")
        val saeun = createMockElementPillar("병", "FiRe", "오", "화")
        val wolun = createMockElementPillar("무", "eArTh", "진", "토")
        val ilun = createMockElementPillar("경", "MeTaL", "유", "금")

        // Act
        todaySajuPaljaView.setTodaySajuData(daeun, saeun, wolun, ilun)

        // Assert - All should be parsed correctly despite mixed case
        val daeunStemElement = todaySajuPaljaView.findViewById<TextView>(R.id.daeun_stem_element)
        val saeunStemElement = todaySajuPaljaView.findViewById<TextView>(R.id.saeun_stem_element)
        val wolunStemElement = todaySajuPaljaView.findViewById<TextView>(R.id.wolun_stem_element)
        val ilunStemElement = todaySajuPaljaView.findViewById<TextView>(R.id.ilun_stem_element)

        assertEquals("Mixed case Wood should work", "木, 나무", daeunStemElement.text.toString())
        assertEquals("Mixed case Fire should work", "火, 불", saeunStemElement.text.toString())
        assertEquals("Mixed case Earth should work", "土, 흙", wolunStemElement.text.toString())
        assertEquals("Mixed case Metal should work", "金, 쇠", ilunStemElement.text.toString())
    }
}
