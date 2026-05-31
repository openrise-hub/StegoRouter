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
import io.openrise.stegorouter.ui.ConfigManager
import io.openrise.stegorouter.ui.Operation
import io.openrise.stegorouter.ui.ScreenType

class PasswordScreen : Screen {
    private val passwordInputState = TextInputState()
    private val confirmInputState = TextInputState()
    private var confirmMode = false

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
            .text(Text.from("Enter Password"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val config = ConfigManager.load()
        val hasDefault = config.defaultPassword != null
        val statusText = if (hasDefault) "Using default password (Enter to use, or type new)" else "No default password set"
        val status = Paragraph.builder()
            .text(Text.from(statusText))
            .build()
        frame.renderWidget(status, chunks[1])

        val label = Paragraph.builder()
            .text(Text.from("Password:"))
            .build()
        frame.renderWidget(label, chunks[1])

        val input = TextInput.builder()
            .placeholder("Enter password...")
            .masked()
            .build()
        frame.renderStatefulWidget(input, chunks[2], if (confirmMode) confirmInputState else passwordInputState)

        val help = Paragraph.builder()
            .text(Text.from("Enter: Continue | Esc: Back"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[3])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isCancel() -> {
                val prevScreen = if (state.operation == Operation.EMBED) {
                    ScreenType.PAYLOAD_INPUT
                } else {
                    ScreenType.FILE_SELECT
                }
                state.copy(currentScreen = prevScreen, password = null)
            }
            event.isSelect() -> {
                val password = passwordInputState.text()
                val config = ConfigManager.load()

                if (password.isEmpty() && config.defaultPassword != null) {
                    state.copy(
                        password = config.defaultPassword,
                        currentScreen = if (state.operation == Operation.EMBED) ScreenType.OUTPUT_NAME else ScreenType.PROCESSING
                    )
                } else if (password.isNotEmpty()) {
                    if (state.operation == Operation.EMBED && !confirmMode) {
                        confirmMode = true
                        state
                    } else if (state.operation == Operation.EMBED && confirmMode) {
                        val confirm = confirmInputState.text()
                        if (password == confirm) {
                            confirmMode = false
                            state.copy(password = password, currentScreen = ScreenType.OUTPUT_NAME)
                        } else {
                            confirmMode = false
                            passwordInputState.clear()
                            confirmInputState.clear()
                            state
                        }
                    } else {
                        state.copy(password = password, currentScreen = ScreenType.PROCESSING)
                    }
                } else {
                    state
                }
            }
            else -> state
        }
    }
}
