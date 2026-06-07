package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CompletionResponse(
    @Json(name = "xp_awarded") val xpAwarded: Int? = null,
    @Json(name = "leveled_up") val leveledUp: Boolean? = null,
    @Json(name = "level_after") val levelAfter: Int? = null,
    @Json(name = "xp_after") val xpAfter: Int? = null,
    @Json(name = "stat_deltas") val statDeltas: List<StatDeltaResponse>? = null,
    @Json(name = "hp_restored") val hpRestored: Int? = null,
    @Json(name = "hp_after") val hpAfter: Int? = null
)

@JsonClass(generateAdapter = true)
data class StatDeltaResponse(
    @Json(name = "stat_name") val statName: String,
    @Json(name = "before") val before: Int,
    @Json(name = "after") val after: Int,
    @Json(name = "delta") val delta: Int
)
