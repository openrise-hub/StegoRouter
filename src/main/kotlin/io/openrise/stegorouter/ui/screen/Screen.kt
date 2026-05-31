package io.openrise.stegorouter.ui.screen

import dev.tamboui.terminal.Frame
import dev.tamboui.tui.TuiRunner
import io.openrise.stegorouter.ui.AppState

interface Screen {
    fun render(frame: Frame, state: AppState)
    fun handleEvent(event: Any, runner: TuiRunner, state: AppState): AppState
}
