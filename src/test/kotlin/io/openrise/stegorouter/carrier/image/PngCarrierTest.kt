package io.openrise.stegorouter.carrier.image

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.carrier.PayloadCapacityException
import io.openrise.stegorouter.config.ImageStegoConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class PngCarrierTest {

    @Test
    fun `embed and extract with default config`() {
        val carrier = createMinimalPng(10, 10, 3)
        val payload = "Hello, PNG!".toByteArray()
        val pngCarrier = PngCarrier()

        val embedded = pngCarrier.embed(carrier, payload)
        val extracted = pngCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with LSB replacement mode`() {
        val carrier = createMinimalPng(10, 10, 3)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(lsbMode = LsbMode.REPLACEMENT, bitsPerChannel = 1)
        val pngCarrier = PngCarrier(config)

        val embedded = pngCarrier.embed(carrier, payload)
        val extracted = pngCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with LSB matching mode`() {
        val carrier = createMinimalPng(10, 10, 3)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(lsbMode = LsbMode.MATCHING, bitsPerChannel = 1)
        val pngCarrier = PngCarrier(config)

        val embedded = pngCarrier.embed(carrier, payload)
        val extracted = pngCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with 2 bits per channel`() {
        val carrier = createMinimalPng(10, 10, 3)
        val payload = "Test".toByteArray()
        val config = ImageStegoConfig(bitsPerChannel = 2)
        val pngCarrier = PngCarrier(config)

        val embedded = pngCarrier.embed(carrier, payload)
        val extracted = pngCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `calculate capacity returns correct value`() {
        val carrier = createMinimalPng(100, 100, 3)
        val pngCarrier = PngCarrier()

        val capacity = pngCarrier.calculateCapacity(carrier)

        assertThat(capacity).isEqualTo(100L * 100 * 3 / 8)
    }

    @Test
    fun `calculate capacity with 2 bits per channel`() {
        val carrier = createMinimalPng(100, 100, 3)
        val config = ImageStegoConfig(bitsPerChannel = 2)
        val pngCarrier = PngCarrier(config)

        val capacity = pngCarrier.calculateCapacity(carrier)

        assertThat(capacity).isEqualTo(100L * 100 * 3 * 2 / 8)
    }

    @Test
    fun `embed throws exception when payload too large`() {
        val carrier = createMinimalPng(2, 2, 3)
        val payload = ByteArray(100)
        val pngCarrier = PngCarrier()

        assertThatThrownBy { pngCarrier.embed(carrier, payload) }
            .isInstanceOf(PayloadCapacityException::class.java)
    }

    @Test
    fun `embed throws exception for invalid PNG`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val pngCarrier = PngCarrier()

        assertThatThrownBy { pngCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns PNG`() {
        val pngCarrier = PngCarrier()

        assertThat(pngCarrier.supportedType()).isEqualTo(FileType.PNG)
    }

    @Test
    fun `byte level verification - embedded data differs from original`() {
        val carrier = createMinimalPng(10, 10, 3)
        val payload = "Test".toByteArray()
        val pngCarrier = PngCarrier()

        val embedded = pngCarrier.embed(carrier, payload)

        assertThat(embedded).isNotEqualTo(carrier)
    }

    private fun createMinimalPng(width: Int, height: Int, channels: Int): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        val ihdrData = ByteArray(13)
        writeInt(ihdrData, 0, width)
        writeInt(ihdrData, 4, height)
        ihdrData[8] = 8
        ihdrData[9] = if (channels == 3) 2 else 6
        ihdrData[10] = 0
        ihdrData[11] = 0
        ihdrData[12] = 0
        writeChunk(output, "IHDR", ihdrData)

        val imageData = createImageData(width, height, channels)
        val compressed = compress(imageData)
        writeChunk(output, "IDAT", compressed)

        writeChunk(output, "IEND", ByteArray(0))

        return output.toByteArray()
    }

    private fun createImageData(width: Int, height: Int, channels: Int): ByteArray {
        val rowSize = width * channels + 1
        val data = ByteArray(rowSize * height)

        for (row in 0 until height) {
            data[row * rowSize] = 0
            for (col in 0 until width * channels) {
                data[row * rowSize + 1 + col] = ((row + col) % 256).toByte()
            }
        }

        return data
    }

    private fun compress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val deflater = Deflater()
        val deflaterStream = DeflaterOutputStream(output, deflater)
        deflaterStream.write(data)
        deflaterStream.close()
        return output.toByteArray()
    }

    private fun writeChunk(output: ByteArrayOutputStream, type: String, data: ByteArray) {
        val lengthBytes = ByteArray(4)
        writeInt(lengthBytes, 0, data.size)
        output.write(lengthBytes)

        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        output.write(typeBytes)
        output.write(data)

        val crc = calculateCrc(typeBytes + data)
        val crcBytes = ByteArray(4)
        writeInt(crcBytes, 0, crc)
        output.write(crcBytes)
    }

    private fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 24).toByte()
        data[offset + 1] = (value shr 16).toByte()
        data[offset + 2] = (value shr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    private fun calculateCrc(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if ((crc and 1) != 0) {
                    (crc ushr 1) xor 0xEDB88320.toInt()
                } else {
                    crc ushr 1
                }
            }
        }
        return crc xor 0xFFFFFFFF.toInt()
    }
}
