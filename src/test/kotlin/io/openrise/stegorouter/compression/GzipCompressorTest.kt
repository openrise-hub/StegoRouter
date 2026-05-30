package io.openrise.stegorouter.compression

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GzipCompressorTest {
    @Test
    fun `compress and decompress empty data`() {
        val data = ByteArray(0)
        val compressed = GzipCompressor.compress(data)
        val decompressed = GzipCompressor.decompress(compressed)
        assertContentEquals(data, decompressed)
    }

    @Test
    fun `compress and decompress text data`() {
        val data = "Hello, StegoRouter!".toByteArray()
        val compressed = GzipCompressor.compress(data)
        val decompressed = GzipCompressor.decompress(compressed)
        assertContentEquals(data, decompressed)
    }

    @Test
    fun `compress and decompress binary data`() {
        val data = ByteArray(1024) { it.toByte() }
        val compressed = GzipCompressor.compress(data)
        val decompressed = GzipCompressor.decompress(compressed)
        assertContentEquals(data, decompressed)
    }

    @Test
    fun `compress and decompress large payload`() {
        val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val compressed = GzipCompressor.compress(data)
        val decompressed = GzipCompressor.decompress(compressed)
        assertContentEquals(data, decompressed)
    }

    @Test
    fun `compression reduces size for repetitive data`() {
        val data = "A".repeat(10000).toByteArray()
        val compressed = GzipCompressor.compress(data)
        assert(compressed.size < data.size)
    }

    @Test
    fun `decompress invalid data throws exception`() {
        val invalidData = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertFailsWith<Exception> {
            GzipCompressor.decompress(invalidData)
        }
    }
}
