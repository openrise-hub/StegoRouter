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
import io.openrise.stegorouter.ui.PayloadSource
import io.openrise.stegorouter.ui.ScreenType
import java.awt.FileDialog
import java.awt.Frame as AwtFrame
import java.io.File

class PayloadInputScreen : Screen {
    private val textInputState = TextInputState()
    private var mode = PayloadSource.TEXT

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Enter Payload").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val modeText = if (mode == PayloadSource.TEXT) "Mode: Text Input" else "Mode: File"
        val modeWidget = Paragraph.builder()
            .text(Text.from(modeText).style(Style.DEFAULT.fg(Color.Yellow)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(modeWidget, chunks[1])

        val content = if (mode == PayloadSource.TEXT) {
            val input = TextInput.builder()
                .placeholder("Enter payload text...")
                .build()
            frame.renderStatefulWidget(input, chunks[2], textInputState)
            null
        } else {
            val message = if (state.payload != null) {
                "File selected: ${state.payload.size} bytes\n\nPress Enter to continue"
            } else {
                "Press Enter to select payload file"
            }
            Paragraph.builder()
                .text(Text.from(message).style(Style.DEFAULT.fg(Color.White)))
                .alignment(dev.tamboui.layout.Alignment.CENTER)
                .build()
        }

        if (content != null) {
            frame.renderWidget(content, chunks[2])
        }

        val help = Paragraph.builder()
            .text(Text.from("Tab: Switch Mode | Enter: Continue | Esc: Back").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[3])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isEscape(event) -> {
                state.copy(currentScreen = ScreenType.FILE_SELECT, payload = null)
            }
            Keys.isTab(event) -> {
                mode = if (mode == PayloadSource.TEXT) PayloadSource.FILE else PayloadSource.TEXT
                state
            }
            Keys.isSelect(event) -> {
                if (mode == PayloadSource.TEXT) {
                    val text = textInputState.value
                    if (text.isNotEmpty()) {
                        state.copy(
                            payload = text.toByteArray(),
                            payloadSource = PayloadSource.TEXT,
                            currentScreen = ScreenType.PASSWORD
                        )
                    } else {
                        state
                    }
                } else {
                    openFilePicker(state)
                }
            }
            else -> state
        }
    }

    private fun openFilePicker(state: AppState): AppState {
        val dialog = FileDialog(null as AwtFrame?, "Select Payload File", FileDialog.LOAD)
        dialog.isVisible = true

        val file = if (dialog.file != null) {
            File(dialog.directory, dialog.file)
        } else {
            null
        }

        return if (file != null && file.exists()) {
            state.copy(
                payload = file.readBytes(),
                payloadSource = PayloadSource.FILE,
                currentScreen = ScreenType.PASSWORD
            )
        } else {
            state
        }
    }
}
