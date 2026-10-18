package com.ghostreborn.akira.features.home.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.mutableIntStateOf
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.models.AnimeDetail
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.services.WatchHistoryManager
import com.ghostreborn.akira.ui.components.AkiraTVButton
import com.ghostreborn.akira.ui.theme.AkiraDarkBackground
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun TVAnimeDetailScreen(
    animeId: String,
    initialTitle: String? = null,
    initialPoster: String? = null,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (String, List<StreamSource>, Long, List<String>, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAudioTab by remember { mutableStateOf("SUB") }
    var loadingEpisodeString by remember { mutableStateOf<String?>(null) }
    var lastProgressMs by remember { mutableStateOf(0L) }
    var lastWatchedEp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(animeId) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val fetched = AnimeRepository.fetchAnimeDetail(animeId, context)
                detail = fetched
                val hist = WatchHistoryManager.getProgress(context, animeId)
                if (hist != null) {
                    lastWatchedEp = hist.episodeNumber
                    lastProgressMs = hist.seekPositionMs
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load anime details"
            } finally {
                isLoading = false
            }
        }
    }

    fun playEpisode(epNum: String) {
        if (loadingEpisodeString != null) return
        loadingEpisodeString = epNum
        scope.launch {
            try {
                val transType = selectedAudioTab.lowercase()
                val token = TokenManager.getAccessToken(context)
                val fetchResult = AnimeRepository.fetchEpisodeStreams(
                    showId = animeId,
                    translationType = transType,
                    episodeString = epNum,
                    authToken = token,
                    context = context
                )
                val sources = fetchResult.streams

                if (sources.isEmpty()) {
                    Toast.makeText(context, "No streaming servers found for Episode $epNum", Toast.LENGTH_SHORT).show()
                    loadingEpisodeString = null
                    return@launch
                }

                val epList = if (selectedAudioTab == "DUB") {
                    detail?.episodesDetail?.dub ?: emptyList()
                } else {
                    detail?.episodesDetail?.sub ?: emptyList()
                }

                val resumeMs = if (lastWatchedEp == epNum) lastProgressMs else 0L
                loadingEpisodeString = null
                onPlayEpisode(epNum, sources, resumeMs, epList, transType)
            } catch (e: Exception) {
                loadingEpisodeString = null
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler { onNavigateBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraDarkBackground)
    ) {
        if (isLoading && detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AkiraPrimary)
            }
            return
        }

        val displayDetail = detail
        val title = displayDetail?.name ?: initialTitle ?: "Anime"
        val poster = displayDetail?.thumbnail ?: initialPoster

        val currentEpisodes = if (selectedAudioTab == "DUB") {
            displayDetail?.episodesDetail?.dub ?: emptyList()
        } else {
            displayDetail?.episodesDetail?.sub ?: emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "billboard_header",
                span = { GridItemSpan(8) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val isBackFocused by backInteraction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(if (isBackFocused) 2.dp else 0.dp, if (isBackFocused) Color.White else Color.Transparent, CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(interactionSource = backInteraction, indication = null) {
                                onNavigateBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .aspectRatio(0.70f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        val request = remember(poster) {
                            ImageRequest.Builder(context)
                                .data(poster)
                                .crossfade(true)
                                .apply { ImageUtils.imageHeaders.forEach { (k, v) -> addHeader(k, v) } }
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!displayDetail?.genres.isNullOrEmpty()) {
                            Text(
                                text = displayDetail!!.genres.joinToString(" • "),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        }

                        if (!displayDetail?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = displayDetail!!.description!!,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val playTargetEp = lastWatchedEp ?: currentEpisodes.firstOrNull() ?: "1"
                            AkiraTVButton(
                                text = if (lastWatchedEp != null) "Resume EP $lastWatchedEp" else "Play EP $playTargetEp",
                                leadingIcon = Icons.Rounded.PlayArrow,
                                loading = loadingEpisodeString == playTargetEp,
                                onClick = { playEpisode(playTargetEp) }
                            )

                            listOf("SUB", "DUB").forEach { tab ->
                                val isSelected = selectedAudioTab == tab
                                val tabInteraction = remember { MutableInteractionSource() }
                                val isTabFocused by tabInteraction.collectIsFocusedAsState()

                                val tabBgMod = if (isSelected) {
                                    Modifier.background(brush = AkiraLogoGradientBrush)
                                } else {
                                    Modifier.background(color = Color.White.copy(alpha = 0.12f))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isTabFocused) 2.dp else 0.dp,
                                            color = if (isTabFocused) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .then(tabBgMod)
                                        .clickable(interactionSource = tabInteraction, indication = null) {
                                            selectedAudioTab = tab
                                        }
                                        .padding(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = tab,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(
                key = "episodes_title",
                span = { GridItemSpan(8) }
            ) {
                Text(
                    text = "Episodes (${currentEpisodes.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(
                items = currentEpisodes,
                key = { it }
            ) { epNumber ->
                val isCurrentEp = lastWatchedEp == epNumber
                val isEpLoading = loadingEpisodeString == epNumber

                TVEpisodeChip(
                    epNumber = epNumber,
                    isCurrentEp = isCurrentEp,
                    isLoading = isEpLoading,
                    onClick = { playEpisode(epNumber) }
                )
            }
        }


    }
}

@Composable
fun TVEpisodeChip(
    epNumber: String,
    isCurrentEp: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val epInteraction = remember { MutableInteractionSource() }
    val isEpFocused by epInteraction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isEpFocused) 1.1f else 1.0f,
        animationSpec = tween(120),
        label = "epChipScale"
    )

    val chipBgMod = when {
        isEpFocused -> Modifier.background(brush = AkiraLogoGradientBrush)
        isCurrentEp -> Modifier.background(color = AkiraPrimary.copy(alpha = 0.85f))
        else -> Modifier.background(color = Color.White.copy(alpha = 0.09f))
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isEpFocused) 2.5.dp else 0.dp,
                color = if (isEpFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .then(chipBgMod)
            .clickable(interactionSource = epInteraction, indication = null) {
                onClick()
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = "EP $epNumber",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isEpFocused || isCurrentEp) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
