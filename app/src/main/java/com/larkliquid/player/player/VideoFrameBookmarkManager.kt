package com.larkliquid.player.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.view.TextureView
import androidx.compose.runtime.Immutable
import com.larkliquid.player.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class VideoBookmarkItem(
    val id: Long,
    val videoId: Long,
    val positionMs: Long,
    val label: String
)

/**
 * 2026 Video Frame Snapshot & Smart Bookmark Engine (`VideoFrameBookmarkManager.kt`).
 */
object VideoFrameBookmarkManager {

    suspend fun captureCurrentVideoFrameToGallery(
        context: Context,
        textureView: TextureView?,
        video: VideoItem,
        positionMs: Long
    ): String? = withContext(Dispatchers.IO) {
        val bitmap: Bitmap = runCatching { textureView?.bitmap }.getOrNull() ?: return@withContext null
        val timestampLabel = VideoItem.formatTimestamp(positionMs).replace(":", "-")
        val fileName = "EGPlayer_${video.title.take(20).replace(" ", "_")}_$timestampLabel.jpg"

        runCatching {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EGPlayer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@runCatching null

            resolver.openOutputStream(uri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            fileName
        }.getOrNull()
    }

    fun createBookmark(
        videoId: Long,
        positionMs: Long,
        index: Int
    ): VideoBookmarkItem {
        val formatted = VideoItem.formatTimestamp(positionMs)
        return VideoBookmarkItem(
            id = System.currentTimeMillis(),
            videoId = videoId,
            positionMs = positionMs,
            label = "#$index • $formatted"
        )
    }

    fun bookmarksForVideo(
        allBookmarks: List<VideoBookmarkItem>,
        videoId: Long
    ): List<VideoBookmarkItem> = allBookmarks
        .filter { it.videoId == videoId }
        .sortedBy { it.positionMs }
}
