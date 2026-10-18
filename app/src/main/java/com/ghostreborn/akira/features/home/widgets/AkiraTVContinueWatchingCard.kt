package com.ghostreborn.akira.features.home.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.player.services.AnimeProgress
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils

@Composable
fun AkiraTVContinueWatchingCard(
    progress: AnimeProgress,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "continueCardScale"
    )

    val cardShape = RoundedCornerShape(14.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "continueCardBorder"
    )

    val progressFraction = if (progress.totalDurationMs > 0) {
        (progress.seekPositionMs.toFloat() / progress.totalDurationMs.toFloat()).coerceIn(0.02f, 1f)
    } else 0f

    val percent = (progressFraction * 100).toInt()

    Box(
        modifier = modifier
            .width(240.dp)
            .height(140.dp)
            .zIndex(if (isFocused) 10f else 1f)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = cardShape,
                spotColor = if (isFocused) AkiraPrimary else Color.Black
            )
            .clip(cardShape)
            .border(
                width = if (isFocused) 2.5.dp else 0.dp,
                color = borderColor,
                shape = cardShape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Continue watching ${progress.animeTitle}, Episode ${progress.episodeNumber}, $percent% completed"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onTap()
            }
    ) {
        val request = remember(progress.thumbnail) {
            ImageRequest.Builder(context)
                .data(progress.thumbnail)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(true)
                .apply {
                    ImageUtils.imageHeaders.forEach { (k, v) ->
                        addHeader(k, v)
                    }
                }
                .build()
        }

        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // Episode badge top-start
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "EP ${progress.episodeNumber}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Center play icon indicator on focus
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AkiraPrimary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Title and bottom progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            Text(
                text = progress.animeTitle,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(4.dp)
                        .background(brush = AkiraLogoGradientBrush)
                )
            }
        }
    }
}
