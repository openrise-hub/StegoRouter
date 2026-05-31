package io.openrise.stegorouter.ui

import java.io.File

data class AppState(
    val operation: Operation = Operation.NONE,
    val carrierFile: File? = null,
    val payload: ByteArray? = null,
    val payloadSource: PayloadSource = PayloadSource.NONE,
    val password: String? = null,
    val outputFile: File? = null,
    val outputDir: File = File("."),
    val batchQueue: MutableList<BatchItem> = mutableListOf(),
    val currentScreen: ScreenType = ScreenType.MAIN_MENU,
    val lastResult: OperationResult? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other) return false

        other as AppState

        if (operation != other.operation) return false
        if (carrierFile != other.carrierFile) return false
        if (payload != null) {
            if (other.payload == null) return false
            if (!payload.contentEquals(other.payload)) return false
        } else if (other.payload != null) return false
        if (payloadSource != other.payloadSource) return false
        if (password != other.password) return false
        if (outputFile != other.outputFile) return false
        if (outputDir != other.outputDir) return false
        if (batchQueue != other.batchQueue) return false
        if (currentScreen != other.currentScreen) return false
        if (lastResult != other.lastResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operation.hashCode()
        result = 31 * result + (carrierFile?.hashCode() ?: 0)
        result = 31 * result + (payload?.contentHashCode() ?: 0)
        result = 31 * result + payloadSource.hashCode()
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (outputFile?.hashCode() ?: 0)
        result = 31 * result + outputDir.hashCode()
        result = 31 * result + batchQueue.hashCode()
        result = 31 * result + currentScreen.hashCode()
        result = 31 * result + (lastResult?.hashCode() ?: 0)
        return result
    }
}

enum class Operation { EMBED, EXTRACT, NONE }

enum class PayloadSource { TEXT, FILE, NONE }

enum class ScreenType {
    MAIN_MENU,
    FILE_SELECT,
    PAYLOAD_INPUT,
    PASSWORD,
    OUTPUT_NAME,
    PROCESSING,
    RESULTS,
    BATCH_QUEUE,
    SETTINGS
}

data class BatchItem(
    val carrierFile: File,
    val operation: Operation,
    var status: BatchStatus = BatchStatus.PENDING,
    var result: OperationResult? = null
)

enum class BatchStatus { PENDING, PROCESSING, COMPLETE, FAILED }

data class OperationResult(
    val success: Boolean,
    val message: String,
    val outputFile: File? = null,
    val payloadSize: Long = 0,
    val capacityUsed: Long = 0,
    val timeElapsed: Long = 0
)
