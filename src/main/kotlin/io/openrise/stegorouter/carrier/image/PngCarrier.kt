package io.openrise.stegorouter.carrier.image

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.ImageStegoConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class PngCarrier(
    config: ImageStegoConfig = ImageStegoConfig()
) : ImageCarrierAlgorithm(config) {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        require(carrier.size >= 8) { "Invalid PNG file" }
        require(carrier.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) { "Invalid PNG signature" }

        val chunks = parseChunks(carrier)
        val ihdr = chunks.first { it.type == "IHDR" }
        val width = readInt(ihdr.data, 0)
        val height = readInt(ihdr.data, 4)
        val bitDepth = ihdr.data[8].toInt() and 0xFF
        val colorType = ihdr.data[9].toInt() and 0xFF
        val channels = getChannels(colorType)

        require(bitDepth == 8) { "Only 8-bit PNG supported" }

        val capacityBytes = calculateCapacityBits(width, height, channels) / 8
        BoundaryValidator.validateCapacity(payload.size, capacityBytes, "PNG")

        val idatChunks = chunks.filter { it.type == "IDAT" }
        val compressedData = idatChunks.fold(ByteArray(0)) { acc, chunk -> acc + chunk.data }
        val decompressed = decompress(compressedData)

        val stride = width * channels + 1
        val modified = embedWithFilters(decompressed, payload, stride, width, channels)

        val recompressed = compress(modified)

        val result = ByteArrayOutputStream()
        result.write(PNG_SIGNATURE)

        for (chunk in chunks) {
            if (chunk.type == "IDAT") {
                if (chunk == idatChunks.first()) {
                    writeChunk(result, "IDAT", recompressed)
                }
            } else {
                writeChunk(result, chunk.type, chunk.data)
            }
        }

        return result.toByteArray()
    }

    override fun extract(carrier: ByteArray): ByteArray {
        require(carrier.size >= 8) { "Invalid PNG file" }
        require(carrier.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) { "Invalid PNG signature" }

        val chunks = parseChunks(carrier)
        val ihdr = chunks.first { it.type == "IHDR" }
        val width = readInt(ihdr.data, 0)
        val height = readInt(ihdr.data, 4)
        val colorType = ihdr.data[9].toInt() and 0xFF
        val channels = getChannels(colorType)

        val idatChunks = chunks.filter { it.type == "IDAT" }
        val compressedData = idatChunks.fold(ByteArray(0)) { acc, chunk -> acc + chunk.data }
        val decompressed = decompress(compressedData)

        val stride = width * channels + 1
        val totalBits = width * height * channels * config.bitsPerChannel
        val payloadSize = totalBits / 8

        return extractWithFilters(decompressed, payloadSize, stride, width, channels)
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        require(carrier.size >= 8) { "Invalid PNG file" }
        require(carrier.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) { "Invalid PNG signature" }

        val chunks = parseChunks(carrier)
        val ihdr = chunks.first { it.type == "IHDR" }
        val width = readInt(ihdr.data, 0)
        val height = readInt(ihdr.data, 4)
        val colorType = ihdr.data[9].toInt() and 0xFF
        val channels = getChannels(colorType)

        return calculateCapacityBits(width, height, channels) / 8
    }

    override fun supportedType(): FileType = FileType.PNG

    private fun getChannels(colorType: Int): Int = when (colorType) {
        0 -> 1
        2 -> 3
        3 -> 1
        4 -> 2
        6 -> 4
        else -> throw IllegalArgumentException("Unsupported color type: $colorType")
    }

    private data class Chunk(val type: String, val data: ByteArray)

    private fun parseChunks(data: ByteArray): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var offset = 8

        while (offset < data.size) {
            val length = readInt(data, offset)
            val type = String(data, offset + 4, 4, Charsets.US_ASCII)
            val chunkData = data.copyOfRange(offset + 8, offset + 8 + length)
            chunks.add(Chunk(type, chunkData))
            offset += 12 + length
            if (type == "IEND") break
        }

        return chunks
    }

    private fun readInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }

    private fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value shr 24).toByte()
        data[offset + 1] = (value shr 16).toByte()
        data[offset + 2] = (value shr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    private fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        val input = InflaterInputStream(ByteArrayInputStream(data), inflater)
        return input.readBytes()
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

    private fun embedWithFilters(
        data: ByteArray,
        payload: ByteArray,
        stride: Int,
        width: Int,
        channels: Int
    ): ByteArray {
        val result = data.clone()
        var bitIndex = 0
        val totalBits = payload.size * 8

        for (byte in payload) {
            for (bitPos in 7 downTo 0) {
                if (bitIndex >= totalBits) break

                val row = bitIndex / (width * channels * config.bitsPerChannel)
                val pixelInRow = (bitIndex / config.bitsPerChannel) % (width * channels)
                val channelBit = bitIndex % config.bitsPerChannel

                val dataOffset = row * stride + 1 + pixelInRow

                if (dataOffset < result.size) {
                    val bit = ((byte.toInt() shr bitPos) and 1)
                    result[dataOffset] = modifyPixelValue(result[dataOffset], bit, channelBit)
                }
                bitIndex++
            }
        }

        return result
    }

    private fun extractWithFilters(
        data: ByteArray,
        payloadSize: Int,
        stride: Int,
        width: Int,
        channels: Int
    ): ByteArray {
        val result = ByteArray(payloadSize)
        var bitIndex = 0

        for (byteIndex in result.indices) {
            var byte = 0
            for (bitPos in 7 downTo 0) {
                val row = bitIndex / (width * channels * config.bitsPerChannel)
                val pixelInRow = (bitIndex / config.bitsPerChannel) % (width * channels)
                val channelBit = bitIndex % config.bitsPerChannel

                val dataOffset = row * stride + 1 + pixelInRow

                if (dataOffset < data.size) {
                    val bit = extractBit(data[dataOffset], channelBit)
                    byte = (byte shl 1) or bit
                }
                bitIndex++
            }
            result[byteIndex] = byte.toByte()
        }

        return result
    }
}
