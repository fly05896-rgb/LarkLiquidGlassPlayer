package com.larkliquid.player

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.larkliquid.player.data.AppSettingsConfig
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.data.VideoPlaylist
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
            val scope = rememberCoroutineScope()
            val videos = remember { mutableStateListOf<VideoItem>() }
            val playlists = remember { mutableStateListOf<VideoPlaylist>().apply { addAll(repository.defaultPlaylists()) } }

            var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
            var floatingVideo by remember { mutableStateOf<VideoItem?>(null) }
            var isFloatingExpanded by remember { mutableStateOf(true) }
            var settingsConfig by remember { mutableStateOf(AppSettingsConfig()) }

            val colorScheme = if (settingsConfig.isDarkMode) {
                darkColorScheme(
                    primary = Color(0xFF0A84FF),
                    secondary = Color(0xFF64D2FF),
                    background = Color(0xFF0B0D13),
                    surface = Color(0xFF161922)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF007AFF),
                    secondary = Color(0xFF007AFF),
                    background = Color(0xFFF3F5F9),
                    surface = Color(0xFFFFFFFF)
                )
            }

            val directoryPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { treeUri: Uri? ->
                if (treeUri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    val displayPath = treeUri.lastPathSegment?.replace("primary:", "Internal Storage/")
                        ?: treeUri.toString()
                    settingsConfig = settingsConfig.copy(
                        privateFolderPath = displayPath,
                        privateFolderUriString = treeUri.toString()
                    )
                }
            }

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
                    permissionLauncher.launch(permissionToRequest)
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                Crossfade(targetState = selectedVideo, label = "ScreenTransition") { currentVideo ->
                    if (currentVideo == null) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        HomeLibraryScreen(
                            videos = videos,
                            playlists = playlists,
                            floatingVideo = floatingVideo,
                            isFloatingExpanded = isFloatingExpanded,
                            onSetFloatingExpanded = { isFloatingExpanded = it },
                            onCloseFloatingVideo = { floatingVideo = null },
                            onStartFloatingVideo = { item ->
                                floatingVideo = item
                                isFloatingExpanded = true
                            },
                            glassConfig = settingsConfig,
                            onUpdateGlassConfig = { settingsConfig = it },
                            onPlayVideo = { item ->
                                floatingVideo = null
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
                                    val movingToPrivate = !old.isPrivateVault
                                    videos[idx] = old.copy(
                                        isPrivateVault = movingToPrivate,
                                        folderName = if (movingToPrivate) settingsConfig.privateFolderPath else old.folderName
                                    )
                                }
                            },
                            onSoftDeleteVideo = { videoId ->
                                val idx = videos.indexOfFirst { it.id == videoId }
                                if (idx >= 0) {
                                    val old = videos[idx]
                                    videos[idx] = old.copy(
                                        isDeleted = true,
                                        deletedAtSec = System.currentTimeMillis() / 1000L
                                    )
                                    if (floatingVideo?.id == videoId) floatingVideo = null
                                }
                            },
                            onRestoreDeletedVideo = { videoId ->
                                val idx = videos.indexOfFirst { it.id == videoId }
                                if (idx >= 0) {
                                    val old = videos[idx]
                                    videos[idx] = old.copy(isDeleted = false, deletedAtSec = 0L)
                                }
                            },
                            onPermanentDeleteVideo = { videoId ->
                                videos.removeAll { it.id == videoId }
                            },
                            onCreatePlaylist = { name, initialVideoIds ->
                                playlists.add(
                                    0,
                                    VideoPlaylist(
                                        id = System.currentTimeMillis(),
                                        name = name,
                                        videoIds = initialVideoIds
                                    )
                                )
                            },
                            onDeletePlaylist = { playlistId ->
                                playlists.removeAll { it.id == playlistId }
                            },
                            onAddVideoToPlaylist = { playlistId, videoId ->
                                val idx = playlists.indexOfFirst { it.id == playlistId }
                                if (idx >= 0) {
                                    val pl = playlists[idx]
                                    if (videoId !in pl.videoIds) {
                                        playlists[idx] = pl.copy(videoIds = pl.videoIds + videoId)
                                    }
                                }
                            },
                            onRemoveVideoFromPlaylist = { playlistId, videoId ->
                                val idx = playlists.indexOfFirst { it.id == playlistId }
                                if (idx >= 0) {
                                    val pl = playlists[idx]
                                    playlists[idx] = pl.copy(videoIds = pl.videoIds - videoId)
                                }
                            },
                            onRescanMediaFiles = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    permissionToRequest
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    permissionLauncher.launch(permissionToRequest)
                                }
                                val existingStates = videos.associateBy { it.id }
                                val fresh = repository.loadVideos().map { item ->
                                    val prev = existingStates[item.id]
                                    if (prev != null) {
                                        item.copy(
                                            isFavorite = prev.isFavorite,
                                            isPrivateVault = prev.isPrivateVault,
                                            isDeleted = prev.isDeleted,
                                            lastPositionMs = prev.lastPositionMs
                                        )
                                    } else item
                                }
                                videos.clear()
                                videos.addAll(fresh)
                                fresh.count { !it.isDeleted }
                            },
                            onPickPrivateFolderDirectory = {
                                runCatching { directoryPickerLauncher.launch(null) }
                            }
                        )
                    } else {
                        Ios26VideoPlayerScreen(
                            video = currentVideo,
                            glassConfig = settingsConfig,
                            onClose = { lastPosMs ->
                                val idx = videos.indexOfFirst { it.id == currentVideo.id }
                                if (idx >= 0) {
                                    videos[idx] = videos[idx].copy(lastPositionMs = lastPosMs)
                                }
                                selectedVideo = null
                                activeVideoForPip = null
                            },
                            onMinimizeToFloating = { lastPosMs ->
                                val updated = currentVideo.copy(lastPositionMs = lastPosMs)
                                val idx = videos.indexOfFirst { it.id == currentVideo.id }
                                if (idx >= 0) {
                                    videos[idx] = updated
                                }
                                selectedVideo = null
                                activeVideoForPip = null
                                floatingVideo = updated
                                isFloatingExpanded = true
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
