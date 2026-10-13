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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils

/**
 * TV Card optimized for Leanback 10-foot experience:
 * - Magnifies to 1.08x scale on D-Pad focus
 * - High-contrast border outline on TV remote focus
 */
@Composable
fun AkiraTVAnimeCard(
    anime: AnimeShow,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(150),
        label = "tvCardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "tvCardBorder"
    )

    val shadowElevation = if (isFocused) 16.dp else 4.dp

    Box(
        modifier = modifier
            .zIndex(if (isFocused) 10f else 1f)
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = cardShape,
                spotColor = if (isFocused) AkiraPrimary else Color.Black
            )
            .clip(cardShape)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = borderColor,
                shape = cardShape
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = "${anime.name}. ${if (anime.availableEpisodesSub > 0) "${anime.availableEpisodesSub} episodes sub" else ""}"
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onTap()
            }
    ) {
        val request = remember(anime.thumbnail) {
            ImageRequest.Builder(context)
                .data(anime.thumbnail)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                .apply {
                    ImageUtils.imageHeaders.forEach { (k, v) ->
                        addHeader(k, v)
                    }
                }
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = request,
            contentDescription = anime.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Episode badge at top-right
        val eps = if (anime.availableEpisodesSub > 0) anime.availableEpisodesSub else anime.availableEpisodesDub
        if (eps > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFocused) AkiraLogoGradientBrush else Brush.linearGradient(listOf(Color(0xCC000000), Color(0xCC000000))))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "EP $eps",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Title and meta at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = anime.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            if (!anime.type.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${anime.type}${if (anime.seasonYear != null) " • ${anime.seasonYear}" else ""}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
