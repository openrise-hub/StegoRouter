package io.openrise.stegorouter.carrier.executable

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType

class ExeCarrier : CarrierAlgorithm {

    private val EXE_MAGIC = byteArrayOf(0x4D, 0x5A)
    private val STEGO_MAGIC = byteArrayOf(0x53, 0x54, 0x47, 0x52)

    override fun embed(carrier: ByteArray, payload: ByteArray): ByteArray {
        require(carrier.size >= 2) { "Invalid EXE file" }
        require(carrier.copyOfRange(0, 2).contentEquals(EXE_MAGIC)) { "Invalid EXE signature" }

        val result = ByteArray(carrier.size + payload.size + 8)
        System.arraycopy(carrier, 0, result, 0, carrier.size)
        System.arraycopy(payload, 0, result, carrier.size, payload.size)

        writeIntLE(result, carrier.size + payload.size, payload.size)
        System.arraycopy(STEGO_MAGIC, 0, result, carrier.size + payload.size + 4, 4)

        return result
    }

    override fun extract(carrier: ByteArray): ByteArray {
        require(carrier.size >= 2) { "Invalid EXE file" }
        require(carrier.copyOfRange(0, 2).contentEquals(EXE_MAGIC)) { "Invalid EXE signature" }
        require(carrier.size >= 8) { "File too small to contain payload" }

        val magicOffset = carrier.size - 4
        if (!carrier.copyOfRange(magicOffset, magicOffset + 4).contentEquals(STEGO_MAGIC)) {
            return ByteArray(0)
        }

        val payloadLength = readIntLE(carrier, carrier.size - 8)
        require(payloadLength >= 0 && payloadLength <= carrier.size - 8) {
            "Invalid payload length"
        }

        val payloadStart = carrier.size - 8 - payloadLength
        return carrier.copyOfRange(payloadStart, payloadStart + payloadLength)
    }

    override fun calculateCapacity(carrier: ByteArray): Long {
        require(carrier.size >= 2) { "Invalid EXE file" }
        require(carrier.copyOfRange(0, 2).contentEquals(EXE_MAGIC)) { "Invalid EXE signature" }

        return Int.MAX_VALUE.toLong()
    }

    override fun supportedType(): FileType = FileType.EXE

    private fun readIntLE(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun writeIntLE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value shr 8).toByte()
        data[offset + 2] = (value shr 16).toByte()
        data[offset + 3] = (value shr 24).toByte()
    }
}
