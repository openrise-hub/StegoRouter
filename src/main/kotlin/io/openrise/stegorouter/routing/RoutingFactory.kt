package io.openrise.stegorouter.routing

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.validation.MagicByteValidator

class RoutingFactory {
    private val algorithms = mutableMapOf<FileType, CarrierAlgorithm>()

    fun register(algorithm: CarrierAlgorithm) {
        algorithms[algorithm.supportedType()] = algorithm
    }

    fun resolve(carrier: ByteArray): CarrierAlgorithm {
        val fileType = MagicByteValidator.validate(carrier)
        return algorithms[fileType]
            ?: throw UnsupportedFileTypeException("No algorithm registered for file type: $fileType")
    }
}
