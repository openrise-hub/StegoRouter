package io.openrise.stegorouter.carrier.archive

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.ZipStegoConfig
import java.io.ByteArrayOutputStream

class ZipCarrier(
    private val config: ZipStegoConfig = ZipStegoConfig()
) : CarrierAlgorithm {

    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val EOCD_MAGIC = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
    private val STEGO_HEADER_ID = 0x5352

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        require(carrier.size >= 22) { "Invalid ZIP file" }
        require(carrier.copyOfRange(0, 4).contentEquals(ZIP_MAGIC)) { "Invalid ZIP signature" }

        val eocdOffset = findEocd(carrier)
        require(eocdOffset >= 0) { "End of central directory not found" }

        val capacityBytes = calculateCapacity(carrier)
        BoundaryValidator.validateCapacity(payload.size, capacityBytes, "ZIP")

        return when (config.embeddingMode) {
            ZipEmbeddingMode.COMMENT -> embedInComment(carrier, payload, eocdOffset)
            ZipEmbeddingMode.EXTRA_FIELD -> embedInExtraField(carrier, payload)
            ZipEmbeddingMode.BOTH -> embedInBoth(carrier, payload, eocdOffset)
        }
    }

    override fun extract(carrier: ByteArray): ByteArray {
        require(carrier.size >= 22) { "Invalid ZIP file" }
        require(carrier.copyOfRange(0, 4).contentEquals(ZIP_MAGIC)) { "Invalid ZIP signature" }

        val eocdOffset = findEocd(carrier)
        require(eocdOffset >= 0) { "End of central directory not found" }

        val fromExtra = extractFromExtraField(carrier)
        val fromComment = extractFromComment(carrier, eocdOffset)

        return when (config.embeddingMode) {
            ZipEmbeddingMode.EXTRA_FIELD -> fromExtra
            ZipEmbeddingMode.COMMENT -> fromComment
            ZipEmbeddingMode.BOTH -> fromExtra + fromComment
        }
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        require(carrier.size >= 22) { "Invalid ZIP file" }
        require(carrier.copyOfRange(0, 4).contentEquals(ZIP_MAGIC)) { "Invalid ZIP signature" }

        val eocdOffset = findEocd(carrier)
        require(eocdOffset >= 0) { "End of central directory not found" }

        val commentCapacity = 65535L
        val extraFieldCapacity = calculateExtraFieldCapacity(carrier)

        return when (config.embeddingMode) {
            ZipEmbeddingMode.COMMENT -> commentCapacity
            ZipEmbeddingMode.EXTRA_FIELD -> extraFieldCapacity
            ZipEmbeddingMode.BOTH -> extraFieldCapacity + commentCapacity
        }
    }

    override fun supportedType(): FileType = FileType.ZIP

    private fun findEocd(data: ByteArray): Int {
        for (i in data.size - 22 downTo 0) {
            if (data.copyOfRange(i, i + 4).contentEquals(EOCD_MAGIC)) {
                return i
            }
        }
        return -1
    }

    private fun embedInComment(carrier: ByteArray, payload: ByteArray, eocdOffset: Int): ByteArray {
        val result = carrier.copyOfRange(0, eocdOffset + 22)

        val payloadWithLength = ByteArray(4 + payload.size)
        writeIntLE(payloadWithLength, 0, payload.size)
        System.arraycopy(payload, 0, payloadWithLength, 4, payload.size)

        writeShortLE(result, eocdOffset + 20, payloadWithLength.size)

        return result + payloadWithLength
    }

    private fun extractFromComment(carrier: ByteArray, eocdOffset: Int): ByteArray {
        val commentLength = readShortLE(carrier, eocdOffset + 20)
        if (commentLength < 4) return ByteArray(0)

        val commentStart = eocdOffset + 22
        if (commentStart + 4 > carrier.size) return ByteArray(0)

        val payloadLength = readIntLE(carrier, commentStart)
        if (commentLength < 4 + payloadLength) return ByteArray(0)

        return carrier.copyOfRange(commentStart + 4, commentStart + 4 + payloadLength)
    }

    private fun embedInExtraField(carrier: ByteArray, payload: ByteArray): ByteArray {
        val localHeaderOffset = 4

        val fileNameLength = readShortLE(carrier, localHeaderOffset + 26)
        val existingExtraFieldLength = readShortLE(carrier, localHeaderOffset + 28)

        val extraFieldData = ByteArray(4 + 4 + payload.size)
        writeShortLE(extraFieldData, 0, STEGO_HEADER_ID)
        writeShortLE(extraFieldData, 2, payload.size)
        System.arraycopy(payload, 0, extraFieldData, 4, payload.size)

        val newExtraFieldLength = existingExtraFieldLength + extraFieldData.size

        val result = ByteArrayOutputStream()
        result.write(carrier, 0, localHeaderOffset + 28)

        val lengthBytes = ByteArray(2)
        writeShortLE(lengthBytes, 0, newExtraFieldLength)
        result.write(lengthBytes)

        result.write(carrier, localHeaderOffset + 30, fileNameLength + existingExtraFieldLength)
        result.write(extraFieldData)

        val dataStart = localHeaderOffset + 30 + fileNameLength + existingExtraFieldLength
        result.write(carrier, dataStart, carrier.size - dataStart)

        return result.toByteArray()
    }

    private fun extractFromExtraField(carrier: ByteArray): ByteArray {
        var offset = 4

        while (offset < carrier.size - 30) {
            if (!carrier.copyOfRange(offset, offset + 4).contentEquals(ZIP_MAGIC)) break

            val fileNameLength = readShortLE(carrier, offset + 26)
            val extraFieldLength = readShortLE(carrier, offset + 28)

            var extraOffset = offset + 30 + fileNameLength
            val extraEnd = extraOffset + extraFieldLength

            while (extraOffset < extraEnd - 4) {
                val headerId = readShortLE(carrier, extraOffset)
                val dataSize = readShortLE(carrier, extraOffset + 2)

                if (headerId == STEGO_HEADER_ID) {
                    return carrier.copyOfRange(extraOffset + 4, extraOffset + 4 + dataSize)
                }

                extraOffset += 4 + dataSize
            }

            val compressedSize = readIntLE(carrier, offset + 18)
            offset = extraEnd + compressedSize
        }

        return ByteArray(0)
    }

    private fun embedInBoth(carrier: ByteArray, payload: ByteArray, eocdOffset: Int): ByteArray {
        val halfSize = payload.size / 2
        val firstHalf = payload.copyOfRange(0, halfSize)
        val secondHalf = payload.copyOfRange(halfSize, payload.size)

        val withExtra = embedInExtraField(carrier, firstHalf)
        val newEocdOffset = findEocd(withExtra)
        return embedInComment(withExtra, secondHalf, newEocdOffset)
    }

    private fun calculateExtraFieldCapacity(carrier: ByteArray): Long {
        var offset = 4
        var totalCapacity = 0L

        while (offset < carrier.size - 30) {
            if (!carrier.copyOfRange(offset, offset + 4).contentEquals(ZIP_MAGIC)) break

            val extraFieldLength = readShortLE(carrier, offset + 28)
            totalCapacity += 65535 - extraFieldLength

            val fileNameLength = readShortLE(carrier, offset + 26)
            val compressedSize = readIntLE(carrier, offset + 18)
            offset += 30 + fileNameLength + extraFieldLength + compressedSize
        }

        return totalCapacity
    }

    private fun readIntLE(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readShortLE(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
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
