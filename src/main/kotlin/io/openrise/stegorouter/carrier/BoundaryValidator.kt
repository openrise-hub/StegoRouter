package io.openrise.stegorouter.carrier

class PayloadCapacityException(message: String) : RuntimeException(message)

object BoundaryValidator {
    fun validateCapacity(payloadSize: Int, capacityBytes: Long, carrierType: String) {
        require(payloadSize >= 0) { "Payload size cannot be negative" }
        require(capacityBytes >= 0) { "Capacity cannot be negative" }

        if (payloadSize > capacityBytes) {
            throw PayloadCapacityException(
                "Payload too large for $carrierType: $payloadSize bytes > $capacityBytes bytes capacity"
            )
        }
    }

    fun validateCarrierSize(carrierSize: Int, minSize: Int, carrierType: String) {
        require(carrierSize >= minSize) {
            "$carrierType carrier too small: $carrierSize bytes < $minSize bytes minimum"
        }
    }

    fun clampPixelValue(value: Int, min: Int = 0, max: Int = 255): Int {
        return value.coerceIn(min, max)
    }

    fun clampSampleValue(value: Int, bitsPerSample: Int): Int {
        val max = (1 shl (bitsPerSample - 1)) - 1
        val min = -(1 shl (bitsPerSample - 1))
        return value.coerceIn(min, max)
    }
}
