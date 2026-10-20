package com.larkliquid.player.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.larkliquid.player.data.SortOption
import com.larkliquid.player.data.VideoFolder
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.ui.glass.LiquidCapsulePill
import com.larkliquid.player.ui.glass.LiquidCircleButton
import com.larkliquid.player.ui.glass.ios26LiquidGlass
import com.larkliquid.player.ui.glass.rememberLiquidInteractivePhysics
import kotlinx.coroutines.flow.collectLatest

enum class HomeTab(val labelAr: String, val labelEn: String) {
    ALL_VIDEOS("الفيديوهات", "Videos"),
    FOLDERS("المجلدات", "Folders"),
    FAVORITES("المفضلة", "Favorites"),
    PRIVATE_VAULT("الخزنة الخاصة", "Vault")
}

/**
 * Lark Video Main Library Screen with Real-Time iOS 26 Liquid Glass Scroll Refraction.
 * When scrolling through videos ("لما اقلب فيديوهات يظهر انكسار في القائمة الرئيسية"),
 * the video cards pass underneath the floating Liquid Glass Top Bar, Category Filter Pills,
 * and Bottom Navigation Bar, creating live SDF optical lens refraction & chromatic dispersion!
 */
@Composable
fun HomeLibraryScreen(
    videos: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onPlayStreamUrl: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onTogglePrivateVault: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.ALL_VIDEOS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showStreamDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    var scrollVelocity by remember { mutableFloatStateOf(0f) }

    // Track real-time scroll velocity to drive the AGSL Liquid Wave Refraction Shader
    LaunchedEffect(listState) {
        var lastOffset = 0
        var lastIndex = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val delta = (index - lastIndex) * 400 + (offset - lastOffset)
                scrollVelocity = delta.toFloat().coerceIn(-800f, 800f)
                lastIndex = index
                lastOffset = offset
            }
    }

    val filterChips = listOf("الكل • All", "4K HDR", "1080p FHD", "أفلام طويلة", "مقاطع قصيرة")

    val filteredVideos by remember(videos, selectedTab, searchQuery, selectedFilterIndex, sortOption) {
        derivedStateOf {
            videos.filter { item ->
                val matchesTab = when (selectedTab) {
                    HomeTab.ALL_VIDEOS -> !item.isPrivateVault
                    HomeTab.FOLDERS -> !item.isPrivateVault
                    HomeTab.FAVORITES -> item.isFavorite && !item.isPrivateVault
                    HomeTab.PRIVATE_VAULT -> item.isPrivateVault
                }
                val matchesSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.folderName.contains(searchQuery, ignoreCase = true)
                val matchesChip = when (selectedFilterIndex) {
                    1 -> item.resolutionBadge.contains("4K")
                    2 -> item.resolutionBadge.contains("1080p")
                    3 -> item.durationMs >= 300_000L
                    4 -> item.durationMs < 300_000L
                    else -> true
                }
                matchesTab && matchesSearch && matchesChip
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

    val folders by remember(filteredVideos) {
        derivedStateOf {
            filteredVideos.groupBy { it.folderName }
                .map { (name, items) -> VideoFolder(name, items) }
        }
    }

    // Main Backdrop layer that captures the scrolling video list for real-time optical refraction
    val mainMenuBackdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF060911))
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060911))
    ) {
        // ==================== LAYER 1: SCROLLING VIDEO CONTENT (Captured by mainMenuBackdrop) ====================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mainMenuBackdrop)
        ) {
            // Dynamic glowing ambient background orbs so refraction is vivid everywhere
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0077FF).copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.18f),
                        radius = size.minDimension * 0.7f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.55f),
                        radius = size.minDimension * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.75f, size.height * 0.88f),
                        radius = size.minDimension * 0.6f
                    )
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = 188.dp,
                    bottom = 124.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTab == HomeTab.FOLDERS) {
                    items(folders, key = { it.name }) { folder ->
                        FolderGlassCard(
                            folder = folder,
                            onSelectFolder = {
                                searchQuery = folder.name
                                selectedTab = HomeTab.ALL_VIDEOS
                            }
                        )
                    }
                } else {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoThumbnailCard(
                            video = video,
                            onClick = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video.id) },
                            onToggleVault = { onTogglePrivateVault(video.id) }
                        )
                    }
                }
            }
        }

        // ==================== LAYER 2: FLOATING REFRACTIVE LIQUID GLASS HEADER ====================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Refractive Glass Bar (Search + Stream URL + Sort)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidCapsulePill(
                    backdrop = mainMenuBackdrop,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    refractionHeight = 22.dp,
                    refractionAmount = 44.dp,
                    blurRadius = 4.dp,
                    scrollVelocity = scrollVelocity,
                    surfaceTint = Color.White.copy(alpha = 0.08f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث في Lark Liquid Glass...",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Play Network Stream URL Button (Liquid Glass Circle)
                LiquidCircleButton(
                    onClick = { showStreamDialog = true },
                    backdrop = mainMenuBackdrop,
                    size = 50.dp,
                    refractionHeight = 20.dp,
                    refractionAmount = 40.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Play URL",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Cycle Sort Option Button (Liquid Glass Circle)
                LiquidCircleButton(
                    onClick = {
                        val nextIdx = (sortOption.ordinal + 1) % SortOption.entries.size
                        sortOption = SortOption.entries[nextIdx]
                    },
                    backdrop = mainMenuBackdrop,
                    size = 50.dp,
                    refractionHeight = 20.dp,
                    refractionAmount = 40.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Refractive Category Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterChips.size) { index ->
                    val isSelected = selectedFilterIndex == index
                    LiquidCapsulePill(
                        backdrop = mainMenuBackdrop,
                        onClick = { selectedFilterIndex = index },
                        refractionHeight = 16.dp,
                        refractionAmount = 34.dp,
                        blurRadius = 3.dp,
                        scrollVelocity = scrollVelocity,
                        surfaceTint = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                        accentTint = if (isSelected) Color(0xFF0088FF) else Color.Unspecified
                    ) {
                        Text(
                            text = filterChips[index],
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ==================== LAYER 3: FLOATING REFRACTIVE iOS 26 BOTTOM NAVIGATION BAR ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .height(68.dp)
                .ios26LiquidGlass(
                    backdrop = mainMenuBackdrop,
                    shape = Capsule(),
                    blurRadius = 5.dp,
                    refractionHeight = 24.dp,
                    refractionAmount = 48.dp,
                    chromaticAberration = true,
                    depthEffect = true,
                    scrollVelocity = scrollVelocity,
                    surfaceTint = Color.White.copy(alpha = 0.09f)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val tabPhysics = rememberLiquidInteractivePhysics(
                    liftHeightDp = 11.dp,
                    maxScaleBoost = 0.16f
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .then(
                            if (isSelected) {
                                Modifier.ios26LiquidGlass(
                                    backdrop = mainMenuBackdrop,
                                    shape = Capsule(),
                                    physics = tabPhysics,
                                    blurRadius = 2.dp,
                                    refractionHeight = 18.dp,
                                    refractionAmount = 36.dp,
                                    surfaceTint = Color.White.copy(alpha = 0.18f),
                                    accentTint = Color(0xFF0091FF)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(interactionSource = null, indication = null) {
                            selectedTab = tab
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (tab) {
                        HomeTab.ALL_VIDEOS -> Icons.Default.VideoLibrary
                        HomeTab.FOLDERS -> Icons.Default.Folder
                        HomeTab.FAVORITES -> Icons.Default.Favorite
                        HomeTab.PRIVATE_VAULT -> Icons.Default.Lock
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.labelEn,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.labelAr,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStreamDialog) {
        AlertDialog(
            onDismissRequest = { showStreamDialog = false },
            title = { Text("تشغيل رابط فيديو مباشر (MP4 / HLS / M3U8)") },
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
                    Text("تشغيل الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStreamDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun VideoThumbnailCard(
    video: VideoItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleVault: () -> Unit
) {
    val cardBackdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF101626))
        drawContent()
    }
    val cardPhysics = rememberLiquidInteractivePhysics(
        liftHeightDp = 8.dp,
        maxScaleBoost = 0.04f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(video.accentColorHex).copy(alpha = 0.36f),
                        Color(0xFF131B2E),
                        Color(0xFF0B101D)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(cardBackdrop)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vivid Video Artwork Thumbnail with Refractive Play Badge
            Box(
                modifier = Modifier
                    .size(width = 132.dp, height = 88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(video.accentColorHex),
                                Color(0xFF0A1124)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Duration Pill in bottom-right of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(Capsule())
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(Color(video.accentColorHex).copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = video.resolutionBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${video.formattedSize} • ${video.folderName}",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) Color(0xFFFF3366) else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onToggleFavorite() }
                )
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Spatial Audio Ready",
                    tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleVault() }
                )
            }
        }
    }
}

@Composable
private fun FolderGlassCard(
    folder: VideoFolder,
    onSelectFolder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF152238), Color(0xFF0D1524))
                )
            )
            .clickable(onClick = onSelectFolder)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0091FF).copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = folder.name,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${folder.videos.size} فيديو • ${folder.totalSizeFormatted}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
        }
    }
}
