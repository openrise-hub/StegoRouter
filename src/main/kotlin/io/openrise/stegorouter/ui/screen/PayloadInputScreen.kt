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
import io.openrise.stegorouter.ui.PayloadSource
import io.openrise.stegorouter.ui.ScreenType
import java.io.File

class PayloadInputScreen : Screen {
    private val textInputState = TextInputState()
    private val pathInputState = TextInputState()
    private var mode = PayloadSource.TEXT
    private var useTextPath = false

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
            .text(Text.from("Enter Payload"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val modeText = if (mode == PayloadSource.TEXT) "Mode: Text Input" else "Mode: File"
        val modeWidget = Paragraph.builder()
            .text(Text.from(modeText))
            .build()
        frame.renderWidget(modeWidget, chunks[1])

        if (mode == PayloadSource.TEXT) {
            val input = TextInput.builder()
                .placeholder("Enter payload text...")
                .build()
            frame.renderStatefulWidget(input, chunks[2], textInputState)
        } else if (state.payload != null) {
            val message = "File selected: ${state.payload.size} bytes\n\nPress Enter to continue"
            val content = Paragraph.builder().text(Text.from(message)).build()
            frame.renderWidget(content, chunks[2])
        } else {
            val input = TextInput.builder()
                .placeholder("Enter file path or press Enter to browse...")
                .build()
            frame.renderStatefulWidget(input, chunks[2], pathInputState)
        }

        val help = Paragraph.builder()
            .text(Text.from("Tab: Switch Mode | Enter: Continue | Esc: Back"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[3])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isCancel() -> {
                state.copy(currentScreen = ScreenType.FILE_SELECT, payload = null)
            }
            event.isFocusNext() -> {
                mode = if (mode == PayloadSource.TEXT) PayloadSource.FILE else PayloadSource.TEXT
                state
            }
            event.isSelect() -> {
                if (mode == PayloadSource.TEXT) {
                    val text = textInputState.text()
                    if (text.isNotEmpty()) {
                        state.copy(
                            payload = text.toByteArray(),
                            payloadSource = PayloadSource.TEXT,
                            currentScreen = ScreenType.PASSWORD
                        )
                    } else {
                        state
                    }
                } else if (state.payload != null) {
                    state.copy(payloadSource = PayloadSource.FILE, currentScreen = ScreenType.PASSWORD)
                } else {
                    val path = pathInputState.text()
                    if (path.isNotEmpty()) {
                        val file = File(path)
                        if (file.exists()) {
                            state.copy(
                                payload = file.readBytes(),
                                payloadSource = PayloadSource.FILE,
                                currentScreen = ScreenType.PASSWORD
                            )
                        } else {
                            state
                        }
                    } else {
                        val files = FileDialogUtil.chooseFile("Select Payload File")
                        if (files != null && files.isNotEmpty()) {
                            state.copy(
                                payload = files.first().readBytes(),
                                payloadSource = PayloadSource.FILE,
                                currentScreen = ScreenType.PASSWORD
                            )
                        } else {
                            state
                        }
                    }
                }
            }
            else -> state
        }
    }
}
