package com.ghostreborn.akira.features.player.screens

import android.net.Uri
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun TVPlayerScreen(
    animeId: String,
    animeTitle: String,
    episodeNumber: String,
    sources: List<StreamSource>,
    thumbnail: String? = null,
    initialPositionMs: Long = 0L,
    episodes: List<String> = emptyList(),
    translationType: String = "sub",
    onNavigateBack: () -> Unit,
    onChangeEpisode: (String, List<StreamSource>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val playableSources = remember(sources) { sources.filter { !it.url.isNullOrBlank() } }
    var currentSourceIndex by remember { mutableIntStateOf(0) }

    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var controlsInteractionTrigger by remember { mutableLongStateOf(0L) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val serverFirstFocusRequester = remember { FocusRequester() }
    val episodeFirstFocusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:155.0) Gecko/20100101 Firefox/155.0")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to com.ghostreborn.akira.core.config.MkissaConfigProvider.currentEndpoints.authReferer,
                    "Origin" to com.ghostreborn.akira.core.config.MkissaConfigProvider.currentEndpoints.authOrigin
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .build()
    }

    fun playSource(index: Int, resumePosMs: Long = 0L) {
        if (playableSources.isEmpty() || index >= playableSources.size) return
        currentSourceIndex = index
        val targetSource = playableSources[index]

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(targetSource.url))
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (resumePosMs > 0) {
                exoPlayer.seekTo(resumePosMs)
            }
            exoPlayer.playWhenReady = true
            isPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "Stream error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetControlsTimer() {
        controlsInteractionTrigger = System.currentTimeMillis()
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        currentPositionMs = target
        showControls = true
        resetControlsTimer()
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    val pos = exoPlayer.currentPosition
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    if (pos > 0 && dur > 0) {
                        WatchHistoryManager.saveProgress(
                            context = context,
                            animeId = animeId,
                            animeTitle = animeTitle,
                            episodeNumber = episodeNumber,
                            seekPositionMs = pos,
                            totalDurationMs = dur,
                            thumbnail = thumbnail
                        )
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> isLoading = false
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                val next = currentSourceIndex + 1
                if (next < playableSources.size) {
                    playSource(next, exoPlayer.currentPosition)
                } else {
                    Toast.makeText(context, "Playback failed on all servers", Toast.LENGTH_SHORT).show()
                }
            }
        })

        playSource(0, initialPositionMs)
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000L)
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (currentPositionMs > 0 && totalDurationMs > 0) {
                    WatchHistoryManager.saveProgress(
                        context = context,
                        animeId = animeId,
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        seekPositionMs = currentPositionMs,
                        totalDurationMs = totalDurationMs,
                        thumbnail = thumbnail
                    )
                }
            }
        }
    }

    // Auto-hide controls with reset on interaction
    LaunchedEffect(showControls, controlsInteractionTrigger) {
        if (showControls) {
            delay(5000L)
            if (!showServerDialog && !showEpisodesDrawer) {
                showControls = false
            }
        }
    }

    // Auto-focus on play/pause or dialogs when shown
    LaunchedEffect(showControls) {
        if (showControls && !showServerDialog && !showEpisodesDrawer) {
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showServerDialog) {
        if (showServerDialog) {
            delay(100L)
            try {
                serverFirstFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showEpisodesDrawer) {
        if (showEpisodesDrawer) {
            delay(100L)
            try {
                episodeFirstFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    BackHandler {
        if (showEpisodesDrawer) {
            showEpisodesDrawer = false
            showControls = true
            resetControlsTimer()
        } else if (showServerDialog) {
            showServerDialog = false
            showControls = true
            resetControlsTimer()
        } else if (showControls) {
            showControls = false
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    resetControlsTimer()

                    // Always handle dedicated media remote keys
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            exoPlayer.play()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            exoPlayer.pause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            seekBy(10000L)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seekBy(-10000L)
                            true
                        }
                        // Handle D-Pad when controls are hidden
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showControls) {
                                seekBy(-10000L)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showControls) {
                                seekBy(10000L)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!showControls) {
                                showControls = true
                                showEpisodesDrawer = true
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Loading video..."
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AkiraPrimary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val backInteraction = remember { MutableInteractionSource() }
                        val isBackFocused by backInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(if (isBackFocused) 2.dp else 0.dp, if (isBackFocused) Color.White else Color.Transparent, CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = "Back"
                                }
                                .focusable(interactionSource = backInteraction)
                                .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                                .clickable(interactionSource = backInteraction, indication = null) {
                                    onNavigateBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = animeTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episode $episodeNumber ($translationType)",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Episodes button
                        if (episodes.isNotEmpty()) {
                            val epBtnInteraction = remember { MutableInteractionSource() }
                            val isEpBtnFocused by epBtnInteraction.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(if (isEpBtnFocused) 2.dp else 0.dp, if (isEpBtnFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                    .background(if (isEpBtnFocused) AkiraPrimary else Color.White.copy(alpha = 0.15f))
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        contentDescription = "View Episodes List"
                                    }
                                    .focusable(interactionSource = epBtnInteraction)
                                    .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                                    .clickable(interactionSource = epBtnInteraction, indication = null) {
                                        showEpisodesDrawer = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Episodes",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Server button
                        val serverInteraction = remember { MutableInteractionSource() }
                        val isServerFocused by serverInteraction.collectIsFocusedAsState()
                        val currentServerName = playableSources.getOrNull(currentSourceIndex)?.sourceName ?: "Server"

                        val serverBgMod = if (isServerFocused) Modifier.background(brush = AkiraLogoGradientBrush) else Modifier.background(color = Color.White.copy(alpha = 0.15f))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(if (isServerFocused) 2.dp else 0.dp, if (isServerFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .then(serverBgMod)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = "Change Server, Current: $currentServerName"
                                }
                                .focusable(interactionSource = serverInteraction)
                                .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                                .clickable(interactionSource = serverInteraction, indication = null) {
                                    showServerDialog = true
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentServerName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rewInteraction = remember { MutableInteractionSource() }
                    val isRewFocused by rewInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(if (isRewFocused) 2.5.dp else 0.dp, if (isRewFocused) Color.White else Color.Transparent, CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Rewind 10 seconds"
                            }
                            .focusable(interactionSource = rewInteraction)
                            .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                            .clickable(interactionSource = rewInteraction, indication = null) { seekBy(-10000L) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    val playInteraction = remember { MutableInteractionSource() }
                    val isPlayFocused by playInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(if (isPlayFocused) 3.dp else 0.dp, if (isPlayFocused) Color.White else Color.Transparent, CircleShape)
                            .background(brush = AkiraLogoGradientBrush)
                            .focusRequester(playPauseFocusRequester)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = if (isPlaying) "Pause video" else "Play video"
                            }
                            .focusable(interactionSource = playInteraction)
                            .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                            .clickable(interactionSource = playInteraction, indication = null) {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                resetControlsTimer()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    val fwdInteraction = remember { MutableInteractionSource() }
                    val isFwdFocused by fwdInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(if (isFwdFocused) 2.5.dp else 0.dp, if (isFwdFocused) Color.White else Color.Transparent, CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Fast forward 10 seconds"
                            }
                            .focusable(interactionSource = fwdInteraction)
                            .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                            .clickable(interactionSource = fwdInteraction, indication = null) { seekBy(10000L) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    val skipInteraction = remember { MutableInteractionSource() }
                    val isSkipFocused by skipInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(if (isSkipFocused) 2.dp else 0.dp, if (isSkipFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Skip 85 seconds intro"
                            }
                            .focusable(interactionSource = skipInteraction)
                            .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                            .clickable(interactionSource = skipInteraction, indication = null) { seekBy(85000L) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+85s Intro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Bottom Timeline & Interactive Scrub Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    val progressFraction = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    val timelineInteraction = remember { MutableInteractionSource() }
                    val isTimelineFocused by timelineInteraction.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(
                                width = if (isTimelineFocused) 2.dp else 0.dp,
                                color = if (isTimelineFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Progress bar: ${formatTime(currentPositionMs)} of ${formatTime(totalDurationMs)}. Press left or right to scrub."
                            }
                            .focusable(interactionSource = timelineInteraction)
                            .onFocusChanged { if (it.isFocused) resetControlsTimer() }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            seekBy(-10000L)
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            seekBy(10000L)
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTimelineFocused) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(if (isTimelineFocused) 10.dp else 6.dp)
                                    .background(brush = AkiraLogoGradientBrush)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(totalDurationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Server Selection Dialog Modal
        if (showServerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showServerDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161424))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Select Server", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    playableSources.forEachIndexed { idx, src ->
                        val isSelected = currentSourceIndex == idx
                        val interaction = remember { MutableInteractionSource() }
                        val isFocused by interaction.collectIsFocusedAsState()

                        val serverItemMod = when {
                            isFocused -> Modifier.background(brush = AkiraLogoGradientBrush)
                            isSelected -> Modifier.background(color = AkiraPrimary)
                            else -> Modifier.background(color = Color.White.copy(alpha = 0.08f))
                        }

                        val serverName = src.sourceName.ifEmpty { "Server ${idx + 1}" }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                .then(serverItemMod)
                                .then(if (idx == 0) Modifier.focusRequester(serverFirstFocusRequester) else Modifier)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = "$serverName${if (isSelected) ", Selected" else ""}"
                                }
                                .focusable(interactionSource = interaction)
                                .clickable(interactionSource = interaction, indication = null) {
                                    showServerDialog = false
                                    playSource(idx, exoPlayer.currentPosition)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = serverName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Episodes Drawer Modal
        if (showEpisodesDrawer && episodes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.90f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(text = "Episodes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(episodes) { ep ->
                            val isCurrent = ep == episodeNumber
                            val interaction = remember { MutableInteractionSource() }
                            val isFocused by interaction.collectIsFocusedAsState()

                            val epDrawerMod = if (isFocused || isCurrent) {
                                Modifier.background(brush = AkiraLogoGradientBrush)
                            } else {
                                Modifier.background(color = Color.White.copy(alpha = 0.15f))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .then(epDrawerMod)
                                    .then(if (ep == episodeNumber || (episodes.indexOf(ep) == 0 && episodeNumber !in episodes)) Modifier.focusRequester(episodeFirstFocusRequester) else Modifier)
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        contentDescription = "Episode $ep${if (isCurrent) ", Now Playing" else ""}"
                                    }
                                    .focusable(interactionSource = interaction)
                                    .clickable(interactionSource = interaction, indication = null) {
                                        showEpisodesDrawer = false
                                        if (ep != episodeNumber) {
                                            scope.launch {
                                                val nextResult = AnimeRepository.fetchEpisodeStreams(
                                                    context = context,
                                                    showId = animeId,
                                                    episodeString = ep,
                                                    translationType = translationType
                                                )
                                                onChangeEpisode(ep, nextResult.streams)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "EP $ep",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}

