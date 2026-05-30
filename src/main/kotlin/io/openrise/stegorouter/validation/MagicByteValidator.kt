package io.openrise.stegorouter.validation

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.routing.UnsupportedFileTypeException

object MagicByteValidator {
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val BMP_MAGIC = byteArrayOf(0x42, 0x4D)
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46)
    private val EXE_MAGIC = byteArrayOf(0x4D, 0x5A)
    private val ELF_MAGIC = byteArrayOf(0x7F, 0x45, 0x4C, 0x46)
    private val RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WAVE_MAGIC = byteArrayOf(0x57, 0x41, 0x56, 0x45)

    fun validate(data: ByteArray): FileType {
        if (data.size < 4) {
            throw UnsupportedFileTypeException("Data too short to identify file type")
        }

        if (matches(data, PNG_MAGIC)) return FileType.PNG
        if (matches(data, BMP_MAGIC)) return FileType.BMP
        if (matches(data, ZIP_MAGIC)) return FileType.ZIP
        if (matches(data, PDF_MAGIC)) return FileType.PDF
        if (matches(data, EXE_MAGIC)) return FileType.EXE
        if (matches(data, ELF_MAGIC)) return FileType.ELF

        if (matches(data, RIFF_MAGIC) && data.size >= 12 && matchesAt(data, 8, WAVE_MAGIC)) {
            return FileType.WAV
        }

        if (isSvg(data)) return FileType.SVG

        throw UnsupportedFileTypeException("Unrecognized file type")
    }

    private fun matches(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        return data.copyOfRange(0, magic.size).contentEquals(magic)
    }

    private fun matchesAt(data: ByteArray, offset: Int, magic: ByteArray): Boolean {
        if (data.size < offset + magic.size) return false
        return data.copyOfRange(offset, offset + magic.size).contentEquals(magic)
    }

    private fun isSvg(data: ByteArray): Boolean {
        val text = String(data, Charsets.UTF_8).take(1024).lowercase()
        return text.contains("<svg") || text.contains("<?xml") && text.contains("<svg")
    }
}
