package com.ascend.app.ui.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.remote.api.UserApiService
import com.ascend.app.data.repository.QuestRepository
import com.ascend.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import java.util.Locale

data class StatsUiState(
    val isLoading: Boolean = true,
    val totalXp: Int = 0,
    val level: Int = 1,
    val totalQuests: Int = 0,
    val habitsCompleted: Int = 0,
    val streakFreezes: Int = 1,
    val heatmapFloats: List<Float> = List(70) { 0f },
    val bestStreak: Int = 0,
    val xpHistory: List<Float> = emptyList(),
    val totalXpLast30Days: Int = 0,
    val questDistribution: List<Triple<String, Int, Color>> = emptyList(),
    val questsThisWeek: Int = 0,
    val questsSkipped: Int = 0,
    val onTimePercentage: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val api: UserApiService,
    private val questRepo: QuestRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state = _state.asStateFlow()

    init {
        fetchStats()
    }

    private fun fetchStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val envelope = api.getStats()
                val data = envelope.data

                // Fetch heatmap
                var hFloats = List(70) { 0f }
                when (val heatResult = questRepo.getHeatmap()) {
                    is Result.Success -> {
                        val pts = heatResult.data
                        val maxCount = pts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                        val today = LocalDate.now()
                        hFloats = (69 downTo 0).map { i ->
                            val d = today.minusDays(i.toLong())
                            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val c = pts.find { it.date == dStr }?.count ?: 0
                            c.toFloat() / maxCount.toFloat()
                        }
                    }
                    else -> {}
                }

                // Process XP History (accumulate for last 30 days chart)
                val today = LocalDate.now()
                var acc = 0f
                val xpVals = (29 downTo 0).map { i ->
                    val dStr = today.minusDays(i.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val dailyXp = data?.xp_history?.find { it.date == dStr }?.xp ?: 0
                    acc += dailyXp
                    maxOf(8f, acc) // ensure some minimum height if 0
                }
                val totalXp30d = data?.xp_history?.sumOf { it.xp } ?: 0

                // Process Quest Distribution
                val dist = data?.quest_distribution?.map { 
                    Triple(
                        it.skill_area.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        it.count,
                        getSkillColor(it.skill_area)
                    )
                } ?: emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        totalXp = data?.total_xp ?: 0,
                        level = data?.level ?: 1,
                        totalQuests = data?.total_quests ?: 0,
                        habitsCompleted = data?.habits_completed ?: 0,
                        streakFreezes = data?.streak_freezes ?: 1,
                        heatmapFloats = hFloats,
                        bestStreak = data?.best_streak ?: 0,
                        xpHistory = xpVals,
                        totalXpLast30Days = totalXp30d,
                        questDistribution = dist.ifEmpty { 
                            // fallback just in case UI expects something
                            listOf(Triple("General", 0, Color(0xFF00E5FF))) 
                        },
                        questsThisWeek = data?.quests_this_week ?: 0,
                        questsSkipped = data?.quests_skipped ?: 0,
                        onTimePercentage = data?.on_time_percentage ?: 0f
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load stats: ${e.message}") }
            }
        }
    }

    private fun getSkillColor(skill: String): Color {
        return when (skill.lowercase()) {
            "tech" -> Color(0xFF00E5FF)
            "physical" -> Color(0xFFFFB4AB)
            "mental" -> Color(0xFFB388FF)
            "social" -> Color(0xFFFFD700)
            "finance" -> Color(0xFF732EE4)
            else -> Color(0xFF00E5FF)
        }
    }
}

