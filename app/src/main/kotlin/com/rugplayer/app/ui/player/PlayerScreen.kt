package com.rugplayer.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rugplayer.app.AppGraph
import com.rugplayer.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    graph: AppGraph,
    onBack: () -> Unit,
    initialVideoId: Long? = null,
    streamUrl: String? = null,
    streamTitle: String? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val viewModel: PlayerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    graph.videoRepository,
                    graph.playbackPositionDao,
                    graph.settingsRepository,
                    graph.playbackController,
                )
            }
        },
    )

    LaunchedEffect(initialVideoId, streamUrl) {
        if (streamUrl != null) {
            viewModel.startStream(streamUrl, streamTitle ?: "Network stream")
        } else if (initialVideoId != null) {
            viewModel.start(initialVideoId)
        }
    }

    val state by viewModel.uiState.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    var brightness by remember { mutableFloatStateOf(0.5f) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var volumeFraction by remember { mutableFloatStateOf(0.5f) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var seekDeltaSeconds by remember { mutableIntStateOf(0) }
    var showSeekHud by remember { mutableStateOf(false) }
    var seekDragAccumulatedMs by remember { mutableFloatStateOf(0f) }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
        )
    }
    var resizeModeIndex by remember { mutableIntStateOf(0) }

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    LaunchedEffect(Unit) {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        volumeFraction = if (maxVolume > 0) current.toFloat() / maxVolume else 0f
        activity?.window?.attributes?.screenBrightness?.let {
            if (it in 0f..1f) brightness = it
        }
    }

    // Auto-hide controls while playing.
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    KeepScreenOn(enabled = state.isPlaying)
    ImmersiveMode(hideSystemBars = !controlsVisible)
    ForceOrientationForPlayback(isPortraitVideo = state.isPortraitVideo)

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "subtitle"
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.addExternalSubtitle(uri, name)
        }
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    playerViewRef = this
                }
            },
            update = { playerView ->
                playerView.player = viewModel.controller
                playerView.resizeMode = resizeModes[resizeModeIndex]
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        // Safety net: if a frame still hasn't rendered a couple seconds
        // after playback is ready, re-poke the player reference to force
        // PlayerView to redo its surface attachment.
        LaunchedEffect(state.isReady, viewModel.controller) {
            if (!state.isReady) return@LaunchedEffect
            delay(2000)
            if (!state.firstFrameRendered && state.errorMessage == null) {
                val playerView = playerViewRef
                val currentController = viewModel.controller
                if (playerView != null && currentController != null) {
                    playerView.player = null
                    playerView.player = currentController
                }
            }
        }

        GestureOverlay(
            enabled = !state.locked,
            onTap = { controlsVisible = !controlsVisible },
            onDoubleTapSeek = { forward ->
                viewModel.seekBy(if (forward) 10_000 else -10_000)
                seekDeltaSeconds = if (forward) 10 else -10
                showSeekHud = true
                scope.launch {
                    delay(500)
                    showSeekHud = false
                }
            },
            onBrightnessDelta = { delta ->
                brightness = (brightness + delta).coerceIn(0f, 1f)
                activity?.let { act ->
                    val params = act.window.attributes
                    params.screenBrightness = brightness
                    act.window.attributes = params
                }
                showBrightnessHud = true
            },
            onVolumeDelta = { delta ->
                volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                audioManager?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (volumeFraction * maxVolume).toInt(),
                    0,
                )
                showVolumeHud = true
            },
            onSeekDragStart = {
                seekDragAccumulatedMs = 0f
                showSeekHud = true
            },
            onSeekDrag = { deltaMs ->
                seekDragAccumulatedMs += deltaMs
                seekDeltaSeconds = (seekDragAccumulatedMs / 1000).toInt()
            },
            onSeekDragEnd = {
                if (seekDragAccumulatedMs != 0f) viewModel.seekBy(seekDragAccumulatedMs.toLong())
                showSeekHud = false
            },
        )

        LaunchedEffect(showBrightnessHud) {
            if (showBrightnessHud) {
                delay(700)
                showBrightnessHud = false
            }
        }
        LaunchedEffect(showVolumeHud) {
            if (showVolumeHud) {
                delay(700)
                showVolumeHud = false
            }
        }

        if (state.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        GestureHud(
            icon = Icons.Filled.Brightness6,
            value = brightness,
            visible = showBrightnessHud,
            alignment = Alignment.CenterStart,
        )
        GestureHud(
            icon = Icons.Filled.VolumeUp,
            value = volumeFraction,
            visible = showVolumeHud,
            alignment = Alignment.CenterEnd,
        )
        SeekHud(deltaSeconds = seekDeltaSeconds, visible = showSeekHud)

        if (state.locked) {
            LockedOverlay(onUnlock = { viewModel.toggleLock() }, modifier = Modifier.align(Alignment.CenterStart))
        } else {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                PlayerTopBar(
                    title = state.current?.title ?: state.streamTitle ?: "",
                    onBack = onBack,
                    onSubtitles = { showSubtitleSheet = true },
                    onSpeed = { showSpeedSheet = true },
                    onSleepTimer = { showSleepTimerSheet = true },
                    onLock = { viewModel.toggleLock() },
                    onToggleZoom = { resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size },
                )
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                PlayerBottomBar(
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    bufferedMs = state.bufferedPositionMs,
                    abStartMs = state.abRepeatStartMs,
                    abEndMs = state.abRepeatEndMs,
                    hasNext = state.currentIndex < state.queue.lastIndex,
                    hasPrevious = state.currentIndex > 0,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onNext = viewModel::playNext,
                    onPrevious = viewModel::playPrevious,
                    onPip = { enterPip(activity) },
                    onAbRepeat = viewModel::setAbRepeatPoint,
                )
            }
        }
    }

    if (showSpeedSheet) {
        SpeedSheet(
            currentSpeed = state.speed,
            onSelect = {
                viewModel.setSpeed(it)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false },
        )
    }

    if (showSubtitleSheet) {
        SubtitleSheet(
            tracks = state.subtitleTracks,
            selectedId = state.selectedSubtitleId,
            onSelect = {
                viewModel.selectSubtitle(it)
                showSubtitleSheet = false
            },
            onAddExternal = {
                subtitlePicker.launch(arrayOf("*/*"))
                showSubtitleSheet = false
            },
            onDismiss = { showSubtitleSheet = false },
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            activeEndAtMs = state.sleepTimerEndAtMs,
            onSelectMinutes = {
                viewModel.startSleepTimer(it * 60_000L)
                showSleepTimerSheet = false
            },
            onCancel = {
                viewModel.cancelSleepTimer()
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false },
        )
    }
}

