package com.larkliquid.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LiquidGlassStyleConfig(
    val presetName: String = "iOS 26 Crystal Default",
    val refractionMultiplier: Float = 1.0f,
    val blurMultiplier: Float = 1.0f,
    val chromaticAberration: Boolean = true,
    val tintColorHex: Long = 0xFF0091FF
)

data class VideoItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val folderName: String,
    val dateAddedSec: Long,
    val isFavorite: Boolean = false,
    val isPrivateVault: Boolean = false,
    val lastPositionMs: Long = 0L,
    val accentColorHex: Long = 0xFF0091FF
) {
    val resolutionBadge: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K HDR"
            width >= 2560 || height >= 1440 -> "2K QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            else -> "SD"
        }

    val formattedDuration: String
        get() = formatTimestamp(durationMs)

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

    companion object {
        fun formatTimestamp(ms: Long, prefixNegative: Boolean = false): String {
            val safeMs = ms.coerceAtLeast(0L)
            val totalSeconds = safeMs / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            val core = if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
            return if (prefixNegative) "-$core" else core
        }
    }
}

data class VideoFolder(
    val name: String,
    val videos: List<VideoItem>
) {
    val totalSizeFormatted: String
        get() {
            val totalBytes = videos.sumOf { it.sizeBytes }
            val mb = totalBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
}

enum class SortOption(val labelAr: String) {
    DATE_DESC("الأحدث أولاً"),
    NAME_ASC("الاسم (أ - ي)"),
    DURATION_DESC("الأطول مدة"),
    SIZE_DESC("الأكبر حجماً")
}

class VideoRepository(private val context: Context) {

    /**
     * Scans all videos on the device via Android MediaStore (Lark Video engine)
     * and appends 8 built-in 4K/HD showcase streams so the user can test iOS 26 Liquid Glass
     * refraction immediately even if the device/emulator has no local video files yet.
     */
    suspend fun loadVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                val palette = listOf(0xFF0091FF, 0xFF7C4DFF, 0xFF00E5FF, 0xFFFF3366, 0xFF00E676, 0xFFFF9100)
                var idx = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id.mp4"
                    val duration = cursor.getLong(durCol).coerceAtLeast(1000L)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val width = cursor.getInt(widthCol).takeIf { it > 0 } ?: 1920
                    val height = cursor.getInt(heightCol).takeIf { it > 0 } ?: 1080
                    val folder = cursor.getString(folderCol) ?: "Internal Storage"
                    val dateAdded = cursor.getLong(dateCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    list.add(
                        VideoItem(
                            id = id,
                            title = name.substringBeforeLast('.'),
                            uri = contentUri,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            folderName = folder,
                            dateAddedSec = dateAdded,
                            accentColorHex = palette[idx % palette.size]
                        )
                    )
                    idx++
                }
            }
        }

        list.addAll(builtInShowcaseStreams())
        list
    }

    fun builtInShowcaseStreams(): List<VideoItem> = listOf(
        VideoItem(
            id = -101L,
            title = "Sintel — Fantasy Wolf & Dragon (iOS 26 Reference Scene 1)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
            durationMs = 888_000L,
            sizeBytes = 129_400_000L,
            width = 3840,
            height = 2160,
            folderName = "iOS 26 Showcase 4K",
            dateAddedSec = 1760000800L,
            isFavorite = true,
            accentColorHex = 0xFF2979FF
        ),
        VideoItem(
            id = -102L,
            title = "Hello Desk Studio — Optical Glass Refraction (iOS 26 Reference Scene 2)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            durationMs = 15_000L,
            sizeBytes = 14_200_000L,
            width = 1920,
            height = 1080,
            folderName = "iOS 26 Showcase 4K",
            dateAddedSec = 1760000700L,
            isFavorite = true,
            accentColorHex = 0xFF00E5FF
        ),
        VideoItem(
            id = -103L,
            title = "Tears of Steel — Sci-Fi 4K HDR Spatial Audio Cinema",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            durationMs = 734_000L,
            sizeBytes = 185_000_000L,
            width = 3840,
            height = 2160,
            folderName = "Cinema 4K HDR",
            dateAddedSec = 1760000600L,
            isFavorite = true,
            accentColorHex = 0xFFAA00FF
        ),
        VideoItem(
            id = -104L,
            title = "Big Buck Bunny — 60fps Fluid Motion & Color Vibrancy",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            durationMs = 596_000L,
            sizeBytes = 158_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Cinema 4K HDR",
            dateAddedSec = 1760000500L,
            accentColorHex = 0xFF00E676
        ),
        VideoItem(
            id = -105L,
            title = "Elephants Dream — Surreal 3D Audio & Bass Boost Test",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            durationMs = 653_000L,
            sizeBytes = 142_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Spatial Audio Lab",
            dateAddedSec = 1760000400L,
            accentColorHex = 0xFFFF3366
        ),
        VideoItem(
            id = -106L,
            title = "For Bigger Escapes — Alpine Nature 4K Crystal HDR",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
            durationMs = 15_000L,
            sizeBytes = 18_500_000L,
            width = 3840,
            height = 2160,
            folderName = "iOS 26 Showcase 4K",
            dateAddedSec = 1760000300L,
            accentColorHex = 0xFF00B0FF
        ),
        VideoItem(
            id = -107L,
            title = "For Bigger Fun — High-Contrast Chromatic Dispersion Demo",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
            durationMs = 60_000L,
            sizeBytes = 42_000_000L,
            width = 1920,
            height = 1080,
            folderName = "Spatial Audio Lab",
            dateAddedSec = 1760000200L,
            accentColorHex = 0xFFFF9100
        ),
        VideoItem(
            id = -108L,
            title = "Subaru Outback & Bullrun — Fast Panning Motion Refraction",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"),
            durationMs = 47_000L,
            sizeBytes = 36_800_000L,
            width = 1920,
            height = 1080,
            folderName = "Action 60FPS",
            dateAddedSec = 1760000100L,
            accentColorHex = 0xFF651FFF
        )
    )
}
