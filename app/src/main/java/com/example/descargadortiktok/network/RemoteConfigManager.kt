package com.example.descargadortiktok.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class AppConfig(
    val primaryApi: String = "https://www.tikwm.com/",
    val fallbackApi1: String = "https://tdownv4.sl-bjs.workers.dev/",
    val fallbackApi2: String = "https://api.douyin.wtf/",
    val slideshowDuration: Double = 4.0,
    val minImagesForSlideshow: Int = 3
)

object RemoteConfigManager {
    // URL del archivo JSON en tu repositorio de GitHub (versión RAW)
    private const val CONFIG_URL = "https://raw.githubusercontent.com/Tesla1234356/actualizacion_descargadortiktokapi/main/tiktok_config.json"
    
    var config = AppConfig()
        private set

    private val client = OkHttpClient()

    suspend fun fetchConfig() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(CONFIG_URL).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                if (!json.isNullOrBlank()) {
                    config = Gson().fromJson(json, AppConfig::class.java)
                    println("DEBUG_CONFIG: Configuración remota cargada con éxito")
                }
            }
        } catch (e: Exception) {
            println("DEBUG_CONFIG: Error al cargar config remota, usando valores por defecto: ${e.message}")
        }
    }
}