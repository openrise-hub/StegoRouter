package io.openrise.stegorouter.carrier.image

import io.openrise.stegorouter.carrier.BoundaryValidator
import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.config.ImageStegoConfig

abstract class ImageCarrierAlgorithm(
    protected val config: ImageStegoConfig = ImageStegoConfig()
) : CarrierAlgorithm {

    protected fun embedBits(pixels: ByteArray, payload: ByteArray): ByteArray {
        val result = pixels.clone()
        var bitIndex = 0
        val totalBits = payload.size * 8

        for (byte in payload) {
            for (bitPos in 7 downTo 0) {
                if (bitIndex >= totalBits) break

                val pixelIndex = bitIndex / config.bitsPerChannel
                val channelBit = bitIndex % config.bitsPerChannel

                if (pixelIndex < result.size) {
                    val bit = ((byte.toInt() shr bitPos) and 1)
                    result[pixelIndex] = modifyPixelValue(result[pixelIndex], bit, channelBit)
                }
                bitIndex++
            }
        }

        return result
    }

    protected fun extractBits(pixels: ByteArray, payloadSize: Int): ByteArray {
        val result = ByteArray(payloadSize)
        var bitIndex = 0

        for (byteIndex in result.indices) {
            var byte = 0
            for (bitPos in 7 downTo 0) {
                val pixelIndex = bitIndex / config.bitsPerChannel
                val channelBit = bitIndex % config.bitsPerChannel

                if (pixelIndex < pixels.size) {
                    val bit = extractBit(pixels[pixelIndex], channelBit)
                    byte = (byte shl 1) or bit
                }
                bitIndex++
            }
            result[byteIndex] = byte.toByte()
        }

        return result
    }

    private fun modifyPixelValue(pixel: Byte, bit: Int, channelBit: Int): Byte {
        val value = pixel.toInt() and 0xFF
        val mask = (1 shl config.bitsPerChannel) - 1
        val shift = channelBit

        val modified = when (config.lsbMode) {
            LsbMode.REPLACEMENT -> {
                val cleared = value and (mask.inv() shl shift)
                cleared or ((bit and mask) shl shift)
            }
            LsbMode.MATCHING -> {
                val currentBit = (value shr shift) and 1
                if (currentBit != bit) {
                    val delta = if (value < 128) 1 else -1
                    value + delta
                } else {
                    value
                }
            }
        }

        return BoundaryValidator.clampPixelValue(modified).toByte()
    }

    private fun extractBit(pixel: Byte, channelBit: Int): Int {
        val value = pixel.toInt() and 0xFF
        return (value shr channelBit) and 1
    }

    protected fun calculateCapacityBits(width: Int, height: Int, channels: Int): Long {
        return width.toLong() * height * channels * config.bitsPerChannel
    }
}
