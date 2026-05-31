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
import io.openrise.stegorouter.ui.Operation
import io.openrise.stegorouter.ui.ScreenType
import java.awt.FileDialog
import java.awt.Frame as AwtFrame
import java.io.File

class FileSelectionScreen : Screen {
    private var filePickerOpened = false

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Select Carrier File").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val message = if (state.carrierFile != null) {
            "Selected: ${state.carrierFile.absolutePath}\n\nPress Enter to continue or Esc to select another file"
        } else {
            "Press Enter to open file picker\nPress Esc to go back"
        }

        val content = Paragraph.builder()
            .text(Text.from(message).style(Style.DEFAULT.fg(Color.White)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(content, chunks[1])

        val help = Paragraph.builder()
            .text(Text.from("Enter: Select/Continue | Esc: Back").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[2])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isEscape(event) -> {
                state.copy(currentScreen = ScreenType.MAIN_MENU, carrierFile = null)
            }
            Keys.isSelect(event) -> {
                if (state.carrierFile != null) {
                    val nextScreen = if (state.operation == Operation.EMBED) {
                        ScreenType.PAYLOAD_INPUT
                    } else {
                        ScreenType.PASSWORD
                    }
                    state.copy(currentScreen = nextScreen)
                } else {
                    openFilePicker(state)
                }
            }
            else -> state
        }
    }

    private fun openFilePicker(state: AppState): AppState {
        val dialog = FileDialog(null as AwtFrame?, "Select Carrier File", FileDialog.LOAD)
        dialog.isVisible = true

        val file = if (dialog.file != null) {
            File(dialog.directory, dialog.file)
        } else {
            null
        }

        return if (file != null && file.exists()) {
            state.copy(carrierFile = file)
        } else {
            state
        }
    }
}
