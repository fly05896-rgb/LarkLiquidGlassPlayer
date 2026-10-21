package com.larkliquid.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Immutable
data class AppSettingsConfig(
    val isDarkMode: Boolean = true,
    val privateFolderPath: String = "Internal Storage/EGPlayerPrivateVault",
    val privateFolderUriString: String = "",
    val privatePinCode: String = "0000",
    val autoPlayNext: Boolean = true,
    val resumeLastPosition: Boolean = true,
    val backgroundAudioEnabled: Boolean = true,
    val hardwareDecoderPriority: Boolean = true,
    val defaultSkipSeconds: Int = 10
)

typealias LiquidGlassStyleConfig = AppSettingsConfig

@Immutable
data class VideoPlaylist(
    val id: Long,
    val name: String,
    val videoIds: List<Long> = emptyList(),
    val createdAtSec: Long = System.currentTimeMillis() / 1000L
)

@Immutable
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
    val isDeleted: Boolean = false,
    val deletedAtSec: Long = 0L,
    val lastPositionMs: Long = 0L,
    val accentColorHex: Long = 0xFF0088FF
) {
    val nativeAspectRatio: Float
        get() {
            if (width <= 0 || height <= 0) return 16f / 9f
            return (width.toFloat() / height.toFloat()).coerceIn(0.35f, 2.65f)
        }

    val resolutionBadge: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 2560 || height >= 1440 -> "2K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            else -> "HD"
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

@Immutable
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

enum class SortOption {
    DATE_DESC,
    NAME_ASC,
    DURATION_DESC,
    SIZE_DESC
}

class VideoRepository(private val context: Context) {

    /**
     * Scans real local device videos via Android MediaStore (`EXTERNAL_CONTENT_URI`).
     * Zero fake videos or fake poster images are used.
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
                            accentColorHex = 0xFF0088FF
                        )
                    )
                }
            }
        }

        list
    }

    fun defaultPlaylists(): List<VideoPlaylist> = listOf(
        VideoPlaylist(
            id = 1L,
            name = "المفضلة / Favorites",
            videoIds = emptyList()
        )
    )
}
