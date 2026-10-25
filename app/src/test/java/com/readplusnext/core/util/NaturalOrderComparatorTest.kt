package com.readplusnext.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun `numbers compare by value not lexicographically`() {
        assertTrue(NaturalOrderComparator.compare("page_2", "page_10") < 0)
        assertTrue(NaturalOrderComparator.compare("page_10", "page_2") > 0)
        assertTrue(NaturalOrderComparator.compare("chapter 3", "chapter 11") < 0)
    }

    @Test
    fun `sorts page names naturally`() {
        val pages = listOf("page_10", "page_2", "page_1", "page_20")
        assertEquals(
            listOf("page_1", "page_2", "page_10", "page_20"),
            pages.sortedWith(NaturalOrderComparator),
        )
    }

    @Test
    fun `identical strings compare equal`() {
        assertEquals(0, NaturalOrderComparator.compare("cover.png", "cover.png"))
        assertEquals(0, NaturalOrderComparator.compare("1.jpg", "01.jpg"))
    }

    @Test
    fun `letters compare case insensitively`() {
        assertEquals(0, NaturalOrderComparator.compare("Page_2", "page_2"))
    }
}
