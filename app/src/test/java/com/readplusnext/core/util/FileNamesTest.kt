package com.readplusnext.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNamesTest {

    @Test
    fun `extensionOf lowercases and drops the dot`() {
        assertEquals("pdf", "Chapter.PDF".extensionOf())
        assertEquals("cbz", "book.cbz".extensionOf())
        assertEquals("", "noextension".extensionOf())
    }

    @Test
    fun `withoutExtension removes the last suffix only`() {
        assertEquals("book", "book.cbz".withoutExtension())
        assertEquals("a.b", "a.b.c".withoutExtension())
        assertEquals("noextension", "noextension".withoutExtension())
    }

    @Test
    fun `toSafeFileName strips path separators`() {
        assertEquals("a_b_c", "a/b\\c".toSafeFileName())
        assertEquals("page", "".toSafeFileName())
    }
}
