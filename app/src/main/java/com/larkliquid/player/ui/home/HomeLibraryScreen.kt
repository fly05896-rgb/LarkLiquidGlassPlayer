package com.larkliquid.player.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.larkliquid.player.R
import com.larkliquid.player.data.LiquidGlassStyleConfig
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
    PRIVATE_VAULT("الخزنة", "Vault"),
    GLASS_STUDIO("الزجاج السائل", "Glass Lab")
}

/**
 * Lark Video Main Library Screen with Real-Time iOS 26 Liquid Glass Scroll Refraction.
 * Features photographic video thumbnails (`posterResId`) so scrolling the list creates vivid
 * optical lens refraction across the floating Top Bar, Category Pills, Mini-Player, and Bottom Nav Bar!
 */
@Composable
fun HomeLibraryScreen(
    videos: List<VideoItem>,
    lastPlayedVideo: VideoItem?,
    glassConfig: LiquidGlassStyleConfig,
    onUpdateGlassConfig: (LiquidGlassStyleConfig) -> Unit,
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

    val filterChips = listOf("الكل • All", "بدون إنترنت • Offline", "4K HDR", "1080p FHD", "Spatial 3D")

    val filteredVideos by remember(videos, selectedTab, searchQuery, selectedFilterIndex, sortOption) {
        derivedStateOf {
            videos.filter { item ->
                val matchesTab = when (selectedTab) {
                    HomeTab.ALL_VIDEOS -> !item.isPrivateVault
                    HomeTab.FOLDERS -> !item.isPrivateVault
                    HomeTab.FAVORITES -> item.isFavorite && !item.isPrivateVault
                    HomeTab.PRIVATE_VAULT -> item.isPrivateVault
                    HomeTab.GLASS_STUDIO -> !item.isPrivateVault
                }
                val matchesSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.folderName.contains(searchQuery, ignoreCase = true)
                val matchesChip = when (selectedFilterIndex) {
                    1 -> item.isOfflineBuiltIn
                    2 -> item.resolutionBadge.contains("4K")
                    3 -> item.resolutionBadge.contains("1080p")
                    4 -> item.isFavorite
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(glassConfig.tintColorHex).copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.18f),
                        radius = size.minDimension * 0.72f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.55f),
                        radius = size.minDimension * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * 0.75f, size.height * 0.88f),
                        radius = size.minDimension * 0.6f
                    )
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = 192.dp,
                    bottom = if (lastPlayedVideo != null) 196.dp else 128.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    LibraryStatsBanner(
                        totalCount = videos.size,
                        offlineCount = videos.count { it.isOfflineBuiltIn },
                        sortLabel = sortOption.labelAr
                    )
                }

                when (selectedTab) {
                    HomeTab.FOLDERS -> {
                        items(folders, key = { it.name }) { folder ->
                            FolderGlassCard(
                                folder = folder,
                                onSelectFolder = {
                                    searchQuery = folder.name
                                    selectedTab = HomeTab.ALL_VIDEOS
                                }
                            )
                        }
                    }
                    HomeTab.GLASS_STUDIO -> {
                        item {
                            LiquidGlassStudioPanel(
                                config = glassConfig,
                                onUpdateConfig = onUpdateGlassConfig
                            )
                        }
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoThumbnailCard(
                                video = video,
                                onClick = { onPlayVideo(video) },
                                onToggleFavorite = { onToggleFavorite(video.id) },
                                onToggleVault = { onTogglePrivateVault(video.id) }
                            )
                        }
                    }
                    else -> {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidCapsulePill(
                    backdrop = mainMenuBackdrop,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    refractionHeight = (22f * glassConfig.refractionMultiplier).dp,
                    refractionAmount = (44f * glassConfig.refractionMultiplier).dp,
                    blurRadius = (4f * glassConfig.blurMultiplier).dp,
                    scrollVelocity = scrollVelocity,
                    surfaceTint = Color.White.copy(alpha = 0.08f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_ios26),
                        contentDescription = "iOS 26 Logo",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث في Lark Liquid Glass...",
                                color = Color.White.copy(alpha = 0.60f),
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
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                LiquidCircleButton(
                    onClick = { showStreamDialog = true },
                    backdrop = mainMenuBackdrop,
                    size = 50.dp,
                    refractionHeight = (20f * glassConfig.refractionMultiplier).dp,
                    refractionAmount = (40f * glassConfig.refractionMultiplier).dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Play URL",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                LiquidCircleButton(
                    onClick = {
                        val nextIdx = (sortOption.ordinal + 1) % SortOption.entries.size
                        sortOption = SortOption.entries[nextIdx]
                    },
                    backdrop = mainMenuBackdrop,
                    size = 50.dp,
                    refractionHeight = (20f * glassConfig.refractionMultiplier).dp,
                    refractionAmount = (40f * glassConfig.refractionMultiplier).dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterChips.size) { index ->
                    val isSelected = selectedFilterIndex == index
                    LiquidCapsulePill(
                        backdrop = mainMenuBackdrop,
                        onClick = { selectedFilterIndex = index },
                        refractionHeight = (16f * glassConfig.refractionMultiplier).dp,
                        refractionAmount = (34f * glassConfig.refractionMultiplier).dp,
                        blurRadius = (3f * glassConfig.blurMultiplier).dp,
                        scrollVelocity = scrollVelocity,
                        surfaceTint = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                        accentTint = if (isSelected) Color(glassConfig.tintColorHex) else Color.Unspecified
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

        // ==================== LAYER 3: FLOATING MINI-PLAYER + REFRACTIVE iOS 26 BOTTOM NAVIGATION BAR ====================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (lastPlayedVideo != null) {
                LiquidCapsulePill(
                    backdrop = mainMenuBackdrop,
                    onClick = { onPlayVideo(lastPlayedVideo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    refractionHeight = (20f * glassConfig.refractionMultiplier).dp,
                    refractionAmount = (42f * glassConfig.refractionMultiplier).dp,
                    scrollVelocity = scrollVelocity,
                    surfaceTint = Color.White.copy(alpha = 0.12f),
                    accentTint = Color(lastPlayedVideo.accentColorHex)
                ) {
                    Image(
                        painter = painterResource(id = lastPlayedVideo.posterResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "متابعة التشغيل الآن • Continue Watching",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lastPlayedVideo.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .ios26LiquidGlass(
                        backdrop = mainMenuBackdrop,
                        shape = Capsule(),
                        blurRadius = (5f * glassConfig.blurMultiplier).dp,
                        refractionHeight = (24f * glassConfig.refractionMultiplier).dp,
                        refractionAmount = (48f * glassConfig.refractionMultiplier).dp,
                        chromaticAberration = glassConfig.chromaticAberration,
                        depthEffect = true,
                        scrollVelocity = scrollVelocity,
                        surfaceTint = Color.White.copy(alpha = 0.09f)
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
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
                                        refractionHeight = (18f * glassConfig.refractionMultiplier).dp,
                                        refractionAmount = (36f * glassConfig.refractionMultiplier).dp,
                                        surfaceTint = Color.White.copy(alpha = 0.18f),
                                        accentTint = Color(glassConfig.tintColorHex)
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
                            HomeTab.GLASS_STUDIO -> Icons.Default.Tune
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.labelEn,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp)
                        )
                        AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
                            Row {
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = tab.labelAr,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
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
private fun LibraryStatsBanner(
    totalCount: Int,
    offlineCount: Int,
    sortLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0091FF).copy(alpha = 0.28f),
                        Color(0xFF7C4DFF).copy(alpha = 0.22f),
                        Color(0xFF0D1526)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$totalCount فيديو • $offlineCount مدمج بدون إنترنت HD",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = sortLabel,
            color = Color(0xFF00E5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LiquidGlassStudioPanel(
    config: LiquidGlassStyleConfig,
    onUpdateConfig: (LiquidGlassStyleConfig) -> Unit
) {
    val tints = listOf(
        0xFF0091FFL to "iOS Ice Blue",
        0xFF00E5FFL to "Crystal Cyan",
        0xFF7C4DFFL to "Cyber Violet",
        0xFF00E676L to "Emerald Glass",
        0xFFFF3366L to "Rose Quartz"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(config.tintColorHex).copy(alpha = 0.28f),
                        Color(0xFF10192E).copy(alpha = 0.92f)
                    )
                )
            )
            .border(1.2.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "مختبر عدسات الزجاج السائل (iOS 26 Liquid Glass Studio)",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "قوة انكسار العدسة المائية (SDF Refraction Power): ${(config.refractionMultiplier * 100).toInt()}%",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
        Slider(
            value = config.refractionMultiplier,
            onValueChange = { onUpdateConfig(config.copy(refractionMultiplier = it)) },
            valueRange = 0.5f..1.8f
        )

        Text(
            text = "درجة الضبابية الكريستالية (Glass Blur): ${(config.blurMultiplier * 100).toInt()}%",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
        Slider(
            value = config.blurMultiplier,
            onValueChange = { onUpdateConfig(config.copy(blurMultiplier = it)) },
            valueRange = 0.4f..2.0f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "التشتت اللوني الطيفي (RGB Chromatic Dispersion)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = config.chromaticAberration,
                onCheckedChange = { onUpdateConfig(config.copy(chromaticAberration = it)) }
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tints.size) { idx ->
                val (hex, name) = tints[idx]
                val selected = config.tintColorHex == hex
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Color(hex).copy(alpha = if (selected) 0.75f else 0.28f))
                        .clickable { onUpdateConfig(config.copy(tintColorHex = hex, presetName = name)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(video.accentColorHex).copy(alpha = 0.38f),
                        Color(0xFF131B2E),
                        Color(0xFF0B101D)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photographic Video Artwork Thumbnail with Refractive Liquid Glass Play Button
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 94.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = video.posterResId),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(cardBackdrop)
                )

                // Refractive Liquid Glass Play Circle over the poster thumbnail!
                LiquidCircleButton(
                    onClick = onClick,
                    backdrop = cardBackdrop,
                    size = 40.dp,
                    refractionHeight = 14.dp,
                    refractionAmount = 28.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Duration Pill in bottom-right of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(Capsule())
                        .background(Color.Black.copy(alpha = 0.68f))
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (video.isOfflineBuiltIn) {
                        Row(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(Color(0xFF00E676).copy(alpha = 0.28f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Offline",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(Color(video.accentColorHex).copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = video.resolutionBadge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = video.formattedSize,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    tint = Color(0xFF00E5FF).copy(alpha = 0.85f),
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
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
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
