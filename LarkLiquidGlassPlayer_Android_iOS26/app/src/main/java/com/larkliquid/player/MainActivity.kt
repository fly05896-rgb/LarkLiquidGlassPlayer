package com.larkliquid.player

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.data.VideoRepository
import com.larkliquid.player.ui.home.HomeLibraryScreen
import com.larkliquid.player.ui.player.Ios26VideoPlayerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var activeVideoForPip: VideoItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = VideoRepository(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF0091FF),
                    secondary = Color(0xFF00E5FF),
                    background = Color(0xFF060911),
                    surface = Color(0xFF0E1525)
                )
            ) {
                val scope = rememberCoroutineScope()
                val videos = remember { mutableStateListOf<VideoItem>() }
                var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ ->
                    scope.launch {
                        val loaded = repository.loadVideos()
                        videos.clear()
                        videos.addAll(loaded)
                    }
                }

                LaunchedEffect(Unit) {
                    // Check if launched via external video VIEW intent
                    intent?.data?.let { externalUri ->
                        val extVideo = VideoItem(
                            id = -999L,
                            title = externalUri.lastPathSegment ?: "External Video",
                            uri = externalUri,
                            durationMs = 0L,
                            sizeBytes = 0L,
                            width = 1920,
                            height = 1080,
                            folderName = "External",
                            dateAddedSec = System.currentTimeMillis() / 1000L
                        )
                        selectedVideo = extVideo
                        activeVideoForPip = extVideo
                    }

                    val hasPerm = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        permissionToRequest
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                        videos.clear()
                        videos.addAll(repository.loadVideos())
                    } else {
                        // Load showcase streams immediately and request local video permission
                        videos.clear()
                        videos.addAll(repository.builtInShowcaseStreams())
                        permissionLauncher.launch(permissionToRequest)
                    }
                }

                Crossfade(targetState = selectedVideo, label = "ScreenTransition") { currentVideo ->
                    if (currentVideo == null) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        HomeLibraryScreen(
                            videos = videos,
                            onPlayVideo = { item ->
                                selectedVideo = item
                                activeVideoForPip = item
                            },
                            onPlayStreamUrl = { url ->
                                val streamItem = VideoItem(
                                    id = System.currentTimeMillis(),
                                    title = url.substringAfterLast('/').ifBlank { "Live Stream" },
                                    uri = Uri.parse(url),
                                    durationMs = 0L,
                                    sizeBytes = 0L,
                                    width = 1920,
                                    height = 1080,
                                    folderName = "Network Stream",
                                    dateAddedSec = System.currentTimeMillis() / 1000L
                                )
                                videos.add(0, streamItem)
                                selectedVideo = streamItem
                                activeVideoForPip = streamItem
                            },
                            onToggleFavorite = { videoId ->
                                val idx = videos.indexOfFirst { it.id == videoId }
                                if (idx >= 0) {
                                    val old = videos[idx]
                                    videos[idx] = old.copy(isFavorite = !old.isFavorite)
                                }
                            },
                            onTogglePrivateVault = { videoId ->
                                val idx = videos.indexOfFirst { it.id == videoId }
                                if (idx >= 0) {
                                    val old = videos[idx]
                                    videos[idx] = old.copy(isPrivateVault = !old.isPrivateVault)
                                }
                            }
                        )
                    } else {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        Ios26VideoPlayerScreen(
                            video = currentVideo,
                            onClose = {
                                selectedVideo = null
                                activeVideoForPip = null
                            },
                            onEnterPip = {
                                enterIos26PipMode()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun enterIos26PipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (activeVideoForPip != null) {
            enterIos26PipMode()
        }
    }
}
