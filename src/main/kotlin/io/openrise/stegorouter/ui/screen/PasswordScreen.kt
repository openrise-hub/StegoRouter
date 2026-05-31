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
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Enter Password").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val config = ConfigManager.load()
        val hasDefault = config.defaultPassword != null
        val statusText = if (hasDefault) "Using default password (press Enter to use, or type new)" else "No default password set"
        val status = Paragraph.builder()
            .text(Text.from(statusText).style(Style.DEFAULT.fg(Color.Yellow)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(status, chunks[1])

        val passwordLabel = Paragraph.builder()
            .text(Text.from("Password:").style(Style.DEFAULT.fg(Color.White)))
            .build()
        frame.renderWidget(passwordLabel, chunks[2])

        val passwordInput = TextInput.builder()
            .placeholder("Enter password...")
            .password(true)
            .build()
        frame.renderStatefulWidget(passwordInput, chunks[3], if (confirmMode) confirmInputState else passwordInputState)

        val help = Paragraph.builder()
            .text(Text.from("Enter: Continue | Esc: Back").style(Style.DEFAULT.fg(Color.Gray)))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[4])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isEscape(event) -> {
                val prevScreen = if (state.operation == Operation.EMBED) {
                    ScreenType.PAYLOAD_INPUT
                } else {
                    ScreenType.FILE_SELECT
                }
                state.copy(currentScreen = prevScreen, password = null)
            }
            Keys.isSelect(event) -> {
                val password = passwordInputState.value
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
                        val confirm = confirmInputState.value
                        if (password == confirm) {
                            confirmMode = false
                            state.copy(
                                password = password,
                                currentScreen = ScreenType.OUTPUT_NAME
                            )
                        } else {
                            confirmMode = false
                            passwordInputState.setValue("")
                            confirmInputState.setValue("")
                            state
                        }
                    } else {
                        state.copy(
                            password = password,
                            currentScreen = ScreenType.PROCESSING
                        )
                    }
                } else {
                    state
                }
            }
            else -> state
        }
    }
}
