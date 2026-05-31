package io.openrise.stegorouter.ui.screen

import dev.tamboui.layout.Alignment
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Layout
import dev.tamboui.terminal.Frame
import dev.tamboui.text.Text
import dev.tamboui.tui.TuiRunner
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.list.ListItem
import dev.tamboui.widgets.list.ListState
import dev.tamboui.widgets.list.ListWidget
import dev.tamboui.widgets.paragraph.Paragraph
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

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("StegoRouter - Steganography Tool"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val list = ListWidget.builder()
            .items(*menuItems.toTypedArray())
            .highlightSymbol("> ")
            .build()
        frame.renderStatefulWidget(list, chunks[1], listState)

        val help = Paragraph.builder()
            .text(Text.from("Use arrows to navigate, Enter to select, q to quit"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[2])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isQuit() -> {
                runner.quit()
                state
            }
            event.isUp() -> {
                listState.selectPrevious()
                state
            }
            event.isDown() -> {
                listState.selectNext(menuItems.size)
                state
            }
            event.isSelect() -> {
                val selected = listState.selected()
                when (selected) {
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
