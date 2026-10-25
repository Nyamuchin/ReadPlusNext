package com.readplusnext.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `columns default to three`() {
        val settings = AppSettings()
        assertEquals(3, settings.comicGridColumns)
        assertEquals(3, settings.videoGridColumns)
    }

    @Test
    fun `clampColumns keeps values inside the supported range`() {
        assertEquals(2, AppSettings.clampColumns(1))
        assertEquals(2, AppSettings.clampColumns(Int.MIN_VALUE))
        assertEquals(3, AppSettings.clampColumns(3))
        assertEquals(5, AppSettings.clampColumns(9))
        assertEquals(5, AppSettings.clampColumns(Int.MAX_VALUE))
    }
}
