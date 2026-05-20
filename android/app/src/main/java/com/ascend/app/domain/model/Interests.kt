package com.ascend.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InterestCategory(
    val id: String,
    val name: String,
    val description: String,
    val color: String,
    val subcategories: List<InterestSubcategory>
): Parcelable

@Parcelize
data class InterestSubcategory(
    val id:String,
    val name:String,
    val description:String,
    val questHints:String=""
): Parcelable

@Parcelize
data class UserInterest(
    val category:String,
    val subcategory:String="",
    val customGoal:String="",
    val priority:Int=1,
    val proficiency:String="Beginner"
): Parcelable

enum class InterestPriority(val value:Int, val label:String, val description:String){
    PRIMARY(1,"PRIMARY","FOCUS AREA-most quests from here"),
    SECONDARY(2,"SECONDARY", "SUPPORT AREA-occasional quests"),
    OPTIONAL(3,"OPTIONAL","EXPLORE-quests when relevant")
}