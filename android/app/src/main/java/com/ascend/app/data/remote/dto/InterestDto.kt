package com.ascend.app.data.remote.dto

import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.InterestSubcategory
import com.ascend.app.domain.model.UserInterest
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


//Response DTO
@JsonClass(generateAdapter = true)
data class CategoriesResponseDto(
    @Json(name="categories") val categories:List<CategoryDto>
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name="id") val id:String,
    @Json(name="name") val name:String,
    @Json(name="icon") val icon:String,
    @Json(name="description") val description:String,
    @Json(name="color") val color:String,
    @Json(name="subcategories") val subcategories:List<SubcategoryDto>
)

@JsonClass(generateAdapter = true)
data class SubcategoryDto(
    @Json(name = "id")           val id: String,
    @Json(name = "name")         val name: String,
    @Json(name = "description")  val description: String,
    @Json(name = "quest_hints")  val questHints: String = ""
)

@JsonClass(generateAdapter = true)
data class UserInterestsResponseDto(
    @Json(name = "configured") val configured: Boolean,
    @Json(name = "interests")  val interests: List<UserInterestDto>
)

@JsonClass(generateAdapter = true)
data class SaveInterestsResponseDto(
    @Json(name = "configured") val configured: Boolean = false,
    @Json(name = "message")    val message: String = "",
    @Json(name = "count")      val count: Int = 0,
    @Json(name = "interests")  val interests: List<UserInterestDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserInterestDto(
    @Json(name = "id")           val id: String = "",
    @Json(name = "user_id")      val userId: String = "",
    @Json(name = "category")     val category: String,
    @Json(name = "subcategory")  val subcategory: String = "",
    @Json(name = "custom_goal")  val customGoal: String = "",
    @Json(name = "priority")     val priority: Int = 1,
    @Json(name = "proficiency")  val proficiency: String = "Beginner"
)

//Request DTO
@JsonClass(generateAdapter = true)
data class SaveInterestsRequestDto(
    @Json(name = "interests") val interests: List<InterestInputDto>
)

@JsonClass(generateAdapter = true)
data class InterestInputDto(
    @Json(name = "category")     val category: String,
    @Json(name = "subcategory")  val subcategory: String,
    @Json(name = "custom_goal")  val customGoal: String,
    @Json(name = "priority")     val priority: Int,
    @Json(name = "proficiency")  val proficiency: String
)

//Mappers
fun CategoryDto.toDomain() = InterestCategory(
    id = id, name = name, description = description,
    color = color, subcategories = subcategories.map { it.toDomain() }
)

fun SubcategoryDto.toDomain() = InterestSubcategory(
    id = id, name = name,
    description = description, questHints = questHints
)

fun UserInterestDto.toDomain() = UserInterest(
    category = category, subcategory = subcategory,
    customGoal = customGoal, priority = priority,
    proficiency = proficiency
)

fun UserInterest.toDto() = InterestInputDto(
    category = category, subcategory = subcategory,
    customGoal = customGoal, priority = priority,
    proficiency = proficiency
)