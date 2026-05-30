package io.openrise.stegorouter.carrier.executable

import io.openrise.stegorouter.carrier.FileType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ElfCarrierTest {

    @Test
    fun `embed and extract with minimal ELF`() {
        val carrier = createMinimalElf()
        val payload = "Hello, ELF!".toByteArray()
        val elfCarrier = ElfCarrier()

        val embedded = elfCarrier.embed(carrier, payload)
        val extracted = elfCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with binary data`() {
        val carrier = createMinimalElf()
        val payload = ByteArray(100) { it.toByte() }
        val elfCarrier = ElfCarrier()

        val embedded = elfCarrier.embed(carrier, payload)
        val extracted = elfCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `calculate capacity returns large value`() {
        val carrier = createMinimalElf()
        val elfCarrier = ElfCarrier()

        val capacity = elfCarrier.calculateCapacity(carrier)

        assertThat(capacity).isGreaterThan(1000000)
    }

    @Test
    fun `embed throws exception for invalid ELF`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val elfCarrier = ElfCarrier()

        assertThatThrownBy { elfCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `extract returns empty array when no payload`() {
        val carrier = createMinimalElf()
        val elfCarrier = ElfCarrier()

        val extracted = elfCarrier.extract(carrier)

        assertThat(extracted).isEmpty()
    }

    @Test
    fun `supported type returns ELF`() {
        val elfCarrier = ElfCarrier()

        assertThat(elfCarrier.supportedType()).isEqualTo(FileType.ELF)
    }

    @Test
    fun `byte level verification - embedded data differs from original`() {
        val carrier = createMinimalElf()
        val payload = "Test".toByteArray()
        val elfCarrier = ElfCarrier()

        val embedded = elfCarrier.embed(carrier, payload)

        assertThat(embedded).isNotEqualTo(carrier)
        assertThat(embedded.size).isGreaterThan(carrier.size)
    }

    private fun createMinimalElf(): ByteArray {
        val elf = ByteArray(100)
        elf[0] = 0x7F
        elf[1] = 0x45
        elf[2] = 0x4C
        elf[3] = 0x46
        for (i in 4 until 100) {
            elf[i] = (i % 256).toByte()
        }
        return elf
    }
}
