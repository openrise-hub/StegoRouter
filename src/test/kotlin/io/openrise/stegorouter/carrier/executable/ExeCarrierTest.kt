package io.openrise.stegorouter.carrier.executable

import io.openrise.stegorouter.carrier.FileType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ExeCarrierTest {

    @Test
    fun `embed and extract with minimal EXE`() {
        val carrier = createMinimalExe()
        val payload = "Hello, EXE!".toByteArray()
        val exeCarrier = ExeCarrier()

        val embedded = exeCarrier.embed(carrier, payload)
        val extracted = exeCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with binary data`() {
        val carrier = createMinimalExe()
        val payload = ByteArray(100) { it.toByte() }
        val exeCarrier = ExeCarrier()

        val embedded = exeCarrier.embed(carrier, payload)
        val extracted = exeCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `calculate capacity returns large value`() {
        val carrier = createMinimalExe()
        val exeCarrier = ExeCarrier()

        val capacity = exeCarrier.calculateCapacity(carrier)

        assertThat(capacity).isGreaterThan(1000000)
    }

    @Test
    fun `embed throws exception for invalid EXE`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val exeCarrier = ExeCarrier()

        assertThatThrownBy { exeCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `extract returns empty array when no payload`() {
        val carrier = createMinimalExe()
        val exeCarrier = ExeCarrier()

        val extracted = exeCarrier.extract(carrier)

        assertThat(extracted).isEmpty()
    }

    @Test
    fun `supported type returns EXE`() {
        val exeCarrier = ExeCarrier()

        assertThat(exeCarrier.supportedType()).isEqualTo(FileType.EXE)
    }

    @Test
    fun `byte level verification - embedded data differs from original`() {
        val carrier = createMinimalExe()
        val payload = "Test".toByteArray()
        val exeCarrier = ExeCarrier()

        val embedded = exeCarrier.embed(carrier, payload)

        assertThat(embedded).isNotEqualTo(carrier)
        assertThat(embedded.size).isGreaterThan(carrier.size)
    }

    private fun createMinimalExe(): ByteArray {
        val exe = ByteArray(100)
        exe[0] = 0x4D
        exe[1] = 0x5A
        for (i in 2 until 100) {
            exe[i] = (i % 256).toByte()
        }
        return exe
    }
}
