package io.openrise.stegorouter.validation

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.routing.UnsupportedFileTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MagicByteValidatorTest {
    @Test
    fun `validate PNG magic bytes`() {
        val data = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertEquals(FileType.PNG, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate BMP magic bytes`() {
        val data = byteArrayOf(0x42, 0x4D, 0x00, 0x00, 0x00, 0x00)
        assertEquals(FileType.BMP, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate WAV magic bytes`() {
        val data = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45)
        assertEquals(FileType.WAV, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate ZIP magic bytes`() {
        val data = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)
        assertEquals(FileType.ZIP, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate PDF magic bytes`() {
        val data = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34)
        assertEquals(FileType.PDF, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate EXE magic bytes`() {
        val data = byteArrayOf(0x4D, 0x5A, 0x90, 0x00, 0x03, 0x00)
        assertEquals(FileType.EXE, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate ELF magic bytes`() {
        val data = byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01)
        assertEquals(FileType.ELF, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate SVG content`() {
        val data = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()
        assertEquals(FileType.SVG, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate SVG without XML declaration`() {
        val data = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()
        assertEquals(FileType.SVG, MagicByteValidator.validate(data))
    }

    @Test
    fun `validate unknown file type throws exception`() {
        val data = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        assertFailsWith<UnsupportedFileTypeException> {
            MagicByteValidator.validate(data)
        }
    }

    @Test
    fun `validate data too short throws exception`() {
        val data = byteArrayOf(0x00, 0x01)
        assertFailsWith<UnsupportedFileTypeException> {
            MagicByteValidator.validate(data)
        }
    }
}
