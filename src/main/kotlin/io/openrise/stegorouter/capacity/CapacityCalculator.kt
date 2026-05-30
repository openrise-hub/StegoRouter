package io.openrise.stegorouter.capacity

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.routing.UnsupportedFileTypeException
import io.openrise.stegorouter.validation.MagicByteValidator

class CapacityCalculator {
    private val algorithms = mutableMapOf<FileType, CarrierAlgorithm>()

    fun register(algorithm: CarrierAlgorithm) {
        algorithms[algorithm.supportedType()] = algorithm
    }

    fun calculateCapacity(carrier: ByteArray): Long {
        val fileType = MagicByteValidator.validate(carrier)
        val algorithm = algorithms[fileType]
            ?: throw UnsupportedFileTypeException("No algorithm registered for file type: $fileType")
        return algorithm.calculateCapacity(carrier)
    }
}
