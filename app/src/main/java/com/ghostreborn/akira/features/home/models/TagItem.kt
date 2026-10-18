package com.ghostreborn.akira.features.home.models

import org.json.JSONObject

data class TagItem(
    val name: String,
    val slug: String,
    val tagType: String? = null,
    val animeCount: Int = 0,
    val mangaCount: Int = 0,
    val gameCount: Int = 0,
    val otherCount: Int = 0,
    val views: Long = 0
) {
    companion object {
        fun fromJson(json: JSONObject): TagItem {
            return TagItem(
                name = json.optString("name", ""),
                slug = json.optString("slug", "").ifBlank {
                    json.optString("name", "").replace(Regex("[^a-zA-Z0-9]+"), "-").lowercase()
                },
                tagType = json.optString("tagType").takeIf { it.isNotBlank() },
                animeCount = json.optInt("animeCount", 0),
                mangaCount = json.optInt("mangaCount", 0),
                gameCount = json.optInt("gameCount", 0),
                otherCount = json.optInt("otherCount", 0),
                views = json.optLong("views", 0)
            )
        }
    }
}
