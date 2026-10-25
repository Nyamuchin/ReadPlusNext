package com.readplusnext.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HashingTest {

    @Test
    fun `string md5 matches known digest`() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", "abc".md5Hex())
    }

    @Test
    fun `byte array md5 matches string md5`() {
        val bytes = "ReadPlusNext".toByteArray()
        assertEquals("ReadPlusNext".md5Hex(), bytes.md5Hex())
    }
}
