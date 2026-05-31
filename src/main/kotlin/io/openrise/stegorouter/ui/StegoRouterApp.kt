package io.openrise.stegorouter.ui

import dev.tamboui.tui.TuiConfig
import dev.tamboui.tui.TuiRunner
import io.openrise.stegorouter.ui.screen.*
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.time.Duration
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "stegorouter",
    mixinStandardHelpOptions = true,
    version = ["StegoRouter 1.0"],
    description = ["Steganography tool for embedding and extracting payloads in various file formats"]
)
class StegoRouterApp : Callable<Int> {

    @Option(names = ["-e", "--embed"], description = ["Embed payload into carrier"])
    var embedMode: Boolean = false

    @Option(names = ["-x", "--extract"], description = ["Extract payload from carrier"])
    var extractMode: Boolean = false

    @Option(names = ["-f", "--file"], description = ["Carrier file"])
    var carrierFile: File? = null

    @Option(names = ["-p", "--payload"], description = ["Payload file or text"])
    var payload: String? = null

    @Option(names = ["--password"], description = ["Encryption password"])
    var password: String? = null

    @Option(names = ["-o", "--output"], description = ["Output file"])
    var outputFile: File? = null

    @Option(names = ["--batch"], description = ["Batch process multiple files"])
    var batchFiles: List<File>? = null

    override fun call(): Int {
        return if (hasCliArgs()) {
            runHeadless()
        } else {
            launchTui()
        }
    }

    private fun hasCliArgs(): Boolean {
        return embedMode || extractMode || carrierFile != null || payload != null || password != null || outputFile != null || batchFiles != null
    }

    private fun runHeadless(): Int {
        println("Running in headless mode...")
        
        if (carrierFile == null) {
            System.err.println("Error: Carrier file required (-f/--file)")
            return 1
        }

        if (!carrierFile!!.exists()) {
            System.err.println("Error: Carrier file not found: ${carrierFile!!.absolutePath}")
            return 1
        }

        val operation = when {
            embedMode -> Operation.EMBED
            extractMode -> Operation.EXTRACT
            else -> {
                System.err.println("Error: Must specify --embed or --extract")
                return 1
            }
        }

        if (operation == Operation.EMBED && payload == null) {
            System.err.println("Error: Payload required for embed operation (-p/--payload)")
            return 1
        }

        var pw = password
        if (pw == null) {
            val config = ConfigManager.load()
            pw = config.defaultPassword
            if (pw == null) {
                System.err.println("Error: Password required (--password or set default in TUI)")
                return 1
            }
        }

        val payloadData = if (operation == Operation.EMBED) {
            val payloadFile = File(payload!!)
            if (payloadFile.exists()) {
                payloadFile.readBytes()
            } else {
                payload!!.toByteArray()
            }
        } else {
            null
        }

        val output = outputFile ?: File(carrierFile!!.parent, "${carrierFile!!.nameWithoutExtension}_stego.${carrierFile!!.extension}")

        val state = AppState(
            operation = operation,
            carrierFile = carrierFile,
            payload = payloadData,
            payloadSource = if (payloadData != null) PayloadSource.FILE else PayloadSource.NONE,
            password = pw,
            outputFile = output,
            currentScreen = ScreenType.PROCESSING
        )

        val processingScreen = ProcessingScreen()
        val result = processingScreen.processOperation(state)

        if (result.success) {
            println("Success: ${result.message}")
            println("Output: ${result.outputFile?.absolutePath}")
            println("Payload size: ${result.payloadSize} bytes")
            println("Time: ${result.timeElapsed}ms")
            return 0
        } else {
            System.err.println("Error: ${result.message}")
            return 1
        }
    }

    private fun launchTui(): Int {
        val config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(250))
            .build()

        var appState = AppState()
        val screens = mapOf(
            ScreenType.MAIN_MENU to MainMenuScreen(),
            ScreenType.FILE_SELECT to FileSelectionScreen(),
            ScreenType.PAYLOAD_INPUT to PayloadInputScreen(),
            ScreenType.PASSWORD to PasswordScreen(),
            ScreenType.OUTPUT_NAME to OutputNameScreen(),
            ScreenType.PROCESSING to ProcessingScreen(),
            ScreenType.RESULTS to ResultsScreen(),
            ScreenType.BATCH_QUEUE to BatchQueueScreen(),
            ScreenType.SETTINGS to SettingsScreen()
        )

        TuiRunner.create(config).use { runner ->
            runner.run(
                { event, _ ->
                    val currentScreen = screens[appState.currentScreen]
                    if (currentScreen != null) {
                        appState = currentScreen.handleEvent(event, runner, appState)
                        true
                    } else {
                        false
                    }
                },
                { frame ->
                    val currentScreen = screens[appState.currentScreen]
                    currentScreen?.render(frame, appState)
                }
            )
        }

        return 0
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(StegoRouterApp()).execute(*args)
    exitProcess(exitCode)
}
