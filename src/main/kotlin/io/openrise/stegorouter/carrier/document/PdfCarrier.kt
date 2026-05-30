package io.openrise.stegorouter.carrier.document

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType

class PdfCarrier : CarrierAlgorithm {

    private val parser = PdfParser()
    private val PAYLOAD_MARKER = "/StegoRouter"

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        val text = String(carrier, Charsets.ISO_8859_1)
        require(text.startsWith("%PDF-")) { "Invalid PDF file" }

        val structure = parser.parse(carrier)

        val capacityBytes = calculateCapacity(carrier)
        BoundaryValidator.validateCapacity(payload.size, capacityBytes, "PDF")

        val encoded = payload.joinToString("") { String.format("%02x", it) }
        val payloadDict = "<< $PAYLOAD_MARKER <${encoded}> >>"

        val modifications = mutableMapOf<Int, ByteArray>()

        if (structure.objects.isNotEmpty()) {
            val firstObj = structure.objects.first()
            val originalContent = String(firstObj.content, Charsets.ISO_8859_1)
            val modifiedContent = originalContent.replace(">>", "$payloadDict >>")
            modifications[firstObj.objectNumber] = modifiedContent.toByteArray(Charsets.ISO_8859_1)
        }

        return parser.rebuild(structure, modifications)
    }

    override fun extract(carrier: ByteArray): ByteArray {
        val text = String(carrier, Charsets.ISO_8859_1)
        require(text.startsWith("%PDF-")) { "Invalid PDF file" }

        val markerIndex = text.indexOf(PAYLOAD_MARKER)
        if (markerIndex < 0) return ByteArray(0)

        val startIndex = text.indexOf('<', markerIndex)
        val endIndex = text.indexOf('>', startIndex)

        if (startIndex < 0 || endIndex < 0) return ByteArray(0)

        val hex = text.substring(startIndex + 1, endIndex)
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        val text = String(carrier, Charsets.ISO_8859_1)
        require(text.startsWith("%PDF-")) { "Invalid PDF file" }

        val structure = parser.parse(carrier)
        val streamCount = structure.objects.count { obj ->
            String(obj.content, Charsets.ISO_8859_1).contains("stream")
        }

        return 65535L + streamCount * 1024
    }

    override fun supportedType(): FileType = FileType.PDF
}
