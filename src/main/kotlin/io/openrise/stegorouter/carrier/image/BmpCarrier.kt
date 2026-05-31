package io.openrise.stegorouter.carrier.image

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.ImageStegoConfig

class BmpCarrier(
    config: ImageStegoConfig = ImageStegoConfig()
) : ImageCarrierAlgorithm(config) {

    private val BMP_SIGNATURE = byteArrayOf(0x42, 0x4D)

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        require(carrier.size >= 54) { "Invalid BMP file" }
        require(carrier.copyOfRange(0, 2).contentEquals(BMP_SIGNATURE)) { "Invalid BMP signature" }

        val pixelOffset = readIntLE(carrier, 10)
        val width = readIntLE(carrier, 18)
        val height = readIntLE(carrier, 22)
        val bitsPerPixel = readShortLE(carrier, 28)
        val compression = readIntLE(carrier, 30)

        require(compression == 0) { "Only uncompressed BMP supported" }
        require(bitsPerPixel == 24 || bitsPerPixel == 32) { "Only 24/32-bit BMP supported" }

        val channels = bitsPerPixel / 8
        val absHeight = kotlin.math.abs(height)
        val rowSize = ((width * channels + 3) / 4) * 4
        val padding = rowSize - width * channels

        val capacityBytes = calculateCapacityBits(width, absHeight, channels) / 8
        BoundaryValidator.validateCapacity(payload.size, capacityBytes, "BMP")

        val result = carrier.clone()
        var bitIndex = 0
        val totalBits = payload.size * 8

        for (byte in payload) {
            for (bitPos in 7 downTo 0) {
                if (bitIndex >= totalBits) break

                val pixelIndex = bitIndex / config.bitsPerChannel
                val channelBit = bitIndex % config.bitsPerChannel

                val row = pixelIndex / (width * channels)
                val col = pixelIndex % (width * channels)

                if (row < absHeight) {
                    val yOffset = if (height > 0) (absHeight - 1 - row) else row
                    val pixelOffset2 = pixelOffset + yOffset * rowSize + col

                    if (pixelOffset2 < result.size) {
                        val bit = ((byte.toInt() shr bitPos) and 1)
                        result[pixelOffset2] = modifyPixelValue(result[pixelOffset2], bit, channelBit)
                    }
                }
                bitIndex++
            }
        }

        return result
    }

    override fun extract(carrier: ByteArray): ByteArray {
        require(carrier.size >= 54) { "Invalid BMP file" }
        require(carrier.copyOfRange(0, 2).contentEquals(BMP_SIGNATURE)) { "Invalid BMP signature" }

        val pixelOffset = readIntLE(carrier, 10)
        val width = readIntLE(carrier, 18)
        val height = readIntLE(carrier, 22)
        val bitsPerPixel = readShortLE(carrier, 28)
        val channels = bitsPerPixel / 8
        val absHeight = kotlin.math.abs(height)
        val rowSize = ((width * channels + 3) / 4) * 4

        val totalBits = width * absHeight * channels * config.bitsPerChannel
        val payloadSize = totalBits / 8

        val result = ByteArray(payloadSize)
        var bitIndex = 0

        for (byteIndex in result.indices) {
            var byte = 0
            for (bitPos in 7 downTo 0) {
                val pixelIndex = bitIndex / config.bitsPerChannel
                val channelBit = bitIndex % config.bitsPerChannel

                val row = pixelIndex / (width * channels)
                val col = pixelIndex % (width * channels)

                if (row < absHeight) {
                    val yOffset = if (height > 0) (absHeight - 1 - row) else row
                    val pixelOffset2 = pixelOffset + yOffset * rowSize + col

                    if (pixelOffset2 < carrier.size) {
                        val bit = extractBit(carrier[pixelOffset2], channelBit)
                        byte = (byte shl 1) or bit
                    }
                }
                bitIndex++
            }
            result[byteIndex] = byte.toByte()
        }

        return result
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        require(carrier.size >= 54) { "Invalid BMP file" }
        require(carrier.copyOfRange(0, 2).contentEquals(BMP_SIGNATURE)) { "Invalid BMP signature" }

        val width = readIntLE(carrier, 18)
        val height = readIntLE(carrier, 22)
        val bitsPerPixel = readShortLE(carrier, 28)
        val channels = bitsPerPixel / 8
        val absHeight = kotlin.math.abs(height)

        return calculateCapacityBits(width, absHeight, channels) / 8
    }

    override fun supportedType(): FileType = FileType.BMP

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
}
