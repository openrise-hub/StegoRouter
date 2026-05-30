package io.openrise.stegorouter.carrier

interface CarrierAlgorithm {
    fun embed(carrier: ByteArray, payload: ByteArray): ByteArray
    fun extract(carrier: ByteArray): ByteArray
    fun calculateCapacity(carrier: ByteArray): Long
    fun supportedType(): FileType
}
