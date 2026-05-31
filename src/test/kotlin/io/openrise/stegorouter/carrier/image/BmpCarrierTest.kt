package io.openrise.stegorouter.carrier.image

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.carrier.PayloadCapacityException
import io.openrise.stegorouter.config.ImageStegoConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BmpCarrierTest {

    @Test
    fun `embed and extract with default config`() {
        val carrier = createMinimalBmp(10, 10, 24)
        val payload = "Hello, BMP!".toByteArray()
        val bmpCarrier = BmpCarrier()

        val embedded = bmpCarrier.embed(carrier, payload)
        val extracted = bmpCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with LSB replacement mode`() {
        val carrier = createMinimalBmp(10, 10, 24)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(lsbMode = LsbMode.REPLACEMENT, bitsPerChannel = 1)
        val bmpCarrier = BmpCarrier(config)

        val embedded = bmpCarrier.embed(carrier, payload)
        val extracted = bmpCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with LSB matching mode`() {
        val carrier = createMinimalBmp(10, 10, 24)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(lsbMode = LsbMode.MATCHING, bitsPerChannel = 1)
        val bmpCarrier = BmpCarrier(config)

        val embedded = bmpCarrier.embed(carrier, payload)
        val extracted = bmpCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with 2 bits per channel`() {
        val carrier = createMinimalBmp(10, 10, 24)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(bitsPerChannel = 2)
        val bmpCarrier = BmpCarrier(config)

        val embedded = bmpCarrier.embed(carrier, payload)
        val extracted = bmpCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with 32-bit BMP`() {
        val carrier = createMinimalBmp(10, 10, 32)
        val payload = "Test".toByteArray()
        val bmpCarrier = BmpCarrier()

        val embedded = bmpCarrier.embed(carrier, payload)
        val extracted = bmpCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `calculate capacity returns correct value for 24-bit`() {
        val carrier = createMinimalBmp(100, 100, 24)
        val bmpCarrier = BmpCarrier()

        val capacity = bmpCarrier.calculateCapacity(carrier)

        assertThat(capacity).isEqualTo(100L * 100 * 3 / 8)
    }

    @Test
    fun `calculate capacity returns correct value for 32-bit`() {
        val carrier = createMinimalBmp(100, 100, 32)
        val bmpCarrier = BmpCarrier()

        val capacity = bmpCarrier.calculateCapacity(carrier)

        assertThat(capacity).isEqualTo(100L * 100 * 4 / 8)
    }

    @Test
    fun `embed throws exception when payload too large`() {
        val carrier = createMinimalBmp(2, 2, 24)
        val payload = ByteArray(100)
        val bmpCarrier = BmpCarrier()

        assertThatThrownBy { bmpCarrier.embed(carrier, payload) }
            .isInstanceOf(PayloadCapacityException::class.java)
    }

    @Test
    fun `embed throws exception for invalid BMP`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val bmpCarrier = BmpCarrier()

        assertThatThrownBy { bmpCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns BMP`() {
        val bmpCarrier = BmpCarrier()

        assertThat(bmpCarrier.supportedType()).isEqualTo(FileType.BMP)
    }

    @Test
    fun `byte level verification - embedded data differs from original`() {
        val carrier = createMinimalBmp(10, 10, 24)
        val payload = "Test".toByteArray()
        val bmpCarrier = BmpCarrier()

        val embedded = bmpCarrier.embed(carrier, payload)

        assertThat(embedded).isNotEqualTo(carrier)
    }

    private fun createMinimalBmp(width: Int, height: Int, bitsPerPixel: Int): ByteArray {
        val channels = bitsPerPixel / 8
        val rowSize = ((width * channels + 3) / 4) * 4
        val pixelDataSize = rowSize * height
        val fileSize = 54 + pixelDataSize

        val bmp = ByteArray(fileSize)

        bmp[0] = 0x42
        bmp[1] = 0x4D
        writeIntLE(bmp, 2, fileSize)
        writeIntLE(bmp, 10, 54)

        writeIntLE(bmp, 14, 40)
        writeIntLE(bmp, 18, width)
        writeIntLE(bmp, 22, height)
        writeShortLE(bmp, 26, 1)
        writeShortLE(bmp, 28, bitsPerPixel)
        writeIntLE(bmp, 30, 0)
        writeIntLE(bmp, 34, pixelDataSize)

        for (i in 54 until fileSize) {
            bmp[i] = ((i - 54) % 256).toByte()
        }

        return bmp
    }

    private fun writeIntLE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value shr 8).toByte()
        data[offset + 2] = (value shr 16).toByte()
        data[offset + 3] = (value shr 24).toByte()
    }

    private fun writeShortLE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value shr 8).toByte()
    }
}
