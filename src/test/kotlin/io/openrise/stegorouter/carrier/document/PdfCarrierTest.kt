package io.openrise.stegorouter.carrier.document

import io.openrise.stegorouter.carrier.FileType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PdfCarrierTest {

    @Test
    fun `embed and extract with minimal PDF`() {
        val carrier = createMinimalPdf()
        val payload = "Hello, PDF!".toByteArray()
        val pdfCarrier = PdfCarrier()

        val embedded = pdfCarrier.embed(carrier, payload)
        val extracted = pdfCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with binary data`() {
        val carrier = createMinimalPdf()
        val payload = ByteArray(50) { it.toByte() }
        val pdfCarrier = PdfCarrier()

        val embedded = pdfCarrier.embed(carrier, payload)
        val extracted = pdfCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `calculate capacity returns positive value`() {
        val carrier = createMinimalPdf()
        val pdfCarrier = PdfCarrier()

        val capacity = pdfCarrier.calculateCapacity(carrier)

        assertThat(capacity).isGreaterThan(0)
    }

    @Test
    fun `embed throws exception for invalid PDF`() {
        val invalidCarrier = "not a pdf".toByteArray()
        val payload = "Test".toByteArray()
        val pdfCarrier = PdfCarrier()

        assertThatThrownBy { pdfCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns PDF`() {
        val pdfCarrier = PdfCarrier()

        assertThat(pdfCarrier.supportedType()).isEqualTo(FileType.PDF)
    }

    private fun createMinimalPdf(): ByteArray {
        val pdf = StringBuilder()
        pdf.append("%PDF-1.4\n")
        pdf.append("1 0 obj\n")
        pdf.append("<< /Type /Catalog /Pages 2 0 R >>\n")
        pdf.append("endobj\n")
        pdf.append("2 0 obj\n")
        pdf.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n")
        pdf.append("endobj\n")
        pdf.append("3 0 obj\n")
        pdf.append("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\n")
        pdf.append("endobj\n")
        pdf.append("xref\n")
        pdf.append("0 4\n")
        pdf.append("0000000000 65535 f \n")
        pdf.append("0000000009 00000 n \n")
        pdf.append("0000000058 00000 n \n")
        pdf.append("0000000115 00000 n \n")
        pdf.append("trailer\n")
        pdf.append("<< /Size 4 /Root 1 0 R >>\n")
        pdf.append("startxref\n")
        pdf.append("190\n")
        pdf.append("%%EOF\n")
        return pdf.toString().toByteArray(Charsets.ISO_8859_1)
    }
}
