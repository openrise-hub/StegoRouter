package io.openrise.stegorouter.config

data class AppConfig(
    val defaultPassword: String? = null,
    val defaultOutputDir: String = ".",
    val lastUsedDir: String = "."
)
