package io.openrise.stegorouter.carrier.document

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.SvgStegoConfig

class SvgCarrier(
    private val config: SvgStegoConfig = SvgStegoConfig()
) : CarrierAlgorithm {

    private val PAYLOAD_MARKER = "<!-- SR:"
    private val PAYLOAD_END = "-->"

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        val svgText = String(carrier, Charsets.UTF_8)
        require(svgText.contains("<svg")) { "Invalid SVG file" }

        val capacityBytes = calculateCapacity(carrier)
        BoundaryValidator.validateCapacity(payload.size, capacityBytes, "SVG")

        var result = svgText

        if (config.useComments) {
            result = embedInComments(result, payload)
        }

        if (config.useWhitespace) {
            result = embedInWhitespace(result, payload)
        }

        if (config.useAttributes) {
            result = embedInAttributes(result, payload)
        }

        return result.toByteArray(Charsets.UTF_8)
    }

    override fun extract(carrier: ByteArray): ByteArray {
        val svgText = String(carrier, Charsets.UTF_8)
        require(svgText.contains("<svg")) { "Invalid SVG file" }

        val payloads = mutableListOf<ByteArray>()

        if (config.useComments) {
            val fromComments = extractFromComments(svgText)
            if (fromComments.isNotEmpty()) payloads.add(fromComments)
        }

        if (config.useWhitespace) {
            val fromWhitespace = extractFromWhitespace(svgText)
            if (fromWhitespace.isNotEmpty()) payloads.add(fromWhitespace)
        }

        if (config.useAttributes) {
            val fromAttributes = extractFromAttributes(svgText)
            if (fromAttributes.isNotEmpty()) payloads.add(fromAttributes)
        }

        return payloads.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        val svgText = String(carrier, Charsets.UTF_8)
        require(svgText.contains("<svg")) { "Invalid SVG file" }

        var capacity = 0L

        if (config.useComments) {
            capacity += 65535
        }

        if (config.useWhitespace) {
            val tagCount = svgText.count { it == '>' }
            capacity += tagCount / 8
        }

        if (config.useAttributes) {
            val numericAttrs = Regex("""\w+="[\d.]+"""").findAll(svgText).count()
            capacity += numericAttrs / 8
        }

        return capacity
    }

    override fun supportedType(): FileType = FileType.SVG

    private fun embedInComments(svgText: String, payload: ByteArray): String {
        val encoded = payload.joinToString("") { String.format("%02x", it) }
        val comment = "$PAYLOAD_MARKER$encoded$PAYLOAD_END"

        val insertPos = svgText.indexOf("</svg>")
        if (insertPos < 0) return svgText

        return svgText.substring(0, insertPos) + comment + svgText.substring(insertPos)
    }

    private fun extractFromComments(svgText: String): ByteArray {
        val startMarker = svgText.indexOf(PAYLOAD_MARKER)
        if (startMarker < 0) return ByteArray(0)

        val endMarker = svgText.indexOf(PAYLOAD_END, startMarker)
        if (endMarker < 0) return ByteArray(0)

        val hex = svgText.substring(startMarker + PAYLOAD_MARKER.length, endMarker)
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun embedInWhitespace(svgText: String, payload: ByteArray): String {
        val result = StringBuilder()
        var bitIndex = 0
        val bits = payload.flatMap { byte ->
            (7 downTo 0).map { pos -> (byte.toInt() shr pos) and 1 }
        }

        for (i in svgText.indices) {
            result.append(svgText[i])
            if (svgText[i] == '>' && bitIndex < bits.size) {
                val bit = bits[bitIndex]
                result.append(if (bit == 1) "\t" else " ")
                bitIndex++
            }
        }

        return result.toString()
    }

    private fun extractFromWhitespace(svgText: String): ByteArray {
        val bits = mutableListOf<Int>()

        for (i in svgText.indices) {
            if (svgText[i] == '>') {
                if (i + 1 < svgText.length) {
                    when (svgText[i + 1]) {
                        '\t' -> bits.add(1)
                        ' ' -> bits.add(0)
                    }
                }
            }
        }

        val bytes = mutableListOf<Byte>()
        for (i in bits.indices step 8) {
            if (i + 8 <= bits.size) {
                var byte = 0
                for (j in 0 until 8) {
                    byte = (byte shl 1) or bits[i + j]
                }
                bytes.add(byte.toByte())
            }
        }

        return bytes.toByteArray()
    }

    private fun embedInAttributes(svgText: String, payload: ByteArray): String {
        val numericPattern = Regex("""(\w+=")([\d.]+)(")""")
        val bits = payload.flatMap { byte ->
            (7 downTo 0).map { pos -> (byte.toInt() shr pos) and 1 }
        }

        var bitIndex = 0
        return numericPattern.replace(svgText) { match ->
            if (bitIndex < bits.size) {
                val prefix = match.groupValues[1]
                val value = match.groupValues[2]
                val suffix = match.groupValues[3]
                val bit = bits[bitIndex]
                bitIndex++

                val modified = if (value.contains('.')) {
                    val parts = value.split('.')
                    val lastDigit = parts[1].last().digitToInt()
                    val newLastDigit = (lastDigit and 0xFE) or bit
                    "${parts[0]}.${parts[1].dropLast(1)}$newLastDigit"
                } else {
                    val num = value.toIntOrNull() ?: return@replace match.value
                    val modified = (num and 0xFE) or bit
                    "$modified.0"
                }

                "$prefix$modified$suffix"
            } else {
                match.value
            }
        }
    }

    private fun extractFromAttributes(svgText: String): ByteArray {
        val numericPattern = Regex("""\w+="([\d.]+)"""")
        val bits = mutableListOf<Int>()

        numericPattern.findAll(svgText).forEach { match ->
            val value = match.groupValues[1]
            if (value.contains('.')) {
                val lastDigit = value.last().digitToIntOrNull() ?: 0
                bits.add(lastDigit and 1)
            }
        }

        val bytes = mutableListOf<Byte>()
        for (i in bits.indices step 8) {
            if (i + 8 <= bits.size) {
                var byte = 0
                for (j in 0 until 8) {
                    byte = (byte shl 1) or bits[i + j]
                }
                bytes.add(byte.toByte())
            }
        }

        return bytes.toByteArray()
    }
}
