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
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.Operation
import io.openrise.stegorouter.ui.ScreenType

class MainMenuScreen : Screen {
    private val listState = ListState()
    private val menuItems = listOf(
        "Embed payload into carrier",
        "Extract payload from carrier",
        "Batch operations",
        "Settings",
        "Quit"
    )

    init {
        listState.select(0)
    }

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = dev.tamboui.widgets.paragraph.Paragraph.builder()
            .text(Text.from("StegoRouter - Steganography Tool").style(Style.DEFAULT.fg(Color.Cyan).bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val list = List.builder()
            .items(menuItems.map { Text.from(it) })
            .highlightStyle(Style.DEFAULT.fg(Color.Yellow).bold())
            .highlightSymbol("> ")
            .build()
        frame.renderStatefulWidget(list, chunks[1], listState)

        val help = dev.tamboui.widgets.paragraph.Paragraph.builder()
            .text(Text.from("Use ↑↓ to navigate, Enter to select, q to quit").style(Style.DEFAULT.fg(Color.Gray)))
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
            Keys.isUp(event) -> {
                val current = listState.selected ?: 0
                val newIndex = if (current > 0) current - 1 else menuItems.size - 1
                listState.select(newIndex)
                state
            }
            Keys.isDown(event) -> {
                val current = listState.selected ?: 0
                val newIndex = if (current < menuItems.size - 1) current + 1 else 0
                listState.select(newIndex)
                state
            }
            Keys.isSelect(event) -> {
                when (listState.selected) {
                    0 -> state.copy(operation = Operation.EMBED, currentScreen = ScreenType.FILE_SELECT)
                    1 -> state.copy(operation = Operation.EXTRACT, currentScreen = ScreenType.FILE_SELECT)
                    2 -> state.copy(currentScreen = ScreenType.BATCH_QUEUE)
                    3 -> state.copy(currentScreen = ScreenType.SETTINGS)
                    4 -> {
                        runner.quit()
                        state
                    }
                    else -> state
                }
            }
            else -> state
        }
    }
}
