package com.larkliquid.player.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

@Immutable
data class SubtitleCueItem(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

/**
 * Multi-Format Subtitle Parser & Real-Time Cue Synchronizer (`SubtitleParserEngine.kt`).
 * Supports external `.srt` (SubRip), `.vtt` (WebVTT), and `.ass`/`.ssa` subtitle files
 * loaded via Android Storage Access Framework (`OpenDocument`).
 */
object SubtitleParserEngine {

    private val SRT_TIMESTAMP_REGEX = Pattern.compile(
        "(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})"
    )

    private val VTT_SHORT_TIMESTAMP_REGEX = Pattern.compile(
        "(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2})\\.(\\d{3})"
    )

    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val ASS_BRACE_REGEX = Regex("\\u007B[^\\u007D]*\\u007D")

    suspend fun parseSubtitleUri(context: Context, uri: Uri): List<SubtitleCueItem> = withContext(Dispatchers.IO) {
        val rawLines = mutableListOf<String>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        rawLines.add(line!!)
                        line = reader.readLine()
                    }
                }
            }
        }
        parseRawLines(rawLines)
    }

    fun parseRawLines(lines: List<String>): List<SubtitleCueItem> {
        val cues = mutableListOf<SubtitleCueItem>()
        var i = 0
        val n = lines.size

        while (i < n) {
            val trimmed = lines[i].trim().removePrefix("\uFEFF")
            if (trimmed.isEmpty() || trimmed.equals("WEBVTT", ignoreCase = true)) {
                i++
                continue
            }

            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val parts = trimmed.substringAfter("Dialogue:").split(",", limit = 10)
                if (parts.size >= 10) {
                    val startMs = parseAssTimestamp(parts[1].trim())
                    val endMs = parseAssTimestamp(parts[2].trim())
                    val cleanText = parts[9]
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .replace(ASS_BRACE_REGEX, "")
                        .trim()
                    if (endMs > startMs && cleanText.isNotEmpty()) {
                        cues.add(SubtitleCueItem(startMs, endMs, cleanText))
                    }
                }
                i++
                continue
            }

            var timeLine = trimmed
            var matcher = SRT_TIMESTAMP_REGEX.matcher(timeLine)
            var shortMatcher = VTT_SHORT_TIMESTAMP_REGEX.matcher(timeLine)

            if (!matcher.find() && !shortMatcher.find() && i + 1 < n) {
                val nextLine = lines[i + 1].trim()
                val nextFull = SRT_TIMESTAMP_REGEX.matcher(nextLine)
                val nextShort = VTT_SHORT_TIMESTAMP_REGEX.matcher(nextLine)
                if (nextFull.find() || nextShort.find()) {
                    i++
                    timeLine = nextLine
                    matcher = SRT_TIMESTAMP_REGEX.matcher(timeLine)
                    shortMatcher = VTT_SHORT_TIMESTAMP_REGEX.matcher(timeLine)
                }
            } else {
                matcher = SRT_TIMESTAMP_REGEX.matcher(timeLine)
                shortMatcher = VTT_SHORT_TIMESTAMP_REGEX.matcher(timeLine)
            }

            var startMs = -1L
            var endMs = -1L

            if (matcher.find()) {
                startMs = toMillis(
                    matcher.group(1)?.toLongOrNull() ?: 0L,
                    matcher.group(2)?.toLongOrNull() ?: 0L,
                    matcher.group(3)?.toLongOrNull() ?: 0L,
                    matcher.group(4)?.toLongOrNull() ?: 0L
                )
                endMs = toMillis(
                    matcher.group(5)?.toLongOrNull() ?: 0L,
                    matcher.group(6)?.toLongOrNull() ?: 0L,
                    matcher.group(7)?.toLongOrNull() ?: 0L,
                    matcher.group(8)?.toLongOrNull() ?: 0L
                )
            } else if (shortMatcher.find()) {
                startMs = toMillis(
                    0L,
                    shortMatcher.group(1)?.toLongOrNull() ?: 0L,
                    shortMatcher.group(2)?.toLongOrNull() ?: 0L,
                    shortMatcher.group(3)?.toLongOrNull() ?: 0L
                )
                endMs = toMillis(
                    0L,
                    shortMatcher.group(4)?.toLongOrNull() ?: 0L,
                    shortMatcher.group(5)?.toLongOrNull() ?: 0L,
                    shortMatcher.group(6)?.toLongOrNull() ?: 0L
                )
            }

            if (startMs >= 0L && endMs > startMs) {
                i++
                val textBuilder = StringBuilder()
                while (i < n && lines[i].trim().isNotEmpty()) {
                    val cleaned = lines[i].trim()
                        .replace(HTML_TAG_REGEX, "")
                        .replace(ASS_BRACE_REGEX, "")
                    if (cleaned.isNotEmpty()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(cleaned)
                    }
                    i++
                }
                val finalCueText = textBuilder.toString().trim()
                if (finalCueText.isNotEmpty()) {
                    cues.add(SubtitleCueItem(startMs, endMs, finalCueText))
                }
            } else {
                i++
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    /**
     * Binary search lookup for active subtitle cue at `positionMs + offsetMs`.
     */
    fun findActiveCueText(
        cues: List<SubtitleCueItem>,
        positionMs: Long,
        syncOffsetMs: Long = 0L
    ): String? {
        if (cues.isEmpty()) return null
        val adjustedPos = (positionMs - syncOffsetMs).coerceAtLeast(0L)

        var low = 0
        var high = cues.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            when {
                adjustedPos < cue.startTimeMs -> high = mid - 1
                adjustedPos > cue.endTimeMs -> low = mid + 1
                else -> return cue.text
            }
        }
        return null
    }

    private fun parseAssTimestamp(raw: String): Long {
        val parts = raw.split(":", ".")
        if (parts.size < 4) return 0L
        val h = parts[0].toLongOrNull() ?: 0L
        val m = parts[1].toLongOrNull() ?: 0L
        val s = parts[2].toLongOrNull() ?: 0L
        val cs = parts[3].toLongOrNull() ?: 0L
        return toMillis(h, m, s, cs * 10L)
    }

    private fun toMillis(h: Long, m: Long, s: Long, ms: Long): Long {
        return (((h * 60L + m) * 60L) + s) * 1000L + ms
    }
}
