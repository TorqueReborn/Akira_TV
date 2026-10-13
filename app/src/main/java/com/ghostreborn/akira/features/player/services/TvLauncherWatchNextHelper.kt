package com.ghostreborn.akira.features.player.services

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.ghostreborn.akira.MainActivity

object TvLauncherWatchNextHelper {
    private const val TAG = "TvLauncherWatchNext"

    /**
     * Publishes or updates an anime episode on the Android TV system "Watch Next" / "Play Next" row.
     */
    fun updateWatchNextProgram(
        context: Context,
        animeId: String,
        animeTitle: String,
        episodeNumber: String,
        progressMs: Long,
        durationMs: Long,
        thumbnailUrl: String?
    ) {
        if (progressMs <= 0 || durationMs <= 0 || animeId.isBlank()) return

        try {
            val contentResolver = context.contentResolver

            // Intent fired when the user clicks the card on the Android TV home screen
            val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("akira://watch/$animeId/$episodeNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val internalProviderId = "${animeId}_ep_${episodeNumber}"

            // Check if this program already exists in Watch Next
            val existingProgramId = findProgramByInternalProviderId(context, internalProviderId)

            val isCompleted = progressMs >= (durationMs * 0.95)
            if (isCompleted) {
                // If the user finished watching, remove it from the Watch Next row
                if (existingProgramId != null) {
                    val uri = TvContractCompat.buildWatchNextProgramUri(existingProgramId)
                    contentResolver.delete(uri, null, null)
                    Log.d(TAG, "Removed finished program $internalProviderId from Watch Next")
                }
                return
            }

            val builder = WatchNextProgram.Builder()
                .setTitle(animeTitle)
                .setEpisodeTitle("Episode $episodeNumber")
                .setEpisodeNumber(episodeNumber.toIntOrNull() ?: 1)
                .setType(TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setLastPlaybackPositionMillis(progressMs.toInt())
                .setDurationMillis(durationMs.toInt())
                .setIntent(deepLinkIntent)
                .setInternalProviderId(internalProviderId)

            if (!thumbnailUrl.isNullOrBlank()) {
                builder.setPosterArtUri(Uri.parse(thumbnailUrl))
            }

            val program = builder.build()

            if (existingProgramId != null) {
                val programUri = TvContractCompat.buildWatchNextProgramUri(existingProgramId)
                contentResolver.update(
                    programUri,
                    program.toContentValues(),
                    null,
                    null
                )
                Log.d(TAG, "Updated Watch Next program: $animeTitle Ep $episodeNumber")
            } else {
                val uri = contentResolver.insert(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    program.toContentValues()
                )
                Log.d(TAG, "Inserted Watch Next program: $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update Watch Next on TV Launcher: ${e.message}")
        }
    }

    private fun findProgramByInternalProviderId(context: Context, internalProviderId: String): Long? {
        val projection = arrayOf(
            TvContractCompat.WatchNextPrograms._ID,
            TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
        )

        return try {
            context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                val providerIdIndex = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID)

                while (cursor.moveToNext()) {
                    if (providerIdIndex != -1 && idIndex != -1) {
                        val currentId = cursor.getString(providerIdIndex)
                        if (currentId == internalProviderId) {
                            return cursor.getLong(idIndex)
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying Watch Next: ${e.message}")
            null
        }
    }
}
