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
import dev.tamboui.widgets.textinput.TextInput
import dev.tamboui.widgets.textinput.TextInputState
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.ScreenType
import java.io.File

class OutputNameScreen : Screen {
    private val outputInputState = TextInputState()

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
            .text(Text.from("Output Filename").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val carrierName = state.carrierFile?.nameWithoutExtension ?: "output"
        val suggestion = "${carrierName}_stego.${state.carrierFile?.extension ?: "bin"}"
        val info = Paragraph.builder()
            .text(Text.from("Suggested: $suggestion").style(Style.DEFAULT.fg(Color.Yellow)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(info, chunks[1])

        val label = Paragraph.builder()
            .text(Text.from("Output filename:").style(Style.DEFAULT.fg(Color.White)))
            .build()
        frame.renderWidget(label, chunks[2])

        val input = TextInput.builder()
            .placeholder(suggestion)
            .build()
        frame.renderStatefulWidget(input, chunks[3], outputInputState)

        val help = Paragraph.builder()
            .text(Text.from("Enter: Continue (empty for suggested) | Esc: Back").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[4])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isEscape(event) -> {
                state.copy(currentScreen = ScreenType.PASSWORD, outputFile = null)
            }
            Keys.isSelect(event) -> {
                val input = outputInputState.value
                val carrierName = state.carrierFile?.nameWithoutExtension ?: "output"
                val extension = state.carrierFile?.extension ?: "bin"
                val suggestion = "${carrierName}_stego.$extension"

                val filename = if (input.isEmpty()) suggestion else input
                val outputDir = state.outputDir
                val outputFile = File(outputDir, filename)

                state.copy(
                    outputFile = outputFile,
                    currentScreen = ScreenType.PROCESSING
                )
            }
            else -> state
        }
    }
}
