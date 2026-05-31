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
import dev.tamboui.widgets.paragraph.Paragraph
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.ScreenType

class ResultsScreen : Screen {
    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val result = state.lastResult
        val titleText = if (result?.success == true) "Success" else "Failed"
        val titleStyle = if (result?.success == true) {
            Style.DEFAULT.fg(Color.Green).bold()
        } else {
            Style.DEFAULT.fg(Color.Red).bold()
        }

        val title = Paragraph.builder()
            .text(Text.from(titleText).style(titleStyle))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val details = buildString {
            appendLine(result?.message ?: "No result")
            appendLine()
            if (result?.outputFile != null) {
                appendLine("Output: ${result.outputFile.absolutePath}")
            }
            if (result?.payloadSize != null && result.payloadSize > 0) {
                appendLine("Payload size: ${result.payloadSize} bytes")
            }
            if (result?.capacityUsed != null && result.capacityUsed > 0) {
                appendLine("Capacity used: ${result.capacityUsed} bytes")
            }
            if (result?.timeElapsed != null) {
                appendLine("Time: ${result.timeElapsed}ms")
            }
        }

        val content = Paragraph.builder()
            .text(Text.from(details).style(Style.DEFAULT.fg(Color.White)))
            .build()
        frame.renderWidget(content, chunks[1])

        val help = Paragraph.builder()
            .text(Text.from("Enter: Main Menu | q: Quit").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[2])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isQuit(event) -> {
                runner.quit()
                state
            }
            Keys.isSelect(event) -> {
                state.copy(
                    currentScreen = ScreenType.MAIN_MENU,
                    carrierFile = null,
                    payload = null,
                    password = null,
                    outputFile = null,
                    lastResult = null
                )
            }
            else -> state
        }
    }
}
