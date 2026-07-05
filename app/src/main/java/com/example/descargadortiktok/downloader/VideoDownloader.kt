package com.example.descargadortiktok.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.descargadortiktok.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

enum class DownloadType {
    VIDEO, AUDIO, SLIDESHOW
}

class VideoDownloader(private val context: Context) {

    private val client = RetrofitClient.okHttpClient

    suspend fun downloadMedia(
        url: String,
        fileName: String,
        type: DownloadType,
        images: List<String>? = null,
        audioUrl: String? = null,
        onProgress: (Float) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (type == DownloadType.SLIDESHOW && images != null && audioUrl != null) {
            return@withContext downloadAndRenderSlideshow(images, audioUrl, fileName, onProgress)
        }

        var uri: Uri? = null
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error de red: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Cuerpo vacío"))
            val totalBytes = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            uri = createMediaUri(fileName, if (type == DownloadType.AUDIO) DownloadType.AUDIO else DownloadType.VIDEO) 
                ?: return@withContext Result.failure(Exception("Error MediaStore"))

            val outputStream: OutputStream = context.contentResolver.openOutputStream(uri) 
                ?: return@withContext Result.failure(Exception("Error al abrir stream"))

            outputStream.use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        onProgress(totalRead.toFloat() / totalBytes)
                    }
                }
                output.flush()
            }

            finalizeMediaUri(uri)
            refreshGallery(uri)

            Result.success(uri)
        } catch (e: Exception) {
            uri?.let { context.contentResolver.delete(it, null, null) }
            Result.failure(e)
        }
    }

    private suspend fun downloadAndRenderSlideshow(
        images: List<String>,
        audioUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<Uri> {
        val tempDir = File(context.cacheDir, "slideshow_temp").apply { mkdirs() }
        val imageFiles = mutableListOf<File>()
        val audioFile = File(tempDir, "audio.mp3")

        try {
            // 1. Download Resources
            images.forEachIndexed { index, url ->
                onProgress(0.05f * (index + 1) / images.size)
                val file = File(tempDir, "img_$index.jpg")
                downloadFile(url, file)
                imageFiles.add(file)
            }
            onProgress(0.1f)
            downloadFile(audioUrl, audioFile)

            // 2. Get Audio Duration using FFmpeg
            val audioInfo = FFmpegKit.execute("-i \"${audioFile.absolutePath}\"")
            val output = audioInfo.allLogsAsString
            val durationRegex = "Duration: (\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})".toRegex()
            val match = durationRegex.find(output)
            val totalSeconds = if (match != null) {
                val (h, m, s, ms) = match.destructured
                h.toDouble() * 3600 + m.toDouble() * 60 + s.toDouble() + ms.toDouble() / 100
            } else 15.0

            // 3. Calculate timings and create command
            val transitionDuration = 0.5
            val outputVideo = File(tempDir, "output.mp4")

            // Lógica de tiempo dinámica:
            // Si hay pocas imágenes (1-3), rotar rápido (4s) y loopear.
            // Si hay muchas (4+), repartir el tiempo total.
            val isFewImages = images.size <= 3
            val imageDuration = if (isFewImages) 4.0 else totalSeconds / images.size
            
            val inputs = StringBuilder()
            val filterComplex = StringBuilder()
            
            images.forEachIndexed { index, _ ->
                inputs.append("-loop 1 -t ${imageDuration + transitionDuration} -i \"${imageFiles[index].absolutePath}\" ")
                
                // Efecto Dinámico (Zoom-in / Ken Burns effect) para darle movimiento a las fotos
                val frames = ((imageDuration + transitionDuration) * 25).toInt()
                val zoomFilter = "scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2,zoompan=z='min(zoom+0.0015,1.15)':d=$frames:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=720x1280:fps=25,format=yuv420p"
                
                filterComplex.append("[$index:v]$zoomFilter[v$index];")
            }
            inputs.append("-i \"${audioFile.absolutePath}\" ")

            var lastStream = "v0"
            if (images.size > 1) {
                for (i in 1 until images.size) {
                    val offset = i * imageDuration - (transitionDuration * (i - 1))
                    val nextStream = "f${i-1}"
                    filterComplex.append("[$lastStream][v$i]xfade=transition=fade:duration=$transitionDuration:offset=$offset[$nextStream];")
                    lastStream = nextStream
                }
                
                // Si son pocas fotos, loopeamos el resultado de la concatenación para cubrir el audio
                if (isFewImages) {
                    val loopStream = "v_loop"
                    filterComplex.append("[$lastStream]loop=loop=-1:size=${(imageDuration * images.size * 25).toInt()}:start=0[$loopStream];")
                    lastStream = loopStream
                }
            } else {
                filterComplex.append("[v0]loop=loop=-1:size=1:start=0[v0_looped];")
                lastStream = "v0_looped"
            }

            val command = "${inputs}-filter_complex \"${filterComplex}\" -map \"[$lastStream]\" -map ${images.size}:a -c:v libx264 -preset fast -tune stillimage -crf 23 -c:a aac -b:a 128k -pix_fmt yuv420p -t $totalSeconds \"${outputVideo.absolutePath}\""

            onProgress(0.4f)
            val session = FFmpegKit.execute(command)
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                onProgress(0.9f)
                val uri = saveFileToMediaStore(outputVideo, fileName, DownloadType.VIDEO)
                tempDir.deleteRecursively()
                return if (uri != null) Result.success(uri) else Result.failure(Exception("Error al guardar"))
            } else {
                val logs = session.allLogsAsString
                tempDir.deleteRecursively()
                return Result.failure(Exception("FFmpeg Error: ${logs.takeLast(200)}"))
            }
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            return Result.failure(e)
        }
    }

    private fun downloadFile(url: String, outputFile: File) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Error al bajar recurso: ${response.code}")
        response.body?.byteStream()?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun saveFileToMediaStore(file: File, fileName: String, type: DownloadType): Uri? {
        val uri = createMediaUri(fileName, type) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        finalizeMediaUri(uri)
        refreshGallery(uri)
        return uri
    }

    private fun createMediaUri(fileName: String, type: DownloadType): Uri? {
        val extension = if (type == DownloadType.VIDEO) ".mp4" else ".mp3"
        val fullFileName = if (fileName.endsWith(extension)) fileName else "$fileName$extension"
        val mimeType = if (type == DownloadType.VIDEO) "video/mp4" else "audio/mpeg"
        val relativePath = if (type == DownloadType.VIDEO) {
            Environment.DIRECTORY_MOVIES + "/TikTokDownloader"
        } else {
            Environment.DIRECTORY_MUSIC + "/TikTokDownloader"
        }
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (type == DownloadType.VIDEO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        }

        return context.contentResolver.insert(collection, contentValues)
    }

    private fun finalizeMediaUri(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, contentValues, null, null)
        }
    }

    private fun refreshGallery(uri: Uri) {
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            val filePath = cursor?.use {
                if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)) else null
            }
            
            if (filePath != null) {
                val mimeType = context.contentResolver.getType(uri)
                MediaScannerConnection.scanFile(context, arrayOf(filePath), arrayOf(mimeType), null)
            }
        } catch (e: Exception) {}
    }
}