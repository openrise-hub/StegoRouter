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
import dev.tamboui.widgets.list.List
import dev.tamboui.widgets.list.ListState
import dev.tamboui.widgets.paragraph.Paragraph
import dev.tamboui.widgets.textinput.TextInput
import dev.tamboui.widgets.textinput.TextInputState
import io.openrise.stegorouter.config.AppConfig
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.ConfigManager
import io.openrise.stegorouter.ui.ScreenType

class SettingsScreen : Screen {
    private val listState = ListState()
    private val inputState = TextInputState()
    private var editingIndex: Int? = null
    private val settings = listOf("Default Password", "Default Output Directory")

    init {
        listState.select(0)
    }

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Settings").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val config = ConfigManager.load()
        val items = listOf(
            Text.from("Default Password: ${if (config.defaultPassword != null) "********" else "(not set)"}"),
            Text.from("Default Output Directory: ${config.defaultOutputDir}")
        )

        val list = List.builder()
            .items(items)
            .highlightStyle(Style.DEFAULT.fg(Color.Yellow).bold())
            .highlightSymbol("> ")
            .build()
        frame.renderStatefulWidget(list, chunks[1], listState)

        if (editingIndex != null) {
            val label = if (editingIndex == 0) "Enter new password:" else "Enter output directory:"
            val inputLabel = Paragraph.builder()
                .text(Text.from(label).style(Style.DEFAULT.fg(Color.White)))
                .build()
            frame.renderWidget(inputLabel, chunks[2])

            val input = TextInput.builder()
                .placeholder("Enter value...")
                .password(editingIndex == 0)
                .build()
            frame.renderStatefulWidget(input, chunks[3], inputState)
        } else {
            val help = Paragraph.builder()
                .text(Text.from("Enter: Edit | Esc: Back").style(Style.DEFAULT.fg(Color.Gray)))
                .alignment(dev.tamboui.layout.Alignment.CENTER)
                .build()
            frame.renderWidget(help, chunks[2])

            val spacer = Paragraph.builder()
                .text(Text.from(""))
                .build()
            frame.renderWidget(spacer, chunks[3])
        }
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            Keys.isEscape(event) -> {
                if (editingIndex != null) {
                    editingIndex = null
                    inputState.setValue("")
                    state
                } else {
                    state.copy(currentScreen = ScreenType.MAIN_MENU)
                }
            }
            Keys.isUp(event) && editingIndex == null -> {
                val current = listState.selected ?: 0
                val newIndex = if (current > 0) current - 1 else settings.size - 1
                listState.select(newIndex)
                state
            }
            Keys.isDown(event) && editingIndex == null -> {
                val current = listState.selected ?: 0
                val newIndex = if (current < settings.size - 1) current + 1 else 0
                listState.select(newIndex)
                state
            }
            Keys.isSelect(event) -> {
                if (editingIndex != null) {
                    saveSetting(state)
                } else {
                    editingIndex = listState.selected
                    state
                }
            }
            else -> state
        }
    }

    private fun saveSetting(state: AppState): AppState {
        val config = ConfigManager.load()
        val value = inputState.value

        val newConfig = when (editingIndex) {
            0 -> config.copy(defaultPassword = if (value.isNotEmpty()) value else null)
            1 -> config.copy(defaultOutputDir = if (value.isNotEmpty()) value else ".")
            else -> config
        }

        ConfigManager.save(newConfig)
        editingIndex = null
        inputState.setValue("")
        return state
    }
}
