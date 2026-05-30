package io.openrise.stegorouter.carrier.audio

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.carrier.PayloadCapacityException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class WavCarrierTest {

    @Test
    fun `embed and extract with default config`() {
        val carrier = createMinimalWav(1000)
        val payload = "Hello, WAV!".toByteArray()
        val wavCarrier = WavCarrier()

        val embedded = wavCarrier.embed(carrier, payload)
        val extracted = wavCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `embed and extract with binary data`() {
        val carrier = createMinimalWav(1000)
        val payload = ByteArray(50) { it.toByte() }
        val wavCarrier = WavCarrier()

        val embedded = wavCarrier.embed(carrier, payload)
        val extracted = wavCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    @Test
    fun `calculate capacity returns correct value`() {
        val carrier = createMinimalWav(8000)
        val wavCarrier = WavCarrier()

        val capacity = wavCarrier.calculateCapacity(carrier)

        assertThat(capacity).isEqualTo(1000L)
    }

    @Test
    fun `embed throws exception when payload too large`() {
        val carrier = createMinimalWav(100)
        val payload = ByteArray(100)
        val wavCarrier = WavCarrier()

        assertThatThrownBy { wavCarrier.embed(carrier, payload) }
            .isInstanceOf(PayloadCapacityException::class.java)
    }

    @Test
    fun `embed throws exception for invalid WAV`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val wavCarrier = WavCarrier()

        assertThatThrownBy { wavCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns WAV`() {
        val wavCarrier = WavCarrier()

        assertThat(wavCarrier.supportedType()).isEqualTo(FileType.WAV)
    }

    @Test
    fun `byte level verification - embedded data differs from original`() {
        val carrier = createMinimalWav(1000)
        val payload = "Test".toByteArray()
        val wavCarrier = WavCarrier()

        val embedded = wavCarrier.embed(carrier, payload)

        assertThat(embedded).isNotEqualTo(carrier)
    }

    @Test
    fun `embed and extract with stereo audio`() {
        val carrier = createMinimalWav(1000, numChannels = 2)
        val payload = "Stereo".toByteArray()
        val wavCarrier = WavCarrier()

        val embedded = wavCarrier.embed(carrier, payload)
        val extracted = wavCarrier.extract(embedded)

        assertThat(extracted).startsWith(payload)
    }

    private fun createMinimalWav(numSamples: Int, numChannels: Int = 1): ByteArray {
        val bitsPerSample = 16
        val sampleRate = 44100
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * blockAlign
        val fileSize = 44 + dataSize

        val wav = ByteArray(fileSize)

        wav[0] = 0x52
        wav[1] = 0x49
        wav[2] = 0x46
        wav[3] = 0x46
        writeIntLE(wav, 4, fileSize - 8)
        wav[8] = 0x57
        wav[9] = 0x41
        wav[10] = 0x56
        wav[11] = 0x45

        wav[12] = 0x66
        wav[13] = 0x6D
        wav[14] = 0x74
        wav[15] = 0x20
        writeIntLE(wav, 16, 16)
        writeShortLE(wav, 20, 1)
        writeShortLE(wav, 22, numChannels)
        writeIntLE(wav, 24, sampleRate)
        writeIntLE(wav, 28, byteRate)
        writeShortLE(wav, 32, blockAlign)
        writeShortLE(wav, 34, bitsPerSample)

        wav[36] = 0x64
        wav[37] = 0x61
        wav[38] = 0x74
        wav[39] = 0x61
        writeIntLE(wav, 40, dataSize)

        for (i in 44 until fileSize) {
            val sample = ((i - 44) % 1000) - 500
            writeShortLE(wav, i, sample)
            i++
        }

        return wav
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
