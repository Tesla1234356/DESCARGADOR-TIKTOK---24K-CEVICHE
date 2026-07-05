package com.example.descargadortiktok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.descargadortiktok.downloader.DownloadType
import com.example.descargadortiktok.network.models.VideoData
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
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFF0F0F12)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F12),
                            Color(0xFF16161E),
                            Color(0xFF0F0F12)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Custom Top Header Libre de Recortes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icono_24k_ceviche),
                        contentDescription = "24K Ceviche",
                        modifier = Modifier.size(110.dp)
                    )
                    
                    if (viewModel.downloadState !is DownloadState.Idle) {
                        IconButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = Color(0xFFFE2C55))
                        }
                    }
                }

                // Header
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFFFE2C55), fontWeight = FontWeight.Black)) {
                            append("Tik")
                        }
                        withStyle(style = SpanStyle(color = Color(0xFF25F4EE), fontWeight = FontWeight.Black)) {
                            append("Tok")
                        }
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(" Pro")
                        }
                    },
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )

                Text(
                    text = "Descarga sin marcas de agua al instante",
                    fontSize = 13.sp,
                    color = Color(0xFF86878B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp)
                )

                // Campo de Texto Premium
                OutlinedTextField(
                    value = viewModel.tiktokUrl,
                    onValueChange = { viewModel.tiktokUrl = it },
                    placeholder = { Text("Pega el enlace de TikTok aquí", color = Color(0xFF86878B)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(18.dp)),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF25F4EE),
                        unfocusedBorderColor = Color(0xFF2C2C35),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF25F4EE),
                        focusedContainerColor = Color(0xFF1A1A22),
                        unfocusedContainerColor = Color(0xFF131318),
                        focusedPlaceholderColor = Color(0xFF86878B),
                        unfocusedPlaceholderColor = Color(0xFF86878B)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFFFE2C55)
                        )
                    },
                    trailingIcon = {
                        if (viewModel.tiktokUrl.isNotEmpty()) {
                            IconButton(onClick = { viewModel.tiktokUrl = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFFFE2C55))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        viewModel.tiktokUrl = clipText
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Pegar",
                                    tint = Color(0xFF25F4EE)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de Análisis
                val isAnalyzing = viewModel.downloadState is DownloadState.LoadingInfo ||
                        viewModel.downloadState is DownloadState.DownloadingStart ||
                        viewModel.downloadState is DownloadState.Downloading

                val buttonAlpha by animateFloatAsState(
                    targetValue = if (isAnalyzing || viewModel.tiktokUrl.isBlank()) 0.5f else 1f,
                    label = "buttonAlpha"
                )

                Button(
                    onClick = { viewModel.fetchVideoInfo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(alpha = buttonAlpha)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFE2C55), Color(0xFFE91E63), Color(0xFF25F4EE))
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = !isAnalyzing && viewModel.tiktokUrl.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANALIZAR ENLACE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Transición y visualización dinámica según estados
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    when (val state = viewModel.downloadState) {
                        is DownloadState.LoadingInfo -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "loadingTransition")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ), label = "angle"
                                )
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ), label = "pulseAlpha"
                                )

                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Analizando",
                                    tint = Color(0xFF25F4EE),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer(rotationZ = angle)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Analizando servidores de TikTok...",
                                    color = Color.White.copy(alpha = pulseAlpha),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        is DownloadState.InfoLoaded -> {
                            VideoInfoCard(state.videoData, viewModel)
                        }

                        is DownloadState.DownloadingStart -> {
                            VideoInfoCard(viewModel.currentVideoData!!, viewModel, isDownloading = true, progress = 0f)
                        }

                        is DownloadState.Downloading -> {
                            VideoInfoCard(viewModel.currentVideoData!!, viewModel, isDownloading = true, progress = state.progress)
                        }

                        is DownloadState.Success -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                StatusCard(state.message, isError = false)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.reset() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFFE2C55), Color(0xFF25F4EE))
                                            )
                                        ),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Text("DESCARGAR OTRO", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        is DownloadState.Error -> {
                            StatusCard(state.message, isError = true)
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131318), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF86878B),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Listo para iniciar descargas",
                                        color = Color(0xFF86878B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun VideoInfoCard(
    videoData: VideoData,
    viewModel: MainViewModel,
    isDownloading: Boolean = false,
    progress: Float = 0f
) {
    val isSlideshow = !videoData.images.isNullOrEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFFFE2C55),
                spotColor = Color(0xFF25F4EE)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFE2C55).copy(alpha = 0.4f),
                        Color(0xFF25F4EE).copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181820))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Miniatura
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFF25F4EE).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = videoData.cover,
                        contentDescription = "Miniatura",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(
                                color = if (isSlideshow) Color(0xFFFE2C55) else Color(0xFF25F4EE),
                                shape = RoundedCornerShape(topStart = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isSlideshow) Icons.Default.PhotoLibrary else Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (isSlideshow) Color.White else Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSlideshow) "CARRUSEL DE FOTOS" else "VIDEO DE TIKTOK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSlideshow) Color(0xFFFE2C55) else Color(0xFF25F4EE),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = videoData.title.ifBlank { "Video de TikTok" },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF25F4EE),
                        trackColor = Color(0xFF2C2C35)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulseText")
                        val textAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "textAlpha"
                        )

                        val statusText = if (viewModel.downloadState is DownloadState.DownloadingStart) {
                            "Preparando..."
                        } else if (isSlideshow && progress > 0.1f && progress < 0.9f) {
                            "FFmpeg: Generando video..."
                        } else {
                            "Descargando archivo..."
                        }

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = textAlpha),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25F4EE)
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.startDownload(DownloadType.VIDEO) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFE2C55), Color(0xFFE91E63))
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    val label = if (isSlideshow) "Descargar Fotos como Video" else "Descargar Video (.MP4)"
                    Text(label, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.startDownload(DownloadType.AUDIO) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E88E5), Color(0xFF25F4EE))
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar Solo Música (.MP3)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, isError: Boolean) {
    val accentColor = if (isError) Color(0xFFFE2C55) else Color(0xFF25F4EE)
    val containerBg = if (isError) Color(0x1BFE2C55) else Color(0x1B25F4EE)
    val icon = if (isError) Icons.Default.Error else Icons.Default.CheckCircle

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}