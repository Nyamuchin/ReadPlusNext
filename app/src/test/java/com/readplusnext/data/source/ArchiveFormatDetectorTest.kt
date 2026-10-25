package com.readplusnext.data.source

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArchiveFormatDetectorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `extension wins over content`() {
        val file = temporaryFolder.newFile("comic.cbz").apply { writeText("not a zip") }
        assertEquals(ArchiveFormat.ZIP, ArchiveFormatDetector.detect(file, "comic.cbz"))
        val pdf = temporaryFolder.newFile("book.pdf").apply { writeText("") }
        assertEquals(ArchiveFormat.PDF, ArchiveFormatDetector.detect(pdf, "book.pdf"))
    }

    @Test
    fun `sniffs pdf magic when the extension is unknown`() {
        val file = temporaryFolder.newFile("mystery.bin").apply { writeText("%PDF-1.7\n") }
        assertEquals(ArchiveFormat.PDF, ArchiveFormatDetector.detect(file, "mystery.bin"))
    }

    @Test
    fun `sniffs zip and rar magic bytes`() {
        val zip = temporaryFolder.newFile("noext").apply {
            writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00))
        }
        assertEquals(ArchiveFormat.ZIP, ArchiveFormatDetector.detect(zip, "noext"))
        val rar = temporaryFolder.newFile("other").apply {
            writeBytes(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00))
        }
        assertEquals(ArchiveFormat.RAR, ArchiveFormatDetector.detect(rar, "other"))
    }

    @Test
    fun `sniffs mobi book header at offset sixty`() {
        val bytes = ByteArray(80)
        "BOOKMOBI".toByteArray(Charsets.US_ASCII).copyInto(bytes, 60)
        val file = temporaryFolder.newFile("book.bin").apply { writeBytes(bytes) }
        assertEquals(ArchiveFormat.MOBI, ArchiveFormatDetector.detect(file, "book.bin"))
    }

    @Test
    fun `unknown content stays unknown`() {
        val file = temporaryFolder.newFile("notes.txt").apply { writeText("hello world") }
        assertEquals(ArchiveFormat.UNKNOWN, ArchiveFormatDetector.detect(file, "notes.txt"))
    }

    @Test
    fun `isSupportedName accepts every importer extension`() {
        listOf("a.zip", "a.cbz", "a.rar", "a.cbr", "a.7z", "a.cb7", "a.pdf", "a.epub", "a.mobi", "a.azw", "a.azw3")
            .forEach { assertTrue(it, ArchiveFormatDetector.isSupportedName(it)) }
        assertFalse(ArchiveFormatDetector.isSupportedName("a.txt"))
        assertTrue(ArchiveFormatDetector.isSupportedName("A.CBZ"))
    }
}
