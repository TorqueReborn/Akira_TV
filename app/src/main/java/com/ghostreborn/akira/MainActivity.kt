package com.ghostreborn.akira

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.screens.TVAnimeDetailScreen
import com.ghostreborn.akira.features.home.screens.TVHomeScreen
import com.ghostreborn.akira.features.player.models.StreamSource
import com.ghostreborn.akira.features.player.screens.TVPlayerScreen
import com.ghostreborn.akira.features.settings.screens.TVSettingsScreen
import com.ghostreborn.akira.features.updater.services.UpdateManager
import com.ghostreborn.akira.features.updater.ui.UpdateDialog
import com.ghostreborn.akira.ui.theme.AkiraTheme
import kotlinx.coroutines.launch

object NavHolder {
    var pendingSources: List<StreamSource> = emptyList()
    var pendingInitialPositionMs: Long = 0L
    var pendingThumbnail: String? = null
    var pendingEpisodes: List<String> = emptyList()
    var pendingTranslationType: String = "sub"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val updateManager = UpdateManager.getInstance(this)
        updateManager.checkForUpdates(isManual = false)

        setContent {
            AkiraTheme {
                val updateState = updateManager.uiState.collectAsState().value

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkiraTVAppNavigation(intent = intent)

                    UpdateDialog(
                        state = updateState,
                        onStartDownload = { release -> updateManager.startDownload(release) },
                        onCancelDownload = { updateManager.cancelDownload() },
                        onInstall = { apkFile -> updateManager.installApk(apkFile) },
                        onDismiss = { updateManager.clearState() },
                        onDismissUpdateVersion = { release -> updateManager.dismissUpdate(release) }
                    )
                }
            }
        }
    }
}

@Composable
fun AkiraTVAppNavigation(intent: android.content.Intent? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val isLoggedIn = remember { TokenManager.isLoggedIn(context) }
    val startDestination = if (isLoggedIn) "home" else "login"

    androidx.compose.runtime.LaunchedEffect(intent?.data) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "akira" && uri.host == "watch") {
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 2) {
                val animeId = pathSegments[0]
                val episodeNumber = pathSegments[1]
                scope.launch {
                    try {
                        val progress = com.ghostreborn.akira.features.player.services.WatchHistoryManager.getProgress(context, animeId)
                        val streamResult = com.ghostreborn.akira.features.home.services.AnimeRepository.fetchEpisodeStreams(
                            context = context,
                            showId = animeId,
                            episodeString = episodeNumber,
                            translationType = "sub"
                        )
                        NavHolder.pendingSources = streamResult.streams
                        NavHolder.pendingInitialPositionMs = progress?.seekPositionMs ?: 0L
                        NavHolder.pendingThumbnail = progress?.thumbnail
                        NavHolder.pendingEpisodes = emptyList()
                        NavHolder.pendingTranslationType = "sub"

                        val title = progress?.animeTitle ?: "Anime"
                        val encodedTitle = Uri.encode(title)
                        navController.navigate("player/$animeId/$episodeNumber?title=$encodedTitle")
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Failed to resume: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- 1. LOGIN SCREEN ---
        composable("login") {
            TVLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- 2. HOME SCREEN ---
        composable("home") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            TVHomeScreen(
                onNavigateToDetail = { anime ->
                    val encodedTitle = Uri.encode(anime.name)
                    val encodedPoster = Uri.encode(anime.thumbnail ?: "")
                    navController.navigate("detail/${anime.id}?title=$encodedTitle&poster=$encodedPoster")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onContinueWatching = { progress ->
                    scope.launch {
                        try {
                            val streamResult = com.ghostreborn.akira.features.home.services.AnimeRepository.fetchEpisodeStreams(
                                context = context,
                                showId = progress.animeId,
                                episodeString = progress.episodeNumber,
                                translationType = "sub"
                            )
                            NavHolder.pendingSources = streamResult.streams
                            NavHolder.pendingInitialPositionMs = progress.seekPositionMs
                            NavHolder.pendingThumbnail = progress.thumbnail
                            NavHolder.pendingEpisodes = emptyList()
                            NavHolder.pendingTranslationType = "sub"

                            val encodedTitle = Uri.encode(progress.animeTitle)
                            navController.navigate("player/${progress.animeId}/${progress.episodeNumber}?title=$encodedTitle")
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Failed to resume: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // --- 3. ANIME DETAIL SCREEN ---
        composable(
            route = "detail/{animeId}?title={title}&poster={poster}",
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("poster") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) }
            val poster = backStackEntry.arguments?.getString("poster")?.let { Uri.decode(it) }

            TVAnimeDetailScreen(
                animeId = animeId,
                initialTitle = title,
                initialPoster = poster,
                onNavigateBack = { navController.popBackStack() },
                onPlayEpisode = { epNumber, sources, startMs, epList, transType ->
                    NavHolder.pendingSources = sources
                    NavHolder.pendingInitialPositionMs = startMs
                    NavHolder.pendingThumbnail = poster
                    NavHolder.pendingEpisodes = epList
                    NavHolder.pendingTranslationType = transType
                    val encodedTitle = Uri.encode(title ?: "Anime")
                    navController.navigate("player/$animeId/$epNumber?title=$encodedTitle")
                }
            )
        }

        // --- 4. VIDEO PLAYER SCREEN ---
        composable(
            route = "player/{animeId}/{episodeNumber}?title={title}",
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("episodeNumber") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "Anime"
                }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            val episodeNumber = backStackEntry.arguments?.getString("episodeNumber") ?: "1"
            val title = backStackEntry.arguments?.getString("title")?.let { Uri.decode(it) } ?: "Anime"
            val sources = NavHolder.pendingSources
            val initialMs = NavHolder.pendingInitialPositionMs
            val thumb = NavHolder.pendingThumbnail
            val episodes = NavHolder.pendingEpisodes
            val translationType = NavHolder.pendingTranslationType

            TVPlayerScreen(
                animeId = animeId,
                animeTitle = title,
                episodeNumber = episodeNumber,
                sources = sources,
                thumbnail = thumb,
                initialPositionMs = initialMs,
                episodes = episodes,
                translationType = translationType,
                onNavigateBack = { navController.popBackStack() },
                onChangeEpisode = { nextEp, nextSources ->
                    NavHolder.pendingSources = nextSources
                    NavHolder.pendingInitialPositionMs = 0L
                    val encodedTitle = Uri.encode(title)
                    navController.navigate("player/$animeId/$nextEp?title=$encodedTitle") {
                        popUpTo("player/$animeId/$episodeNumber?title=$encodedTitle") { inclusive = true }
                    }
                }
            )
        }

        // --- 5. SETTINGS SCREEN ---
        composable("settings") {
            TVSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
