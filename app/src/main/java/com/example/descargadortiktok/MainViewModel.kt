package com.example.descargadortiktok

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.descargadortiktok.downloader.DownloadType
import com.example.descargadortiktok.downloader.VideoDownloader
import com.example.descargadortiktok.network.RemoteConfigManager
import com.example.descargadortiktok.network.RetrofitClient
import com.example.descargadortiktok.network.models.VideoData
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class DownloadState {
    object Idle : DownloadState()
    object LoadingInfo : DownloadState()
    data class InfoLoaded(val videoData: VideoData) : DownloadState()
    data class DownloadingStart(val type: DownloadType) : DownloadState()
    data class Downloading(val progress: Float, val type: DownloadType) : DownloadState()
    data class Success(val message: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var tiktokUrl by mutableStateOf("")
    var downloadState by mutableStateOf<DownloadState>(DownloadState.Idle)
    var progress by mutableFloatStateOf(0f)
    var currentVideoData by mutableStateOf<VideoData?>(null)

    private val downloader = VideoDownloader(application)

    init {
        // Al iniciar el ViewModel, intentamos bajar la configuración más reciente
        viewModelScope.launch {
            RemoteConfigManager.fetchConfig()
        }
    }

    fun fetchVideoInfo() {
        if (tiktokUrl.isBlank()) {
            downloadState = DownloadState.Error("Por favor, ingresa un enlace válido")
            return
        }

        viewModelScope.launch {
            downloadState = DownloadState.LoadingInfo
            // Actualizamos config justo antes de analizar
            RemoteConfigManager.fetchConfig()
            
            try {
                // Intento 1: API Principal (Dinámica desde Config)
                val response = RetrofitClient.getApi().getVideoInfo(tiktokUrl.trim())
                val body = response.body()
                if (response.isSuccessful && body != null && body.code == 0 && body.data != null) {
                    currentVideoData = body.data
                    downloadState = DownloadState.InfoLoaded(body.data)
                } else {
                    tryFallbackInfo(1)
                }
            } catch (e: Exception) {
                tryFallbackInfo(1)
            }
        }
    }

    private suspend fun tryFallbackInfo(attempt: Int) {
        try {
            val encodedUrl = URLEncoder.encode(tiktokUrl.trim(), StandardCharsets.UTF_8.toString())
            val config = RemoteConfigManager.config
            
            val fallbackUrl = when(attempt) {
                1 -> "${config.fallbackApi1}?down=$encodedUrl"
                2 -> "${config.fallbackApi2}api/tiktok/info?url=$encodedUrl"
                else -> null
            }

            if (fallbackUrl == null) {
                downloadState = DownloadState.Error("No se pudo conectar con ningún servidor. Revisa tu conexión.")
                return
            }

            val response = RetrofitClient.getApi().getFallbackInfo(fallbackUrl)
            
            if (response.isSuccessful) {
                val data = response.body()
                val videoUrl: String?
                val audioUrl: String
                val title: String
                val cover: String
                
                if (attempt == 1) { // sl-bjs
                    videoUrl = data?.get("download_url") as? String
                    audioUrl = data?.get("music_url") as? String ?: ""
                    title = data?.get("title") as? String ?: "Video de TikTok"
                    cover = data?.get("thumbnail") as? String ?: ""
                } else { // douyin.wtf
                    val videoData = data?.get("video") as? Map<*, *>
                    videoUrl = videoData?.get("play_addr") as? String ?: data?.get("url") as? String
                    audioUrl = (data?.get("music") as? Map<*, *>)?.get("play_url") as? String ?: ""
                    title = data?.get("desc") as? String ?: "Video"
                    cover = data?.get("cover") as? String ?: ""
                }
                
                if (!videoUrl.isNullOrBlank()) {
                    val fallbackData = VideoData(
                        id = System.currentTimeMillis().toString(),
                        title = title,
                        play = videoUrl,
                        wmplay = "",
                        music = audioUrl,
                        cover = cover,
                        originCover = cover
                    )
                    currentVideoData = fallbackData
                    downloadState = DownloadState.InfoLoaded(fallbackData)
                } else {
                    tryFallbackInfo(attempt + 1)
                }
            } else {
                tryFallbackInfo(attempt + 1)
            }
        } catch (e: Exception) {
            tryFallbackInfo(attempt + 1)
        }
    }

    fun startDownload(type: DownloadType) {
        val videoData = currentVideoData ?: return
        
        viewModelScope.launch {
            downloadState = DownloadState.DownloadingStart(type)
            progress = 0f
            
            val isSlideshow = !videoData.images.isNullOrEmpty()
            val downloadType = if (isSlideshow && type == DownloadType.VIDEO) DownloadType.SLIDESHOW else type
            
            val url = if (type == DownloadType.VIDEO) videoData.play else videoData.music
            val extension = if (type == DownloadType.VIDEO) ".mp4" else ".mp3"
            val fileName = "tiktok_${videoData.id}$extension"

            if (url.isBlank() && downloadType != DownloadType.SLIDESHOW) {
                downloadState = DownloadState.Error("URL no disponible")
                return@launch
            }

            val result = downloader.downloadMedia(
                url = url,
                fileName = fileName,
                type = downloadType,
                images = videoData.images,
                audioUrl = videoData.music
            ) { p ->
                progress = p
                downloadState = DownloadState.Downloading(p, type)
            }

            if (result.isSuccess) {
                val folder = if(type == DownloadType.VIDEO) "galería" else "carpeta de música"
                downloadState = DownloadState.Success("Archivo guardado en la $folder")
            } else {
                downloadState = DownloadState.Error(result.exceptionOrNull()?.message ?: "Error al descargar")
            }
        }
    }
    
    fun reset() {
        tiktokUrl = ""
        currentVideoData = null
        downloadState = DownloadState.Idle
        progress = 0f
    }
}