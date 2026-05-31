package io.openrise.stegorouter.ui.theme

import dev.tamboui.style.Color
import dev.tamboui.style.Style

object StegoRouterTheme {
    val primary = Style.DEFAULT.fg(Color.Cyan).bold()
    val secondary = Style.DEFAULT.fg(Color.Blue)
    val success = Style.DEFAULT.fg(Color.Green).bold()
    val error = Style.DEFAULT.fg(Color.Red).bold()
    val warning = Style.DEFAULT.fg(Color.Yellow)
    val dim = Style.DEFAULT.fg(Color.Gray)
    val highlight = Style.DEFAULT.fg(Color.Yellow).bold()
    val title = Style.DEFAULT.fg(Color.Cyan).bold()
    val border = Style.DEFAULT.fg(Color.DarkGray)
}
