package com.larkliquid.player.ui.home

import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.larkliquid.player.R
import com.larkliquid.player.data.AppLanguage
import com.larkliquid.player.data.AppLocalization
import com.larkliquid.player.data.AppSettingsConfig
import com.larkliquid.player.data.SortOption
import com.larkliquid.player.data.UiStrings
import com.larkliquid.player.data.VideoFolder
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.data.VideoPlaylist
import com.larkliquid.player.ui.components.VideoThumbnailImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class HomeTab {
    ALL_VIDEOS,
    FOLDERS,
    PLAYLISTS,
    SETTINGS;

    fun localizedTitle(strings: UiStrings): String = when (this) {
        ALL_VIDEOS -> strings.tabVideos
        FOLDERS -> strings.tabFolders
        PLAYLISTS -> strings.tabPlaylists
        SETTINGS -> strings.tabSettings
    }
}

@Composable
fun HomeLibraryScreen(
    videos: List<VideoItem>,
    playlists: List<VideoPlaylist>,
    floatingVideo: VideoItem?,
    isFloatingExpanded: Boolean,
    onSetFloatingExpanded: (Boolean) -> Unit,
    onCloseFloatingVideo: () -> Unit,
    onStartFloatingVideo: (VideoItem) -> Unit,
    glassConfig: AppSettingsConfig,
    onUpdateGlassConfig: (AppSettingsConfig) -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onPlayStreamUrl: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onTogglePrivateVault: (Long) -> Unit,
    onSoftDeleteVideo: (Long) -> Unit,
    onRestoreDeletedVideo: (Long) -> Unit,
    onPermanentDeleteVideo: (Long) -> Unit,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddVideoToPlaylist: (Long, Long) -> Unit,
    onRemoveVideoFromPlaylist: (Long, Long) -> Unit,
    onRescanMediaFiles: suspend () -> Int,
    onPickPrivateFolderDirectory: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val currentLang = remember(configuration) { AppLanguage.fromConfiguration(configuration) }
    val strings = remember(currentLang) { AppLocalization.stringsFor(currentLang) }

    var selectedTab by remember { mutableStateOf(HomeTab.ALL_VIDEOS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }

    var openedFolderName by remember { mutableStateOf<String?>(null) }
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showPrivateVaultModal by remember { mutableStateOf(false) }
    var showRecentlyDeletedModal by remember { mutableStateOf(false) }

    var showStreamDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedVideosForNewPlaylist by remember { mutableStateOf(setOf<Long>()) }
    var videoIdForAddToPlaylistSheet by remember { mutableStateOf<Long?>(null) }
    var showFolderPickerSheet by remember { mutableStateOf(false) }

    var isScanningFiles by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }

    // BackHandler so pressing Back inside a folder, playlist, or secondary tab returns to ALL_VIDEOS instead of exiting the app
    BackHandler(enabled = openedFolderName != null || openedPlaylistId != null || selectedTab != HomeTab.ALL_VIDEOS) {
        when {
            openedFolderName != null -> openedFolderName = null
            openedPlaylistId != null -> openedPlaylistId = null
            selectedTab != HomeTab.ALL_VIDEOS -> selectedTab = HomeTab.ALL_VIDEOS
        }
    }

    val isDark = glassConfig.isDarkMode
    val bgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0B0D13) else Color(0xFFF3F5F9),
        animationSpec = tween(260),
        label = "bgColor"
    )
    val cardColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF),
        animationSpec = tween(260),
        label = "cardColor"
    )
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
    val glassSurfaceTint = if (isDark) Color(0xFF12151E).copy(alpha = 0.42f) else Color(0xFFFAFAFA).copy(alpha = 0.48f)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentBlue = if (isDark) Color(0xFF0091FF) else Color(0xFF0088FF)
    val pillUnselectedBg = if (isDark) Color(0xFF222736) else Color(0xFFE6EAF2)

    val filterChips = remember(strings) {
        listOf(strings.filterAll, strings.filterHighRes, strings.filterFavorites, strings.filterRecent)
    }

    val publicVideos by remember(videos, searchQuery, selectedFilterIndex, sortOption) {
        derivedStateOf {
            videos.filter { item ->
                !item.isPrivateVault && !item.isDeleted &&
                        (searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                item.folderName.contains(searchQuery, ignoreCase = true)) &&
                        when (selectedFilterIndex) {
                            1 -> item.width >= 1920 || item.height >= 1080
                            2 -> item.isFavorite
                            3 -> item.lastPositionMs > 0L
                            else -> true
                        }
            }.let { list ->
                when (sortOption) {
                    SortOption.DATE_DESC -> list.sortedByDescending { it.dateAddedSec }
                    SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
                    SortOption.DURATION_DESC -> list.sortedByDescending { it.durationMs }
                    SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                }
            }
        }
    }

    val privateVaultVideos by remember(videos) {
        derivedStateOf { videos.filter { it.isPrivateVault && !it.isDeleted } }
    }

    val recentlyDeletedVideos by remember(videos) {
        derivedStateOf { videos.filter { it.isDeleted } }
    }

    val folders by remember(videos) {
        derivedStateOf {
            videos.filter { !it.isPrivateVault && !it.isDeleted }
                .groupBy { it.folderName }
                .map { (name, items) -> VideoFolder(name, items) }
        }
    }

    // Real LayerBackdrop that captures the scrolling content so the Top Search Bar and Bottom Navigation Bar refract it
    val mainBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }

    CompositionLocalProvider(LocalLayoutDirection provides currentLang.layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // ==================== LAYER 1: MAIN TAB CONTENT (RECORDED INTO mainBackdrop) ====================
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val isRtlDir = currentLang.isRtl
                    val forward = targetState.ordinal >= initialState.ordinal
                    val sign = if (forward) (if (isRtlDir) -1 else 1) else (if (isRtlDir) 1 else -1)
                    (slideInHorizontally(animationSpec = spring(stiffness = 380f)) { width -> sign * width / 5 } +
                            fadeIn(animationSpec = tween(200))) togetherWith
                            (slideOutHorizontally(animationSpec = spring(stiffness = 380f)) { width -> -sign * width / 5 } +
                                    fadeOut(animationSpec = tween(150)))
                },
                label = "TabContentTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(mainBackdrop)
            ) { currentTab ->
                when (currentTab) {
                    HomeTab.ALL_VIDEOS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 112.dp,
                                bottom = 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "filter_chips_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp)
                                ) {
                                    items(filterChips.size) { index ->
                                        val isSelected = selectedFilterIndex == index
                                        Box(
                                            modifier = Modifier
                                                .clip(Capsule())
                                                .background(if (isSelected) accentBlue else pillUnselectedBg)
                                                .clickable { selectedFilterIndex = index }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = filterChips[index],
                                                color = if (isSelected) Color.White else textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            if (publicVideos.isEmpty()) {
                                item(key = "empty_videos_state") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 36.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(cardColor)
                                            .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.app_logo_ios26),
                                            contentDescription = "EG Player",
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                        )
                                        Text(
                                            text = strings.emptyVideosTitle,
                                            color = textPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = strings.emptyVideosSubtitle,
                                            color = textSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (!isScanningFiles) {
                                                        isScanningFiles = true
                                                        scope.launch {
                                                            delay(350L)
                                                            val count = onRescanMediaFiles()
                                                            isScanningFiles = false
                                                            scanStatusMessage = String.format(strings.scanCompletedFmt, count)
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                                shape = Capsule()
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(strings.scanNowBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Button(
                                                onClick = { showStreamDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = pillUnselectedBg),
                                                shape = Capsule()
                                            ) {
                                                Icon(Icons.Default.Link, contentDescription = null, tint = textPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Stream URL", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(publicVideos, key = { it.id }) { video ->
                                    CleanVideoCard(
                                        video = video,
                                        strings = strings,
                                        cardColor = cardColor,
                                        borderColor = cardBorderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        accentBlue = accentBlue,
                                        onClick = { onPlayVideo(video, publicVideos) },
                                        onStartFloating = { onStartFloatingVideo(video) },
                                        onToggleFavorite = { onToggleFavorite(video.id) },
                                        onAddToPlaylist = { videoIdForAddToPlaylistSheet = video.id },
                                        onMoveToPrivate = { onTogglePrivateVault(video.id) },
                                        onDeleteToTrash = { onSoftDeleteVideo(video.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeTab.FOLDERS -> {
                        val activeFolder = folders.find { it.name == openedFolderName }
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 112.dp,
                                bottom = 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (activeFolder == null) {
                                item(key = "folders_header") {
                                    Text(
                                        text = String.format(strings.foldersCountFmt, folders.size),
                                        color = textPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                items(folders, key = { it.name }) { folder ->
                                    CleanFolderCard(
                                        folder = folder,
                                        strings = strings,
                                        isRtl = currentLang.isRtl,
                                        cardColor = cardColor,
                                        borderColor = cardBorderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        accentBlue = accentBlue,
                                        onClick = { openedFolderName = folder.name }
                                    )
                                }
                            } else {
                                item(key = "folder_back_bar") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardColor)
                                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .clickable { openedFolderName = null }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = accentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "${activeFolder.name} (${activeFolder.videos.size})",
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(activeFolder.videos, key = { it.id }) { video ->
                                    CleanVideoCard(
                                        video = video,
                                        strings = strings,
                                        cardColor = cardColor,
                                        borderColor = cardBorderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        accentBlue = accentBlue,
                                        onClick = { onPlayVideo(video, activeFolder.videos) },
                                        onStartFloating = { onStartFloatingVideo(video) },
                                        onToggleFavorite = { onToggleFavorite(video.id) },
                                        onAddToPlaylist = { videoIdForAddToPlaylistSheet = video.id },
                                        onMoveToPrivate = { onTogglePrivateVault(video.id) },
                                        onDeleteToTrash = { onSoftDeleteVideo(video.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeTab.PLAYLISTS -> {
                        val activePlaylist = playlists.find { it.id == openedPlaylistId }
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 112.dp,
                                bottom = 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (activePlaylist == null) {
                                item(key = "create_playlist_btn") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(accentBlue)
                                            .clickable {
                                                newPlaylistName = ""
                                                selectedVideosForNewPlaylist = emptySet()
                                                showCreatePlaylistDialog = true
                                            }
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Playlist",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = strings.createPlaylistBtn,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                items(playlists, key = { it.id }) { playlist ->
                                    val count = playlist.videoIds.count { id -> videos.any { it.id == id && !it.isDeleted } }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(cardColor)
                                            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
                                            .clickable { openedPlaylistId = playlist.id }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(accentBlue.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistPlay,
                                                contentDescription = playlist.name,
                                                tint = accentBlue,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                color = textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = String.format(strings.videosCountFmt, count),
                                                color = textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Playlist",
                                            tint = textSecondary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onDeletePlaylist(playlist.id) }
                                        )
                                    }
                                }
                            } else {
                                val playlistVideos = videos.filter { it.id in activePlaylist.videoIds && !it.isDeleted }
                                item(key = "playlist_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardColor)
                                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .clickable { openedPlaylistId = null }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Back",
                                                tint = accentBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "${activePlaylist.name} (${playlistVideos.size})",
                                                color = textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (playlistVideos.isNotEmpty()) {
                                            Text(
                                                text = strings.playAllBtn,
                                                color = accentBlue,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { onPlayVideo(playlistVideos.first(), playlistVideos) }
                                            )
                                        }
                                    }
                                }
                                items(playlistVideos, key = { it.id }) { video ->
                                    CleanVideoCard(
                                        video = video,
                                        strings = strings,
                                        cardColor = cardColor,
                                        borderColor = cardBorderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        accentBlue = accentBlue,
                                        onClick = { onPlayVideo(video, playlistVideos) },
                                        onStartFloating = { onStartFloatingVideo(video) },
                                        onToggleFavorite = { onToggleFavorite(video.id) },
                                        onAddToPlaylist = { onRemoveVideoFromPlaylist(activePlaylist.id, video.id) },
                                        onMoveToPrivate = { onTogglePrivateVault(video.id) },
                                        onDeleteToTrash = { onSoftDeleteVideo(video.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeTab.SETTINGS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 112.dp,
                                bottom = 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Day / Night Theme Switch Card (with LiquidToggle)
                            item(key = "settings_theme") {
                                SettingsCardContainer(cardColor, cardBorderColor) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentBlue.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                    contentDescription = null,
                                                    tint = accentBlue
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = strings.settingsThemeTitle,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (isDark) strings.settingsThemeDarkSub else strings.settingsThemeLightSub,
                                                    color = textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        LiquidToggle(
                                            selected = { isDark },
                                            onSelect = { onUpdateGlassConfig(glassConfig.copy(isDarkMode = it)) },
                                            backdrop = mainBackdrop
                                        )
                                    }
                                }
                            }

                            // 2. Scan Media Files for New Videos (Clean Vertical Layout so text never wraps in narrow column)
                            item(key = "settings_scan_files") {
                                SettingsCardContainer(cardColor, cardBorderColor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF10B981).copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = strings.settingsScanTitle,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = scanStatusMessage ?: strings.settingsScanSub,
                                                    color = textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (!isScanningFiles) {
                                                    isScanningFiles = true
                                                    scope.launch {
                                                        delay(400L)
                                                        val totalFound = onRescanMediaFiles()
                                                        isScanningFiles = false
                                                        scanStatusMessage = String.format(strings.scanCompletedFmt, totalFound)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                            shape = Capsule(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isScanningFiles) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(strings.scanningProgress, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(strings.scanNowBtn, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (isScanningFiles) {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(Capsule()),
                                                color = accentBlue
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Private Folder & Custom Folder Picker
                            item(key = "settings_private_folder") {
                                SettingsCardContainer(cardColor, cardBorderColor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showPrivateVaultModal = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderSpecial,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = strings.settingsPrivateFolderTitle,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = String.format(
                                                        strings.settingsPrivateFolderSubFmt,
                                                        glassConfig.privateFolderPath,
                                                        privateVaultVideos.size
                                                    ),
                                                    color = textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(pillUnselectedBg)
                                                    .clickable { showFolderPickerSheet = true }
                                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = strings.pickPrivateFolderBtn,
                                                    color = textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentBlue.copy(alpha = 0.14f))
                                                    .clickable { showPrivateVaultModal = true }
                                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = String.format(strings.managePrivateVideosBtnFmt, privateVaultVideos.size),
                                                    color = accentBlue,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Recently Deleted
                            item(key = "settings_recently_deleted") {
                                SettingsCardContainer(cardColor, cardBorderColor) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showRecentlyDeletedModal = true },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RestoreFromTrash,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = strings.settingsTrashTitle,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = String.format(strings.settingsTrashSubFmt, recentlyDeletedVideos.size),
                                                    color = textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${recentlyDeletedVideos.size}",
                                            color = accentBlue,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 5. Smart Playback Preferences (Clean user-friendly options — zero geeky text)
                            item(key = "settings_playback_prefs") {
                                SettingsCardContainer(cardColor, cardBorderColor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = accentBlue,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = strings.settingsPlaybackSectionTitle,
                                                color = textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        SettingsToggleRow(
                                            title = strings.settingsResumeTitle,
                                            subtitle = strings.settingsResumeSub,
                                            checked = glassConfig.resumeLastPosition,
                                            backdrop = mainBackdrop,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onCheckedChange = {
                                                onUpdateGlassConfig(glassConfig.copy(resumeLastPosition = it))
                                            }
                                        )

                                        SettingsToggleRow(
                                            title = strings.settingsAutoPlayTitle,
                                            subtitle = strings.settingsAutoPlaySub,
                                            checked = glassConfig.autoPlayNext,
                                            backdrop = mainBackdrop,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onCheckedChange = {
                                                onUpdateGlassConfig(glassConfig.copy(autoPlayNext = it))
                                            }
                                        )

                                        SettingsToggleRow(
                                            title = strings.settingsBgAudioTitle,
                                            subtitle = strings.settingsBgAudioSub,
                                            checked = glassConfig.backgroundAudioEnabled,
                                            backdrop = mainBackdrop,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            onCheckedChange = {
                                                onUpdateGlassConfig(glassConfig.copy(backgroundAudioEnabled = it))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== LAYER 2: REAL LIQUID GLASS TOP SEARCH BAR ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .drawBackdrop(
                            backdrop = mainBackdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(20f.dp.toPx(), 24f.dp.toPx())
                            },
                            highlight = { Highlight.Default },
                            shadow = { Shadow.Default },
                            onDrawSurface = {
                                drawRect(glassSurfaceTint)
                            }
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_ios26),
                        contentDescription = "EG Player Logo",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchPlaceholder,
                                color = textSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(accentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    // Day / Night Quick Switch Button
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Day/Night Mode",
                        tint = accentBlue,
                        modifier = Modifier
                            .size(21.dp)
                            .clickable {
                                onUpdateGlassConfig(glassConfig.copy(isDarkMode = !isDark))
                            }
                    )
                    // Sort Order Button
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = textPrimary,
                        modifier = Modifier
                            .size(21.dp)
                            .clickable {
                                val nextIdx = (sortOption.ordinal + 1) % SortOption.entries.size
                                sortOption = SortOption.entries[nextIdx]
                            }
                    )
                }
            }

            // ==================== LAYER 3: RETRACTABLE FLOATING VIDEO PLAYER ====================
            if (floatingVideo != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    RetractableFloatingVideoOverlay(
                        video = floatingVideo,
                        isExpanded = isFloatingExpanded,
                        onToggleExpanded = { onSetFloatingExpanded(!isFloatingExpanded) },
                        onExpandToFullPlayer = {
                            val v = floatingVideo
                            onCloseFloatingVideo()
                            onPlayVideo(v, publicVideos.ifEmpty { listOf(v) })
                        },
                        onClose = onCloseFloatingVideo,
                        accentBlue = accentBlue,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 96.dp)
                    )
                }
            }

            // ==================== LAYER 4: OFFICIAL ANDROID LIQUID GLASS BOTTOM TABS ====================
            LiquidBottomTabs(
                selectedTabIndex = { selectedTab.ordinal },
                onTabSelected = { idx ->
                    selectedTab = HomeTab.entries.getOrElse(idx) { HomeTab.ALL_VIDEOS }
                },
                backdrop = mainBackdrop,
                tabsCount = HomeTab.entries.size,
                isDarkThemeOverride = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.BottomCenter)
            ) {
                HomeTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) accentBlue else textPrimary,
                        animationSpec = tween(180),
                        label = "tabColor"
                    )
                    val icon = when (tab) {
                        HomeTab.ALL_VIDEOS -> Icons.Default.VideoLibrary
                        HomeTab.FOLDERS -> Icons.Default.Folder
                        HomeTab.PLAYLISTS -> Icons.Default.PlaylistPlay
                        HomeTab.SETTINGS -> Icons.Default.Settings
                    }
                    LiquidBottomTab(
                        onClick = { selectedTab = tab }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.name,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = tab.localizedTitle(strings),
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ==================== DIALOGS & SHEETS ====================

        // 1. Create & Name Custom Playlist Dialog
        if (showCreatePlaylistDialog) {
            val candidateVideos = videos.filter { !it.isDeleted && !it.isPrivateVault }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text(strings.newPlaylistDialogTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text(strings.newPlaylistPlaceholder) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (candidateVideos.isNotEmpty()) {
                            Text(
                                text = strings.selectVideosLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(candidateVideos, key = { it.id }) { v ->
                                    val checked = v.id in selectedVideosForNewPlaylist
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedVideosForNewPlaylist = if (checked) {
                                                    selectedVideosForNewPlaylist - v.id
                                                } else {
                                                    selectedVideosForNewPlaylist + v.id
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isChecked ->
                                                selectedVideosForNewPlaylist = if (isChecked) {
                                                    selectedVideosForNewPlaylist + v.id
                                                } else {
                                                    selectedVideosForNewPlaylist - v.id
                                                }
                                            }
                                        )
                                        Text(
                                            text = v.title,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val finalName = newPlaylistName.trim().ifBlank { "Playlist ${playlists.size + 1}" }
                            onCreatePlaylist(finalName, selectedVideosForNewPlaylist.toList())
                            showCreatePlaylistDialog = false
                        }
                    ) {
                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }

        // 2. Add Specific Video to Existing Playlist Dialog
        if (videoIdForAddToPlaylistSheet != null) {
            val targetVidId = videoIdForAddToPlaylistSheet!!
            AlertDialog(
                onDismissRequest = { videoIdForAddToPlaylistSheet = null },
                title = { Text(strings.addToPlaylistTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { pl ->
                            val alreadyIn = targetVidId in pl.videoIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (alreadyIn) accentBlue.copy(alpha = 0.14f) else pillUnselectedBg)
                                    .clickable {
                                        if (alreadyIn) {
                                            onRemoveVideoFromPlaylist(pl.id, targetVidId)
                                        } else {
                                            onAddVideoToPlaylist(pl.id, targetVidId)
                                        }
                                        videoIdForAddToPlaylistSheet = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = pl.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (alreadyIn) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = accentBlue)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = textSecondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            videoIdForAddToPlaylistSheet = null
                            selectedVideosForNewPlaylist = setOf(targetVidId)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = true
                        }
                    ) {
                        Text(strings.newPlaylistDialogTitle)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoIdForAddToPlaylistSheet = null }) {
                        Text(strings.closeBtn)
                    }
                }
            )
        }

        // 3. Select Folder for Private Vault Dialog
        if (showFolderPickerSheet) {
            val availableFolderPaths = remember(folders) {
                (listOf(
                    "Internal Storage/EGPlayerPrivateVault",
                    "Internal Storage/DCIM/PrivateVideos",
                    "Internal Storage/Movies/SecretVault"
                ) + folders.map { "Internal Storage/${it.name}/Private" }).distinct()
            }
            var customFolderInput by remember { mutableStateOf(glassConfig.privateFolderPath) }

            AlertDialog(
                onDismissRequest = { showFolderPickerSheet = false },
                title = { Text(strings.pickPrivateFolderBtn, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                showFolderPickerSheet = false
                                onPickPrivateFolderDirectory()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.pickPrivateFolderBtn, color = Color.White, fontSize = 13.sp)
                        }

                        OutlinedTextField(
                            value = customFolderInput,
                            onValueChange = { customFolderInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        availableFolderPaths.forEach { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { customFolderInput = path }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = path, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (customFolderInput.isNotBlank()) {
                                onUpdateGlassConfig(glassConfig.copy(privateFolderPath = customFolderInput.trim()))
                            }
                            showFolderPickerSheet = false
                        }
                    ) {
                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFolderPickerSheet = false }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }

        // 4. Private Vault Modal
        if (showPrivateVaultModal) {
            AlertDialog(
                onDismissRequest = { showPrivateVaultModal = false },
                title = {
                    Column {
                        Text(strings.settingsPrivateFolderTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = glassConfig.privateFolderPath,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (privateVaultVideos.isEmpty()) {
                            Text(text = strings.emptyVideosTitle, fontSize = 13.sp)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(privateVaultVideos, key = { it.id }) { v ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(pillUnselectedBg)
                                            .clickable {
                                                showPrivateVaultModal = false
                                                onPlayVideo(v, privateVaultVideos)
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(v.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                            Text(v.formattedDuration, fontSize = 11.sp, color = textSecondary)
                                        }
                                        TextButton(onClick = { onTogglePrivateVault(v.id) }) {
                                            Text(strings.restoreToPublicBtn, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPrivateVaultModal = false
                        showFolderPickerSheet = true
                    }) {
                        Text(strings.pickPrivateFolderBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrivateVaultModal = false }) {
                        Text(strings.closeBtn)
                    }
                }
            )
        }

        // 5. Recently Deleted Modal
        if (showRecentlyDeletedModal) {
            AlertDialog(
                onDismissRequest = { showRecentlyDeletedModal = false },
                title = { Text(strings.settingsTrashTitle, fontWeight = FontWeight.Bold) },
                text = {
                    if (recentlyDeletedVideos.isEmpty()) {
                        Text(strings.emptyVideosTitle, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentlyDeletedVideos, key = { it.id }) { v ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillUnselectedBg)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(v.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(v.formattedSize, fontSize = 11.sp, color = textSecondary)
                                    }
                                    Row {
                                        Icon(
                                            imageVector = Icons.Default.RestoreFromTrash,
                                            contentDescription = "Restore",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable { onRestoreDeletedVideo(v.id) }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Forever",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clickable { onPermanentDeleteVideo(v.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (recentlyDeletedVideos.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                recentlyDeletedVideos.forEach { onRestoreDeletedVideo(it.id) }
                                showRecentlyDeletedModal = false
                            }
                        ) {
                            Text(strings.restoreAllBtn)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecentlyDeletedModal = false }) {
                        Text(strings.closeBtn)
                    }
                }
            )
        }

        // 6. Play Direct Stream URL Dialog
        if (showStreamDialog) {
            AlertDialog(
                onDismissRequest = { showStreamDialog = false },
                title = { Text(strings.streamUrlDialogTitle) },
                text = {
                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = { streamUrlInput = it },
                        placeholder = { Text("https://example.com/video.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (streamUrlInput.isNotBlank()) {
                                onPlayStreamUrl(streamUrlInput.trim())
                            }
                            showStreamDialog = false
                        }
                    ) {
                        Text(strings.playNowBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStreamDialog = false }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }
    }
}

@Composable
private fun RetractableFloatingVideoOverlay(
    video: VideoItem,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExpandToFullPlayer: () -> Unit,
    onClose: () -> Unit,
    accentBlue: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val miniPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            if (video.lastPositionMs > 0L) seekTo(video.lastPositionMs)
            playWhenReady = true
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(miniPlayer) {
        onDispose {
            miniPlayer.release()
        }
    }

    val slideOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 214.dp,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = 340f
        ),
        label = "FloatingRetractSlide"
    )

    Row(
        modifier = modifier
            .offset { IntOffset(slideOffsetX.roundToPx(), dragOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-420f, 80f)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(118.dp)
                .shadow(8.dp, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .background(Color(0xFF141824).copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                )
                .clickable {
                    val nextState = !isExpanded
                    onToggleExpanded()
                    if (nextState && !miniPlayer.isPlaying) {
                        miniPlayer.play()
                        isPlaying = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(Capsule())
                        .background(if (isExpanded) accentBlue else Color.White.copy(alpha = 0.75f))
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = "Toggle Floating Bar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(214.dp)
                .height(126.dp)
                .shadow(12.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(Color.Black)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        miniPlayer.setVideoTextureView(this)
                    }
                },
                update = { tv ->
                    miniPlayer.setVideoTextureView(tv)
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .aspectRatio(video.nativeAspectRatio)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onExpandToFullPlayer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Floating",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .clickable {
                        if (miniPlayer.isPlaying) {
                            miniPlayer.pause()
                            isPlaying = false
                        } else {
                            miniPlayer.play()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CleanVideoCard(
    video: VideoItem,
    strings: UiStrings,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    onClick: () -> Unit,
    onStartFloating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onMoveToPrivate: () -> Unit,
    onDeleteToTrash: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 132.dp, height = 84.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = video.title,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.resolutionBadge,
                        color = accentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${video.formattedSize} • ${video.folderName}",
                    color = textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = strings.floatingPlayMenu,
                tint = accentBlue,
                modifier = Modifier
                    .size(19.dp)
                    .clickable(onClick = onStartFloating)
            )

            Box {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = textSecondary,
                    modifier = Modifier
                        .size(21.dp)
                        .clickable { menuExpanded = true }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.floatingPlayMenu) },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onStartFloating()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.addToPlaylistTitle) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (video.isFavorite) strings.favoriteMenuRemove else strings.favoriteMenuAdd) },
                        leadingIcon = {
                            Icon(
                                if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.moveToPrivateMenu) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveToPrivate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.deleteToTrashMenu) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            menuExpanded = false
                            onDeleteToTrash()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanFolderCard(
    folder: VideoFolder,
    strings: UiStrings,
    isRtl: Boolean,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = folder.name,
                tint = accentBlue,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${String.format(strings.videosCountFmt, folder.videos.size)} • ${folder.totalSizeFormatted}",
                color = textSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = textSecondary
        )
    }
}

@Composable
private fun SettingsCardContainer(
    cardColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    backdrop: Backdrop,
    textPrimary: Color,
    textSecondary: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = textSecondary, fontSize = 11.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop
        )
    }
}
