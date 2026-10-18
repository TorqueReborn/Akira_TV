package com.ghostreborn.akira.features.home.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.home.models.AnimeShow
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.features.home.widgets.AkiraTVAnimeCard
import com.ghostreborn.akira.features.home.widgets.AkiraTVFeaturedBanner
import com.ghostreborn.akira.ui.theme.AkiraDarkBackground
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TVHomeScreen(
    onNavigateToDetail: (AnimeShow) -> Unit,
    onNavigateToSettings: () -> Unit,
    onContinueWatching: (com.ghostreborn.akira.features.player.services.AnimeProgress) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var translationType by remember { mutableStateOf("sub") }

    val continueWatchingList = remember { mutableStateListOf<com.ghostreborn.akira.features.player.services.AnimeProgress>() }

    fun refreshContinueWatching() {
        continueWatchingList.clear()
        continueWatchingList.addAll(
            com.ghostreborn.akira.features.player.services.WatchHistoryManager.getProgressList(context)
                .filter { it.seekPositionMs > 0 && it.totalDurationMs > 0 && it.seekPositionMs < (it.totalDurationMs * 0.95) }
                .take(10)
        )
    }

    LaunchedEffect(Unit) {
        refreshContinueWatching()
    }

    val categories = remember {
        listOf(
            "Latest Updates" to "Latest_Update",
            "Top Ranked" to "Top",
            "Popular" to "Popular",
            "New Releases" to "Release_Year"
        )
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val shows = remember { mutableStateListOf<AnimeShow>() }
    val featuredShows = remember { mutableStateListOf<AnimeShow>() }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val searchFocusRequester = remember { FocusRequester() }

    fun fetchShows(page: Int, reset: Boolean = false) {
        if (reset) {
            currentPage = 1
            hasMore = true
            errorMessage = null
            if (shows.isEmpty()) isLoading = true
        } else {
            isLoadingMore = true
        }

        scope.launch {
            try {
                val currentCategoryKey = categories[selectedCategoryIndex].second
                val pageResult = if (currentCategoryKey == "Top" && (!isSearchActive || searchQuery.isBlank())) {
                    AnimeRepository.fetchTopRankedAnime(dateRange = 1, size = 20, context = context)
                } else {
                    val query = if (isSearchActive && searchQuery.isNotBlank()) searchQuery.trim() else null
                    val res = AnimeRepository.fetchAnimeList(
                        page = page,
                        limit = 25,
                        translationType = translationType,
                        searchQuery = query,
                        sortBy = if (currentCategoryKey == "Top") "Top" else currentCategoryKey,
                        context = context
                    )
                    res.shows
                }

                if (reset) {
                    shows.clear()
                    if (featuredShows.isEmpty() && pageResult.isNotEmpty()) {
                        featuredShows.addAll(pageResult.take(5))
                    }
                }

                shows.addAll(pageResult)
                hasMore = pageResult.isNotEmpty()
                currentPage = page
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load shows"
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(selectedCategoryIndex, translationType) {
        if (!isSearchActive) {
            fetchShows(1, reset = true)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            !isLoading && !isLoadingMore && hasMore && totalItems > 0 && lastVisible >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            fetchShows(currentPage + 1, reset = false)
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        fetchShows(1, reset = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraDarkBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            state = gridState,
            contentPadding = PaddingValues(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "tv_header",
                contentType = "header",
                span = { GridItemSpan(5) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AKIRA",
                        style = TextStyle(
                            brush = AkiraLogoGradientBrush,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("sub" to "SUB", "dub" to "DUB").forEach { (typeKey, label) ->
                            val isSelected = translationType == typeKey
                            val interaction = remember { MutableInteractionSource() }
                            val isFocused by interaction.collectIsFocusedAsState()

                            val buttonMod = if (isSelected) {
                                Modifier.background(brush = AkiraLogoGradientBrush)
                            } else {
                                Modifier.background(color = Color.White.copy(alpha = 0.12f))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isFocused) 2.dp else 0.dp,
                                        color = if (isFocused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .then(buttonMod)
                                    .clickable(interactionSource = interaction, indication = null) {
                                        if (translationType != typeKey) {
                                            translationType = typeKey
                                            fetchShows(1, reset = true)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        val searchInteraction = remember { MutableInteractionSource() }
                        val isSearchFocused by searchInteraction.collectIsFocusedAsState()
                        val searchMod = if (isSearchActive) Modifier.background(brush = AkiraLogoGradientBrush) else Modifier.background(color = Color.White.copy(alpha = 0.12f))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(if (isSearchFocused) 2.dp else 0.dp, if (isSearchFocused) Color.White else Color.Transparent, CircleShape)
                                .then(searchMod)
                                .clickable(interactionSource = searchInteraction, indication = null) {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) {
                                        searchQuery = ""
                                        fetchShows(1, reset = true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val settingsInteraction = remember { MutableInteractionSource() }
                        val isSettingsFocused by settingsInteraction.collectIsFocusedAsState()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(if (isSettingsFocused) 2.dp else 0.dp, if (isSettingsFocused) Color.White else Color.Transparent, CircleShape)
                                .background(color = Color.White.copy(alpha = 0.12f))
                                .clickable(interactionSource = settingsInteraction, indication = null) {
                                    onNavigateToSettings()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isSearchActive) {
                item(
                    key = "search_field",
                    span = { GridItemSpan(5) }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                delay(450)
                                fetchShows(1, reset = true)
                            }
                        },
                        placeholder = { Text("Type anime title...", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AkiraPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            item(
                key = "category_shelf",
                contentType = "categories",
                span = { GridItemSpan(5) }
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    itemsIndexed(categories) { index, (label, _) ->
                        val isSelected = selectedCategoryIndex == index
                        val interaction = remember { MutableInteractionSource() }
                        val isFocused by interaction.collectIsFocusedAsState()

                        val shelfMod = when {
                            isFocused -> Modifier.background(brush = AkiraLogoGradientBrush)
                            isSelected -> Modifier.background(color = AkiraPrimary.copy(alpha = 0.8f))
                            else -> Modifier.background(color = Color.White.copy(alpha = 0.08f))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isFocused) 2.5.dp else 0.dp,
                                    color = if (isFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .then(shelfMod)
                                .clickable(interactionSource = interaction, indication = null) {
                                    if (selectedCategoryIndex != index) {
                                        selectedCategoryIndex = index
                                        fetchShows(1, reset = true)
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (continueWatchingList.isNotEmpty() && !isSearchActive) {
                item(
                    key = "continue_watching_shelf",
                    contentType = "continue_watching",
                    span = { GridItemSpan(5) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Continue Watching",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = continueWatchingList,
                                key = { it.animeId }
                            ) { progress ->
                                com.ghostreborn.akira.features.home.widgets.AkiraTVContinueWatchingCard(
                                    progress = progress,
                                    onTap = { onContinueWatching(progress) }
                                )
                            }
                        }
                    }
                }
            }

            if (featuredShows.isNotEmpty() && !isSearchActive) {
                item(
                    key = "hero_banner",
                    contentType = "banner",
                    span = { GridItemSpan(5) }
                ) {
                    AkiraTVFeaturedBanner(
                        featuredShows = featuredShows,
                        onTap = onNavigateToDetail,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            if (isLoading && shows.isEmpty()) {
                item(
                    key = "initial_loading",
                    span = { GridItemSpan(5) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AkiraPrimary)
                    }
                }
            }

            items(
                items = shows,
                key = { it.id },
                contentType = { "tv_card" }
            ) { show ->
                AkiraTVAnimeCard(
                    anime = show,
                    onTap = { onNavigateToDetail(show) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoadingMore) {
                item(
                    key = "loading_more",
                    span = { GridItemSpan(5) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AkiraPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
