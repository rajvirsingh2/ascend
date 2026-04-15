package com.ascend.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.User

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
                is DashboardEffect.LevelUp ->
                    snackbarHostState.showSnackbar("Level up! You are now level ${effect.newLevel}")
                is DashboardEffect.NavigateTo -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // XP header
            state.user?.let { user ->
                item { XpHeader(user = user) }
            }

            // generate button
            item {
                FilledTonalButton(
                    onClick = { viewModel.onIntent(DashboardIntent.RequestNewQuests) },
                    enabled = !state.isGeneratingQuest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isGeneratingQuest) {
                        CircularProgressIndicator(strokeWidth = 2.dp,
                            modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generating quests...")
                    } else {
                        Text("Generate new quests")
                    }
                }
            }

            // quests section
            item {
                Text("Active quests",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }

            items(state.activeQuests, key = { it.id }) { quest ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    QuestCard(
                        quest = quest,
                        onComplete = { viewModel.onIntent(DashboardIntent.CompleteQuest(quest.id)) },
                        onSkip = { viewModel.onIntent(DashboardIntent.SkipQuest(quest.id)) }
                    )
                }
            }

            if (state.activeQuests.isEmpty()) {
                item {
                    Text("No active quests. Generate some!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // habits section
            item {
                Text("Today's habits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }

            items(state.todayHabits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    onComplete = { viewModel.onIntent(DashboardIntent.CompleteHabit(habit.id)) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun XpHeader(user: User) {
    val animatedFraction by animateFloatAsState(
        targetValue = user.xpFraction,
        animationSpec = tween(durationMillis = 800),
        label = "xp_bar"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(user.username, style = MaterialTheme.typography.titleLarge)
                Text("Level ${user.level}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text("${user.currentXp} / ${user.xpToNext} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuestCard(
    quest: Quest,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                Text("+${quest.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Text(quest.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                    Text("Complete")
                }
                OutlinedButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, onComplete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.bodyMedium)
                Text("Streak: ${habit.currentStreak} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (habit.completedToday) {
                Text("Done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = onComplete) {
                    Text("Check in")
                }
            }
        }
    }
}