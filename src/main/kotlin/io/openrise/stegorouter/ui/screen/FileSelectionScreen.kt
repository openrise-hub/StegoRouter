package io.openrise.stegorouter.ui.screen

import dev.tamboui.layout.Alignment
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Layout
import dev.tamboui.terminal.Frame
import dev.tamboui.text.Text
import dev.tamboui.tui.TuiRunner
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.input.TextInput
import dev.tamboui.widgets.input.TextInputState
import dev.tamboui.widgets.paragraph.Paragraph
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.FileDialogUtil
import io.openrise.stegorouter.ui.Operation
import io.openrise.stegorouter.ui.ScreenType
import java.io.File

class FileSelectionScreen : Screen {
    private val pathInputState = TextInputState()
    private var useTextInput = false

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Select Carrier File"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        if (state.carrierFile != null) {
            val message = "Selected: ${state.carrierFile.absolutePath}\n\nPress Enter to continue or Esc to select another file"
            val content = Paragraph.builder().text(Text.from(message)).build()
            frame.renderWidget(content, chunks[1])
        } else if (!useTextInput) {
            val message = "Press Enter to open file picker\nor press 't' to type path manually\nPress Esc to go back"
            val content = Paragraph.builder().text(Text.from(message)).build()
            frame.renderWidget(content, chunks[1])
        } else {
            val label = Paragraph.builder()
                .text(Text.from("Enter file path:"))
                .build()
            frame.renderWidget(label, chunks[1])

            val input = TextInput.builder()
                .placeholder("/path/to/file.png")
                .build()
            frame.renderStatefulWidget(input, chunks[1], pathInputState)
        }

        val help = Paragraph.builder()
            .text(Text.from("Enter: Select/Continue | Esc: Back | t: Type path"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[2])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isCancel() -> {
                state.copy(currentScreen = ScreenType.MAIN_MENU, carrierFile = null)
            }
            event.isChar('t') && state.carrierFile == null -> {
                useTextInput = true
                state
            }
            event.isSelect() -> {
                if (state.carrierFile != null) {
                    val nextScreen = if (state.operation == Operation.EMBED) {
                        ScreenType.PAYLOAD_INPUT
                    } else {
                        ScreenType.PASSWORD
                    }
                    useTextInput = false
                    state.copy(currentScreen = nextScreen)
                } else if (useTextInput) {
                    val path = pathInputState.text()
                    val file = File(path)
                    if (file.exists()) {
                        useTextInput = false
                        pathInputState.clear()
                        state.copy(carrierFile = file)
                    } else {
                        state
                    }
                } else {
                    val files = FileDialogUtil.chooseFile("Select Carrier File")
                    if (files != null && files.isNotEmpty()) {
                        state.copy(carrierFile = files.first())
                    } else {
                        state
                    }
                }
            }
            else -> state
        }
    }
}
