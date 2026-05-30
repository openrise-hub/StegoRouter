package io.openrise.stegorouter.carrier.audio

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType

class WavCarrier : CarrierAlgorithm {

    private val RIFF_MAGIC = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WAVE_MAGIC = byteArrayOf(0x57, 0x41, 0x56, 0x45)

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        require(carrier.size >= 44) { "Invalid WAV file" }
        require(carrier.copyOfRange(0, 4).contentEquals(RIFF_MAGIC)) { "Invalid RIFF signature" }
        require(carrier.copyOfRange(8, 12).contentEquals(WAVE_MAGIC)) { "Invalid WAVE signature" }

        val audioFormat = readShortLE(carrier, 20)
        val numChannels = readShortLE(carrier, 22)
        val bitsPerSample = readShortLE(carrier, 34)

        require(audioFormat == 1) { "Only PCM WAV supported" }
        require(bitsPerSample == 16) { "Only 16-bit WAV supported" }

        val dataOffset = findDataChunk(carrier)
        require(dataOffset > 0) { "Data chunk not found" }

        val dataSize = readIntLE(carrier, dataOffset + 4)
        val numSamples = dataSize / (bitsPerSample / 8)

        val capacityBytes = numSamples
        require(payload.size <= capacityBytes) {
            "Payload too large: ${payload.size} > $capacityBytes"
        }

        val result = carrier.clone()
        val sampleStart = dataOffset + 8

        var bitIndex = 0
        val totalBits = payload.size * 8

        for (byte in payload) {
            for (bitPos in 7 downTo 0) {
                if (bitIndex >= totalBits) break

                val sampleIndex = bitIndex
                val sampleOffset = sampleStart + sampleIndex * 2

                if (sampleOffset + 1 < result.size) {
                    val bit = ((byte.toInt() shr bitPos) and 1)
                    val sample = readShortLE(result, sampleOffset)
                    val modified = (sample and 0xFFFE) or bit
                    writeShortLE(result, sampleOffset, modified)
                }
                bitIndex++
            }
        }

        return result
    }

    override fun extract(carrier: ByteArray): ByteArray {
        require(carrier.size >= 44) { "Invalid WAV file" }
        require(carrier.copyOfRange(0, 4).contentEquals(RIFF_MAGIC)) { "Invalid RIFF signature" }
        require(carrier.copyOfRange(8, 12).contentEquals(WAVE_MAGIC)) { "Invalid WAVE signature" }

        val bitsPerSample = readShortLE(carrier, 34)
        val dataOffset = findDataChunk(carrier)
        require(dataOffset > 0) { "Data chunk not found" }

        val dataSize = readIntLE(carrier, dataOffset + 4)
        val numSamples = dataSize / (bitsPerSample / 8)

        val payloadSize = numSamples / 8
        val result = ByteArray(payloadSize)

        val sampleStart = dataOffset + 8
        var bitIndex = 0

        for (byteIndex in result.indices) {
            var byte = 0
            for (bitPos in 7 downTo 0) {
                val sampleIndex = bitIndex
                val sampleOffset = sampleStart + sampleIndex * 2

                if (sampleOffset + 1 < carrier.size) {
                    val sample = readShortLE(carrier, sampleOffset)
                    val bit = sample and 1
                    byte = (byte shl 1) or bit
                }
                bitIndex++
            }
            result[byteIndex] = byte.toByte()
        }

        return result
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        require(carrier.size >= 44) { "Invalid WAV file" }
        require(carrier.copyOfRange(0, 4).contentEquals(RIFF_MAGIC)) { "Invalid RIFF signature" }
        require(carrier.copyOfRange(8, 12).contentEquals(WAVE_MAGIC)) { "Invalid WAVE signature" }

        val bitsPerSample = readShortLE(carrier, 34)
        val dataOffset = findDataChunk(carrier)
        require(dataOffset > 0) { "Data chunk not found" }

        val dataSize = readIntLE(carrier, dataOffset + 4)
        val numSamples = dataSize / (bitsPerSample / 8)

        return numSamples.toLong() / 8
    }

    override fun supportedType(): FileType = FileType.WAV

    private fun findDataChunk(data: ByteArray): Int {
        var offset = 12
        while (offset < data.size - 8) {
            val chunkId = String(data, offset, 4, Charsets.US_ASCII)
            if (chunkId == "data") {
                return offset
            }
            val chunkSize = readIntLE(data, offset + 4)
            offset += 8 + chunkSize
        }
        return -1
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

    private fun writeShortLE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value shr 8).toByte()
    }
}
