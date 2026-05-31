package io.openrise.stegorouter.ui

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.openrise.stegorouter.config.AppConfig
import java.io.File

object ConfigManager {
    private val configFile = File(System.getProperty("user.home"), ".stegorouter.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun load(): AppConfig {
        return try {
            if (configFile.exists()) {
                gson.fromJson(configFile.readText(), AppConfig::class.java) ?: AppConfig()
            } else {
                AppConfig()
            }
        } catch (e: Exception) {
            AppConfig()
        }
    }

    fun save(config: AppConfig) {
        try {
            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            System.err.println("Failed to save configuration: ${e.message}")
        }
    }
}
