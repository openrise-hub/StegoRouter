package io.openrise.stegorouter.ui.screen

import dev.tamboui.layout.Alignment
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Layout
import dev.tamboui.terminal.Frame
import dev.tamboui.text.Text
import dev.tamboui.tui.TuiRunner
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.list.ListState
import dev.tamboui.widgets.list.ListWidget
import dev.tamboui.widgets.paragraph.Paragraph
import io.openrise.stegorouter.ui.AppState
import io.openrise.stegorouter.ui.BatchItem
import io.openrise.stegorouter.ui.BatchStatus
import io.openrise.stegorouter.ui.ScreenType
import java.awt.FileDialog
import java.awt.Frame as AwtFrame
import java.io.File

class BatchQueueScreen : Screen {
    private val listState = ListState()

    override fun render(frame: Frame, state: AppState) {
        val chunks = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.min(0),
                Constraint.length(3)
            )
            .split(frame.area())

        val title = Paragraph.builder()
            .text(Text.from("Batch Queue"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(title, chunks[0])

        val items = if (state.batchQueue.isEmpty()) {
            arrayOf("No items in queue - Press 'a' to add files")
        } else {
            state.batchQueue.map { item ->
                val statusIcon = when (item.status) {
                    BatchStatus.PENDING -> "[ ]"
                    BatchStatus.PROCESSING -> "[~]"
                    BatchStatus.COMPLETE -> "[+]"
                    BatchStatus.FAILED -> "[X]"
                }
                "$statusIcon ${item.carrierFile.name} (${item.operation})"
            }.toTypedArray()
        }

        val list = ListWidget.builder()
            .items(*items)
            .highlightSymbol("> ")
            .build()
        frame.renderStatefulWidget(list, chunks[1], listState)

        val help = Paragraph.builder()
            .text(Text.from("a: Add | Enter: Process | Esc: Back"))
            .alignment(Alignment.CENTER)
            .build()
        frame.renderWidget(help, chunks[2])
    }

    override fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState {
        if (event !is KeyEvent) return state

        return when {
            event.isCancel() -> {
                state.copy(currentScreen = ScreenType.MAIN_MENU)
            }
            event.isChar('a') -> {
                addFilesToQueue(state)
            }
            event.isSelect() -> {
                if (state.batchQueue.isNotEmpty()) {
                    state.copy(currentScreen = ScreenType.PROCESSING)
                } else {
                    state
                }
            }
            else -> state
        }
    }

    private fun addFilesToQueue(state: AppState): AppState {
        val dialog = FileDialog(null as AwtFrame?, "Select Carrier Files", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.isVisible = true

        val files = dialog.files
        if (files != null && files.isNotEmpty()) {
            val newItems = files.map { file ->
                BatchItem(
                    carrierFile = file,
                    operation = io.openrise.stegorouter.ui.Operation.EMBED,
                    status = BatchStatus.PENDING
                )
            }
            val newQueue = state.batchQueue.toMutableList()
            newQueue.addAll(newItems)
            return state.copy(batchQueue = newQueue)
        }
        return state
    }
}
