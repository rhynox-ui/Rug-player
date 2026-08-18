# Rug Player

A native Android video player — the MX Player experience, rebuilt clean: no ads, no "unlock with a subscription" walls, no bundled analytics SDKs. Just your videos.

Built with Kotlin, Jetpack Compose, and Media3 (ExoPlayer).

## Why this exists

MX Player shipped ads in the player chrome, in the library, and between actions, plus a pile of third-party trackers. Rug Player is a from-scratch clone of the parts people actually want — the library, the gesture controls, the subtitle handling — with all of that stripped out and a few things modern players still get wrong fixed.

## Features

**Library**
- Auto-scans on-device videos via MediaStore — no manual folder import needed
- **Folders view** (the default, like MX Player): every folder with videos listed vertically with a thumbnail and count; tap in for a vertical file list. Toggle to a flat "All videos" grid any time.
- Search, sort (date / name / duration / size)
- "Continue watching" row for videos you're partway through
- Multi-select (long-press) to delete videos in bulk, from search results or inside a folder
- Thumbnails generated on-device, nothing uploaded anywhere

**Playback**
- Hardware-accelerated decoding via Media3/ExoPlayer (broad codec/container support, including MKV)
- **Forces landscape fullscreen the moment a video opens** — regardless of your phone's rotation-lock setting — and switches to portrait automatically for portrait-shot clips
- Gesture controls: swipe left half for brightness, right half for volume, drag anywhere to scrub, double-tap either edge for ±10s
- Playback speed (0.25×–3×), zoom/aspect-ratio toggle (fit/zoom/fill)
- Resume playback automatically where you left off
- Screen lock to ignore stray touches
- A–B repeat for looping a section
- Sleep timer
- Picture-in-picture
- Background/audio-only playback with real lock-screen media controls (via a MediaSession-backed playback service) — not a paywalled feature

**Subtitles**
- Embedded track selection (MKV/MP4 soft subs)
- Load external `.srt`/`.vtt`/`.ass`/`.ssa` files on the fly

**Send & Receive**
- Send a video directly to another phone on the same Wi-Fi network — no cloud, no account, no size limit tied to a chat app. One device serves the file over a local HTTP link; the other pastes the link and pulls it straight into its library.
- **Stream tab**: paste any direct video URL and play it immediately, no download step.

**Status Saver**
- Point Rug Player at WhatsApp's (or WhatsApp Business's) status folder once via the system file picker, and save any status video before it expires — copied straight into your library, no re-download later.

**Privacy**
- No ad SDK, no analytics, no account system, no telemetry of any kind
- The *only* network permission in the app exists for the optional Send & Receive screen; nothing else in Rug Player talks to a network at all
- Settings and playback positions live in local Room/DataStore storage; nothing leaves the device unless you explicitly send a file to another phone

## Project structure

```
app/src/main/kotlin/com/rugplayer/app/
├── data/
│   ├── model/        VideoItem
│   ├── repository/   VideoRepository (MediaStore)
│   ├── db/            Room: playback positions
│   └── prefs/         DataStore: app settings
├── player/            PlaybackService (MediaSessionService), MediaController connection
├── transfer/           Local HTTP server + client for Send & Receive
└── ui/
    ├── library/        Library screen + view model
    ├── player/          Player screen, gesture overlay, controls, sheets
    ├── settings/        Settings screen
    ├── transfer/        Send & Receive screen
    └── theme/           Material3 theme (dynamic color on Android 12+)
```

## Building the APK

This repo builds with the standard Android Gradle Plugin — no special setup beyond a JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/
```

A GitHub Actions workflow (`.github/workflows/android-build.yml`) builds a debug APK on every push and uploads it as a workflow artifact, so you can grab an installable APK straight from the **Actions** tab without a local Android SDK.

### Minimum requirements
- Android 8.0 (API 26) and up
- Kotlin 2.0, Compose BOM 2024.12, Media3 1.5

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE` | Build your local video library |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*` | Show playback controls while backgrounded |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Power the optional Send & Receive screen (local Wi-Fi transfer only — used nowhere else in the app) |
| `WRITE_EXTERNAL_STORAGE` (API ≤28 only) | Save a received file into the shared Movies folder on older Android versions |

No `com.google.android.gms.*`, no ad network SDK, no crash/analytics reporter.