private fun enterPip(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
    activity.enterPictureInPictureMode(params)
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) return it.getString(nameIndex)
    }
    return null
}

/**
 * Forces the player into fullscreen landscape the moment a video opens,
 * regardless of the device's rotation-lock setting — matching the "just
 * play it sideways" behavior of most video player apps. Switches to
 * portrait for portrait-shot clips once the real dimensions are known, and
 * releases the forced orientation entirely when leaving the player.
 */
@Composable
private fun ForceOrientationForPlayback(isPortraitVideo: Boolean?) {
    val activity = LocalContext.current as? Activity ?: return

    DisposableEffect(Unit) {
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    LaunchedEffect(isPortraitVideo) {
        val target = if (isPortraitVideo == true) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        if (activity.requestedOrientation == target) return@LaunchedEffect

        // Forcing the orientation change in the same composition pass that
        // creates PlayerView's video surface races the surface attachment —
        // the window rotates/relays out while the surface is still being
        // set up, and it can end up never bound (audio plays, frame never
        // renders). Give the surface a moment to attach first.
        delay(180)
        activity.requestedOrientation = target
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        if (enabled) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun ImmersiveMode(hideSystemBars: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    val view = LocalView.current
    DisposableEffect(hideSystemBars) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        if (hideSystemBars) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
