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
import dev.tamboui.widgets.list.ListState
import dev.tamboui.widgets.list.ListWidget
import dev.tamboui.widgets.paragraph.Paragraph
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.ConfigManager
import io.openrise.stegorouter.ui.ScreenType

class SettingsScreen : Screen {
    private val listState = ListState()
    private val inputState = TextInputState()
    private var editingIndex: Int? = null
    private val settings = listOf("Default Password", "Default Output Directory")

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Settings"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val config = ConfigManager.load()
        val items = arrayOf(
            "Default Password: ${if (config.defaultPassword != null) "********" else "(not set)"}",
            "Default Output Directory: ${config.defaultOutputDir}"
        )

        val list = ListWidget.builder()
            .items(*items)
            .highlightSymbol("> ")
            .build()
        frame.renderStatefulWidget(list, chunks[1], listState)

        if (editingIndex != null) {
            val editChunks = Layout.vertical()
                .constraints(
                    Constraint.length(3),
                    Constraint.min(0)
                )
                .split(chunks[2])

            val label = if (editingIndex == 0) "Enter new password:" else "Enter output directory:"
            val inputLabel = Paragraph.builder()
                .text(Text.from(label))
                .build()
            frame.renderWidget(inputLabel, editChunks[0])

            val inputBuilder = TextInput.builder()
                .placeholder("Enter value...")
            val input = if (editingIndex == 0) inputBuilder.masked().build() else inputBuilder.build()
            frame.renderStatefulWidget(input, editChunks[1], inputState)
        } else {
            val help = Paragraph.builder()
                .text(Text.from("Enter: Edit | Esc: Back"))
                .alignment(Alignment.CENTER)
                .build()
            frame.renderWidget(help, chunks[2])
        }
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isCancel() -> {
                if (editingIndex != null) {
                    editingIndex = null
                    inputState.clear()
                    state
                } else {
                    state.copy(currentScreen = ScreenType.MAIN_MENU)
                }
            }
            event.isUp() && editingIndex == null -> {
                listState.selectPrevious()
                state
            }
            event.isDown() && editingIndex == null -> {
                listState.selectNext(settings.size)
                state
            }
            event.isSelect() -> {
                if (editingIndex != null) {
                    saveSetting(state)
                } else {
                    editingIndex = listState.selected()
                    state
                }
            }
            else -> state
        }
    }

    private fun saveSetting(state: AppState): AppState {
        val config = ConfigManager.load()
        val value = inputState.text()

        val newConfig = when (editingIndex) {
            0 -> config.copy(defaultPassword = if (value.isNotEmpty()) value else null)
            1 -> config.copy(defaultOutputDir = if (value.isNotEmpty()) value else ".")
            else -> config
        }

        ConfigManager.save(newConfig)
        editingIndex = null
        inputState.clear()
        return state
    }
}
