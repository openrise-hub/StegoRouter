package io.openrise.stegorouter.ui.screen

import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Layout
import dev.tamboui.style.Color
import dev.tamboui.style.Style
import dev.tamboui.terminal.Frame
import dev.tamboui.text.Text
import dev.tamboui.tui.Keys
import dev.tamboui.tui.TuiRunner
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.tui.event.TickEvent
import dev.tamboui.widgets.gauge.Gauge
import dev.tamboui.widgets.paragraph.Paragraph
import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.archive.ZipCarrier
import io.openrise.stegorouter.carrier.audio.WavCarrier
import io.openrise.stegorouter.carrier.document.PdfCarrier
import io.openrise.stegorouter.carrier.document.SvgCarrier
import io.openrise.stegorouter.carrier.executable.ElfCarrier
import io.openrise.stegorouter.carrier.executable.ExeCarrier
import io.openrise.stegorouter.carrier.image.BmpCarrier
import io.openrise.stegorouter.carrier.image.PngCarrier
import io.openrise.stegorouter.payload.PayloadProcessor
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.Operation
import io.openrise.stegorouter.ui.OperationResult
import io.openrise.stegorouter.ui.ScreenType
import io.openrise.stegorouter.validation.MagicByteValidator
import java.io.File

class ProcessingScreen : Screen {
    private var progress = 0.0
    private var status = "Initializing..."
    private var processing = false
    private var startTime = 0L

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.length(3),
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Processing").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val statusWidget = Paragraph.builder()
            .text(Text.from(status).style(Style.DEFAULT.fg(Color.Yellow)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(statusWidget, chunks[1])

        val gauge = Gauge.builder()
            .ratio(progress)
            .label("${(progress * 100).toInt()}%")
            .gaugeStyle(Style.DEFAULT.fg(Color.Green))
            .build()
        frame.renderWidget(gauge, chunks[2])

        val details = Paragraph.builder()
            .text(Text.from("Operation: ${if (state.operation == Operation.EMBED) "Embed" else "Extract"}\nCarrier: ${state.carrierFile?.name ?: "N/A"}").style(Style.DEFAULT.fg(Color.White)))
            .build()
        frame.renderWidget(details, chunks[3])

        val help = Paragraph.builder()
            .text(Text.from("Please wait...").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[4])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event is TickEvent && !processing) {
            processing = true
            startTime = System.currentTimeMillis()
            val result = processOperation(state)
            return state.copy(
                lastResult = result,
                currentScreen = ScreenType.RESULTS
            )
        }
        return state
    }

    fun processOperation(state: AppState): OperationResult {
        return try {
            status = "Reading carrier file..."
            progress = 0.1
            val carrierData = state.carrierFile?.readBytes() ?: throw Exception("No carrier file")

            status = "Detecting file type..."
            progress = 0.2
            val fileType = MagicByteValidator.validate(carrierData)

            status = "Selecting algorithm..."
            progress = 0.3
            val algorithm = getAlgorithm(fileType)

            val result = if (state.operation == Operation.EMBED) {
                embed(algorithm, carrierData, state)
            } else {
                extract(algorithm, carrierData, state)
            }

            progress = 1.0
            result
        } catch (e: Exception) {
            OperationResult(
                success = false,
                message = "Error: ${e.message}",
                timeElapsed = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun embed(algorithm: CarrierAlgorithm, carrierData: ByteArray, state: AppState): OperationResult {
        status = "Preparing payload..."
        progress = 0.4

        val payload = state.payload ?: throw Exception("No payload")
        val password = state.password ?: throw Exception("No password")

        status = "Compressing and encrypting..."
        progress = 0.5
        val preparedPayload = PayloadProcessor.prepare(payload, password)

        status = "Embedding payload..."
        progress = 0.7
        val result = algorithm.embed(carrierData, preparedPayload)

        status = "Writing output file..."
        progress = 0.9
        state.outputFile?.writeBytes(result) ?: throw Exception("No output file")

        return OperationResult(
            success = true,
            message = "Payload embedded successfully",
            outputFile = state.outputFile,
            payloadSize = payload.size.toLong(),
            capacityUsed = preparedPayload.size.toLong(),
            timeElapsed = System.currentTimeMillis() - startTime
        )
    }

    private fun extract(algorithm: CarrierAlgorithm, carrierData: ByteArray, state: AppState): OperationResult {
        status = "Extracting payload..."
        progress = 0.5

        val password = state.password ?: throw Exception("No password")

        status = "Decrypting and decompressing..."
        progress = 0.7
        val extractedData = algorithm.extract(carrierData)
        val payload = PayloadProcessor.recover(extractedData, password)

        status = "Saving payload..."
        progress = 0.9
        val outputFile = File(state.outputDir, "extracted_payload.bin")
        outputFile.writeBytes(payload)

        return OperationResult(
            success = true,
            message = "Payload extracted successfully",
            outputFile = outputFile,
            payloadSize = payload.size.toLong(),
            timeElapsed = System.currentTimeMillis() - startTime
        )
    }

    private fun getAlgorithm(fileType: io.openrise.stegorouter.carrier.FileType): CarrierAlgorithm {
        return when (fileType) {
            io.openrise.stegorouter.carrier.FileType.PNG -> PngCarrier()
            io.openrise.stegorouter.carrier.FileType.BMP -> BmpCarrier()
            io.openrise.stegorouter.carrier.FileType.WAV -> WavCarrier()
            io.openrise.stegorouter.carrier.FileType.ZIP -> ZipCarrier()
            io.openrise.stegorouter.carrier.FileType.SVG -> SvgCarrier()
            io.openrise.stegorouter.carrier.FileType.PDF -> PdfCarrier()
            io.openrise.stegorouter.carrier.FileType.EXE -> ExeCarrier()
            io.openrise.stegorouter.carrier.FileType.ELF -> ElfCarrier()
        }
    }
}
