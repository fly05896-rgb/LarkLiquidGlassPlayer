package com.larkliquid.player.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.larkliquid.player.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-Speed Hardware Video Frame Thumbnail Extractor (`VideoThumbnailLoader.kt`).
 * Extracts the actual video frame from each user video on the device via Android's
 * `ContentResolver.loadThumbnail` (API 29+) or `MediaMetadataRetriever` fallback,
 * cached in an in-memory `LruCache` for 120Hz scrolling with zero fake poster images.
 */
object VideoThumbnailCache {
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(4096)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = memoryCache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (memoryCache.get(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    suspend fun loadVideoFrameThumbnail(
        context: Context,
        video: VideoItem,
        widthPx: Int = 360,
        heightPx: Int = 220
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${video.id}_${video.uri}"
        get(cacheKey)?.let { return@withContext it }

        val extracted: Bitmap? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && video.uri.scheme == "content") {
                context.contentResolver.loadThumbnail(video.uri, Size(widthPx, heightPx), null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    video.id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                )
            }
        }.getOrNull() ?: runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (video.uri.scheme == "content" || video.uri.scheme == "file") {
                    retriever.setDataSource(context, video.uri)
                } else {
                    retriever.setDataSource(video.uri.toString(), HashMap())
                }
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()

        if (extracted != null) {
            put(cacheKey, extracted)
        }
        extracted
    }
}

@Composable
fun VideoThumbnailImage(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cacheKey = remember(video.id, video.uri) { "${video.id}_${video.uri}" }
    var bitmap by remember(cacheKey) { mutableStateOf(VideoThumbnailCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            bitmap = VideoThumbnailCache.loadVideoFrameThumbnail(context, video)
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0B1120)
                    ),
                    start = Offset.Zero,
                    end = Offset(300f, 200f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                val path = Path().apply {
                    moveTo(size.width * 0.38f, size.height * 0.28f)
                    lineTo(size.width * 0.72f, size.height * 0.50f)
                    lineTo(size.width * 0.38f, size.height * 0.72f)
                    close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.45f))
            }
        }
    }
}
