package com.example.descargadortiktok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.descargadortiktok.downloader.DownloadType
import com.example.descargadortiktok.ui.theme.DescargadorTikTokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DescargadorTikTokTheme {
                TikTokDownloaderScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTokDownloaderScreen(viewModel: MainViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TikTok Downloader", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (viewModel.downloadState !is DownloadState.Idle) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Descarga sin marca de agua",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = viewModel.tiktokUrl,
                onValueChange = { viewModel.tiktokUrl = it },
                label = { Text("Pega el enlace de TikTok") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { viewModel.fetchVideoInfo() }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.fetchVideoInfo() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = viewModel.downloadState !is DownloadState.LoadingInfo && 
                          viewModel.downloadState !is DownloadState.DownloadingStart &&
                          viewModel.downloadState !is DownloadState.Downloading,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Analizar Enlace")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // UI Dinámica según el estado
            when (val state = viewModel.downloadState) {
                is DownloadState.LoadingInfo -> {
                    CircularProgressIndicator()
                    Text("Analizando video...", modifier = Modifier.padding(top = 8.dp))
                }

                is DownloadState.InfoLoaded -> {
                    VideoInfoCard(state.videoData, viewModel)
                }

                is DownloadState.DownloadingStart -> {
                    VideoInfoCard(viewModel.currentVideoData!!, viewModel, isDownloading = true)
                }

                is DownloadState.Downloading -> {
                    VideoInfoCard(viewModel.currentVideoData!!, viewModel, isDownloading = true, progress = state.progress)
                }

                is DownloadState.Success -> {
                    StatusCard(state.message, isError = false)
                    Button(onClick = { viewModel.reset() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Descargar otro")
                    }
                }

                is DownloadState.Error -> {
                    StatusCard(state.message, isError = true)
                }
                
                else -> {
                    Text("Introduce un link para empezar", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun VideoInfoCard(videoData: com.example.descargadortiktok.network.models.VideoData, viewModel: MainViewModel, isDownloading: Boolean = false, progress: Float = 0f) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = videoData.cover,
                    contentDescription = "Miniatura",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val isSlideshow = !videoData.images.isNullOrEmpty()
                    Text(
                        text = if (isSlideshow) "[Carrusel de Fotos]" else "[Video]",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = videoData.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Descargando: ${(progress * 100).toInt()}%",
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    fontSize = 12.sp
                )
            } else {
                Button(
                    onClick = { viewModel.startDownload(DownloadType.VIDEO) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val label = if (!videoData.images.isNullOrEmpty()) "Descargar Fotos como Video" else "Descargar Video"
                    Text(label)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startDownload(DownloadType.AUDIO) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar Solo Música (MP3)")
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, isError: Boolean) {
    val bgColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val textColor = if (isError) Color(0xFFC62828) else Color(0xFF2E7D32)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}