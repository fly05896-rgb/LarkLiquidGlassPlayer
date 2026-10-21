package com.larkliquid.player.player

import androidx.compose.runtime.Immutable

@Immutable
data class Media3CodecSpec(
    val codecId: String,
    val mimeType: String,
    val containerFormat: String,
    val profileLevel: String,
    val colorSpaceStandard: String,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int,
    val maxFrameRateFps: Int,
    val hardwareAccelerated: Boolean,
    val descriptionAr: String,
    val descriptionEn: String
)

object Media3CodecRegistry {
    val supportedVideoCodecs: List<Media3CodecSpec> = listOf(
        Media3CodecSpec(
            codecId = "HEVC_HDR10",
            mimeType = "video/hevc",
            containerFormat = "MP4/MKV",
            profileLevel = "Main 10 L5.1",
            colorSpaceStandard = "BT.2020",
            maxSupportedWidth = 3840,
            maxSupportedHeight = 2160,
            maxFrameRateFps = 120,
            hardwareAccelerated = true,
            descriptionAr = "دعم HEVC 4K",
            descriptionEn = "4K HEVC HDR10"
        ),
        Media3CodecSpec(
            codecId = "AVC_1080P",
            mimeType = "video/avc",
            containerFormat = "MP4/MKV",
            profileLevel = "High L4.2",
            colorSpaceStandard = "BT.709",
            maxSupportedWidth = 1920,
            maxSupportedHeight = 1080,
            maxFrameRateFps = 120,
            hardwareAccelerated = true,
            descriptionAr = "دعم H.264",
            descriptionEn = "H.264 AVC"
        )
    )
}
