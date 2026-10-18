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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.utils.ImageUtils
import kotlinx.coroutines.delay

@Composable
fun AkiraTVFeaturedBanner(
    featuredShows: List<AnimeShow>,
    modifier: Modifier = Modifier,
    onTap: (AnimeShow) -> Unit
) {
    if (featuredShows.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { featuredShows.size })
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(pagerState, isFocused) {
        while (!isFocused) {
            delay(6000)
            if (featuredShows.size > 1 && !isFocused) {
                val nextPage = (pagerState.currentPage + 1) % featuredShows.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val bannerShape = RoundedCornerShape(20.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(150),
        label = "bannerScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "bannerBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .scale(scale)
            .shadow(if (isFocused) 20.dp else 6.dp, bannerShape, spotColor = if (isFocused) AkiraPrimary else Color.Black)
            .clip(bannerShape)
            .border(if (isFocused) 3.dp else 0.dp, borderColor, bannerShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (pagerState.currentPage < featuredShows.size) {
                    onTap(featuredShows[pagerState.currentPage])
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val show = featuredShows[page]
            val request = remember(show.thumbnail) {
                ImageRequest.Builder(context)
                    .data(show.banner ?: show.thumbnail)
                    .crossfade(true)
                    .apply {
                        ImageUtils.imageHeaders.forEach { (k, v) ->
                            addHeader(k, v)
                        }
                    }
                    .build()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = request,
                    contentDescription = show.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.90f),
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AkiraLogoGradientBrush)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SPOTLIGHT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = show.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!show.type.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${show.type}${if (show.seasonYear != null) " • ${show.seasonYear}" else ""}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isFocused) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFocused) "Press OK to Watch" else "Watch Now",
                                    color = if (isFocused) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
