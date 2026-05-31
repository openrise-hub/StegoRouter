package io.openrise.stegorouter.carrier.archive

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.ZipStegoConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCarrierTest {

    @Test
    fun `embed and extract with comment mode`() {
        val carrier = createMinimalZip()
        val payload = "Hello, ZIP!".toByteArray()
        val config = ZipStegoConfig(embeddingMode = ZipEmbeddingMode.COMMENT)
        val zipCarrier = ZipCarrier(config)

        val embedded = zipCarrier.embed(carrier, payload)
        val extracted = zipCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with extra field mode`() {
        val carrier = createMinimalZip()
        val payload = "Test".toByteArray()
        val config = ZipStegoConfig(embeddingMode = ZipEmbeddingMode.EXTRA_FIELD)
        val zipCarrier = ZipCarrier(config)

        val embedded = zipCarrier.embed(carrier, payload)
        val extracted = zipCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with both modes`() {
        val carrier = createMinimalZip()
        val payload = "Test".toByteArray()
        val config = ZipStegoConfig(embeddingMode = ZipEmbeddingMode.BOTH)
        val zipCarrier = ZipCarrier(config)

        val embedded = zipCarrier.embed(carrier, payload)
        val extracted = zipCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `calculate capacity returns positive value`() {
        val carrier = createMinimalZip()
        val zipCarrier = ZipCarrier()

        val capacity = zipCarrier.calculateCapacity(carrier)

        assertThat(capacity).isGreaterThan(0)
    }

    @Test
    fun `embed throws exception for invalid ZIP`() {
        val invalidCarrier = ByteArray(100)
        val payload = "Test".toByteArray()
        val zipCarrier = ZipCarrier()

        assertThatThrownBy { zipCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns ZIP`() {
        val zipCarrier = ZipCarrier()

        assertThat(zipCarrier.supportedType()).isEqualTo(FileType.ZIP)
    }

    private fun createMinimalZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zipOut ->
            val entry = ZipEntry("test.txt")
            zipOut.putNextEntry(entry)
            zipOut.write("test content".toByteArray())
            zipOut.closeEntry()
        }
        return output.toByteArray()
    }
}
