# EG Player — AndroidX Media3 + Liquid Glass Video Player

Production Android video player built with **Kotlin 2.1 (K2)**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and the **AndroidLiquidGlass (`com.kyant.backdrop`)** real-time optical refraction engine.

## Key Features
- **Authentic Liquid Glass Navigation & Search Bars**: Real-time background blur, vibrancy, and SDF lens refraction powered by `com.kyant.backdrop` and `LiquidBottomTabs`.
- **Automatic Phone Language Detection**: Automatically detects the device language and layout direction (`RTL` / `LTR`) via `LocalConfiguration.current`.
- **Full iOS Video Player Experience**:
  - Previous and Next video buttons + In-player Up Next Queue drawer.
  - Automatic transition to the next video when playback finishes (`Player.STATE_ENDED`).
  - 10-second circular skip buttons (`10`), morphing Play/Pause icon, and animated vector 2.0x speed chevrons on long-press (zero emojis).
  - Vertical drag fine-scrubbing precision (`High-Speed`, `Half-Speed`, `Quarter-Speed`, `Fine Scrubbing`).
  - External `.srt` / `.vtt` / `.ass` subtitle loader, 200% audio booster & 5-band equalizer, A-B repeat, sleep timer, horizontal mirror flip, Picture-in-Picture, and retractable floating player.
- **Single APK GitHub Actions Workflow**: `.github/workflows/build-apk.yml` compiles and packages a single ready-to-install `EGPlayer.apk`.
