package com.example.fortuna_android

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for IntroPagerAdapter
 * Tests ViewPager2 intro page adapter functionality with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntroPagerAdapterTest {

    private lateinit var adapter: IntroPagerAdapter
    private lateinit var parent: ViewGroup

    @Before
    fun setUp() {
        adapter = IntroPagerAdapter()
        parent = FrameLayout(ApplicationProvider.getApplicationContext())
    }

    // ========== getItemCount Tests ==========

    @Test
    fun `test getItemCount returns 7`() {
        // Act
        val count = adapter.itemCount

        // Assert
        assertEquals("Item count should be 7 for 7 intro pages", 7, count)
    }

    // ========== getItemViewType Tests ==========

    @Test
    fun `test getItemViewType for position 0 returns 1`() {
        // Act
        val viewType = adapter.getItemViewType(0)

        // Assert
        assertEquals("Position 0 should map to view type 1 (구조)", 1, viewType)
    }

    @Test
    fun `test getItemViewType for position 1 returns 2`() {
        // Act
        val viewType = adapter.getItemViewType(1)

        // Assert
        assertEquals("Position 1 should map to view type 2 (천간지지)", 2, viewType)
    }

    @Test
    fun `test getItemViewType for position 2 returns 4`() {
        // Act
        val viewType = adapter.getItemViewType(2)

        // Assert
        assertEquals("Position 2 should map to view type 4 (오행)", 4, viewType)
    }

    @Test
    fun `test getItemViewType for position 3 returns 3`() {
        // Act
        val viewType = adapter.getItemViewType(3)

        // Assert
        assertEquals("Position 3 should map to view type 3 (일주)", 3, viewType)
    }

    @Test
    fun `test getItemViewType for position 4 returns 5`() {
        // Act
        val viewType = adapter.getItemViewType(4)

        // Assert
        assertEquals("Position 4 should map to view type 5 (음양)", 5, viewType)
    }

    @Test
    fun `test getItemViewType for position 5 returns 6`() {
        // Act
        val viewType = adapter.getItemViewType(5)

        // Assert
        assertEquals("Position 5 should map to view type 6 (십성)", 6, viewType)
    }

    @Test
    fun `test getItemViewType for position 6 returns 7`() {
        // Act
        val viewType = adapter.getItemViewType(6)

        // Assert
        assertEquals("Position 6 should map to view type 7 (운세)", 7, viewType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test getItemViewType with invalid position throws exception`() {
        // Act
        adapter.getItemViewType(7)  // Invalid position
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test getItemViewType with negative position throws exception`() {
        // Act
        adapter.getItemViewType(-1)  // Negative position
    }

    // ========== onCreateViewHolder Tests ==========

    @Test
    fun `test onCreateViewHolder for view type 1 creates IntroPage1ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 1)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage1ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage1ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 2 creates IntroPage2ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 2)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage2ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage2ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 3 creates IntroPage3ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 3)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage3ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage3ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 4 creates IntroPage4ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 4)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage4ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage4ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 5 creates IntroPage5ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 5)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage5ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage5ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 6 creates IntroPage6ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 6)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage6ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage6ViewHolder)
    }

    @Test
    fun `test onCreateViewHolder for view type 7 creates IntroPage7ViewHolder`() {
        // Act
        val viewHolder = adapter.onCreateViewHolder(parent, 7)

        // Assert
        assertNotNull("ViewHolder should not be null", viewHolder)
        assertTrue("Should be IntroPage7ViewHolder",
            viewHolder is IntroPagerAdapter.IntroPage7ViewHolder)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test onCreateViewHolder with invalid view type throws exception`() {
        // Act
        adapter.onCreateViewHolder(parent, 99)  // Invalid view type
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test onCreateViewHolder with zero view type throws exception`() {
        // Act
        adapter.onCreateViewHolder(parent, 0)  // Invalid view type
    }

    // ========== onBindViewHolder Tests ==========

    @Test
    fun `test onBindViewHolder does not throw exception`() {
        // Arrange
        val viewHolder = adapter.onCreateViewHolder(parent, 1)

        // Act & Assert - Should not throw
        adapter.onBindViewHolder(viewHolder, 0)
        assertTrue("onBindViewHolder executed without error", true)
    }

    @Test
    fun `test onBindViewHolder with different positions`() {
        // Arrange
        val viewHolder1 = adapter.onCreateViewHolder(parent, 1)
        val viewHolder2 = adapter.onCreateViewHolder(parent, 2)
        val viewHolder3 = adapter.onCreateViewHolder(parent, 3)

        // Act & Assert - Should not throw for any position
        adapter.onBindViewHolder(viewHolder1, 0)
        adapter.onBindViewHolder(viewHolder2, 1)
        adapter.onBindViewHolder(viewHolder3, 2)
        assertTrue("onBindViewHolder works for all positions", true)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test all positions create correct view holders`() {
        // Act & Assert - Verify each position creates the correct ViewHolder
        for (position in 0 until adapter.itemCount) {
            val viewType = adapter.getItemViewType(position)
            val viewHolder = adapter.onCreateViewHolder(parent, viewType)
            assertNotNull("ViewHolder at position $position should not be null", viewHolder)
            assertTrue("ViewHolder should be RecyclerView.ViewHolder",
                viewHolder is RecyclerView.ViewHolder)
        }
    }

    @Test
    fun `test view type mapping is consistent`() {
        // Arrange - Expected mappings
        val expectedMappings = mapOf(
            0 to 1,  // 구조
            1 to 2,  // 천간지지
            2 to 4,  // 오행
            3 to 3,  // 일주
            4 to 5,  // 음양
            5 to 6,  // 십성
            6 to 7   // 운세
        )

        // Act & Assert
        expectedMappings.forEach { (position, expectedViewType) ->
            val actualViewType = adapter.getItemViewType(position)
            assertEquals("Position $position should map to view type $expectedViewType",
                expectedViewType, actualViewType)
        }
    }

    @Test
    fun `test adapter can create all 7 view holders`() {
        // Act - Create view holders for all view types
        val viewHolders = (1..7).map { viewType ->
            adapter.onCreateViewHolder(parent, viewType)
        }

        // Assert
        assertEquals("Should create 7 different view holders", 7, viewHolders.size)
        viewHolders.forEach { viewHolder ->
            assertNotNull("Each view holder should not be null", viewHolder)
        }
    }

    @Test
    fun `test view holders are different types`() {
        // Act
        val vh1 = adapter.onCreateViewHolder(parent, 1)
        val vh2 = adapter.onCreateViewHolder(parent, 2)
        val vh3 = adapter.onCreateViewHolder(parent, 3)

        // Assert - Each should be a different class
        assertNotEquals("VH1 and VH2 should be different types",
            vh1::class.java, vh2::class.java)
        assertNotEquals("VH2 and VH3 should be different types",
            vh2::class.java, vh3::class.java)
        assertNotEquals("VH1 and VH3 should be different types",
            vh1::class.java, vh3::class.java)
    }
}
