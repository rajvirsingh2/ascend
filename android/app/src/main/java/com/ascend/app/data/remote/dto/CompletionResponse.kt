package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CompletionResponse(
    @Json(name = "xp_awarded") val xpAwarded: Int? = null,
    @Json(name = "leveled_up") val leveledUp: Boolean? = null,
    @Json(name = "level_after") val levelAfter: Int? = null,
    @Json(name = "xp_after") val xpAfter: Int? = null
)
