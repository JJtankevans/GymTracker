package com.vitoroliveira.gymtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.data.model.Exercise
import com.vitoroliveira.gymtracker.data.model.ExerciseBaseline
import com.vitoroliveira.gymtracker.data.model.ExerciseProgression
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.data.model.WorkoutSet
import com.vitoroliveira.gymtracker.presentation.TimerState
import com.vitoroliveira.gymtracker.presentation.viewmodel.AuthUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseDetailUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.HistoryUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.ProfileUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutHistoryItem
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutUiState
import com.vitoroliveira.gymtracker.ui.auth.LoginScreenContent
import com.vitoroliveira.gymtracker.ui.exercise.ExerciseDetailScreenContent
import com.vitoroliveira.gymtracker.ui.history.HistoryScreenContent
import com.vitoroliveira.gymtracker.ui.muscleGroup.MuscleGroupScreenContent
import com.vitoroliveira.gymtracker.ui.profile.ProfileScreenContent
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import com.vitoroliveira.gymtracker.ui.workout.ActiveWorkoutScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renderiza as telas principais em alguns temas e salva PNGs em
 * app/build/outputs/screen-gallery/<tela>_<tema>.png (modo record).
 * Rodar: ./gradlew :app:testDebugUnitTest --tests "*ScreenGalleryTest*" -Proborazzi.test.record=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ScreenGalleryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val themeIds = listOf("carbon", "studio", "neon", "brutal", "aurora", "papel")

    private val exercises = listOf(
        Exercise(id = "1", name = "Supino Reto", defaultSets = 4, defaultReps = 10),
        Exercise(id = "2", name = "Crucifixo Inclinado", defaultSets = 3, defaultReps = 12),
        Exercise(id = "3", name = "Tríceps Corda", defaultSets = 3, defaultReps = 15)
    )

    private val groups = listOf(
        MuscleGroup(id = "g1", name = "Peito e Tríceps", colorHex = "#FF5722"),
        MuscleGroup(id = "g2", name = "Costas e Bíceps", colorHex = "#2196F3"),
        MuscleGroup(id = "g3", name = "Pernas", colorHex = "#4CAF50")
    )

    @Suppress("DEPRECATION")
    private fun shots(name: String, content: @Composable () -> Unit) {
        var current by mutableStateOf(AppThemes.byId(themeIds.first()))
        composeRule.setContent { GymTrackerTheme(spec = current) { content() } }
        themeIds.forEach { id ->
            current = AppThemes.byId(id)
            composeRule.mainClock.advanceTimeBy(9_000)
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("build/outputs/screen-gallery/${name}_$id.png")
        }
    }

    @Test
    fun activeWorkout() = shots("treino") {
        ActiveWorkoutScreenContent(
            muscleGroupName = "Peito e Tríceps",
            uiState = WorkoutUiState(
                activeWorkout = Workout(id = "w1", finishedExerciseIds = listOf("3")),
                exercises = exercises,
                completedSets = listOf(
                    WorkoutSet(id = "s1", workoutId = "w1", exerciseId = "1", setNumber = 1, reps = 10, weightKg = 80.0),
                    WorkoutSet(id = "s2", workoutId = "w1", exerciseId = "1", setNumber = 2, reps = 8, weightKg = 85.0),
                    WorkoutSet(id = "s3", workoutId = "w1", exerciseId = "3", setNumber = 1, reps = 15, weightKg = 30.0),
                    WorkoutSet(id = "s4", workoutId = "w1", exerciseId = "3", setNumber = 2, reps = 15, weightKg = 30.0),
                    WorkoutSet(id = "s5", workoutId = "w1", exerciseId = "3", setNumber = 3, reps = 12, weightKg = 32.5)
                ),
                baselines = mapOf("1" to ExerciseBaseline(80.0, 10, 82.5)),
                newRecordExerciseIds = setOf("1"),
                timerState = TimerState(totalSeconds = 90, remainingSeconds = 62, isRunning = true)
            ),
            elapsedSeconds = 1432,
            onAddSet = { _, _, _ -> }, onStartTimer = {}, onPauseTimer = {}, onResumeTimer = {},
            onCancelTimer = {}, onFinishWorkout = {}, onClearError = {}, onResumeWorkout = {},
            onDiscardWorkout = {}, onToggleExerciseFinished = {}
        )
    }

    @Test
    fun history() = shots("historico") {
        val base = kotlin.time.Instant.parse("2026-10-18T18:00:00Z")
        HistoryScreenContent(
            uiState = HistoryUiState(
                workouts = listOf(
                    WorkoutHistoryItem("a", "Peito e Tríceps", "#FF5722", base, base, 62),
                    WorkoutHistoryItem("b", "Costas e Bíceps", "#2196F3", kotlin.time.Instant.parse("2026-10-16T18:00:00Z"), null, 55),
                    WorkoutHistoryItem("c", "Pernas", "#4CAF50", kotlin.time.Instant.parse("2026-10-14T18:00:00Z"), null, 71)
                )
            )
        )
    }

    @Test
    fun muscleGroups() = shots("grupos") {
        MuscleGroupScreenContent(
            uiState = MuscleGroupUiState(muscleGroups = groups),
            onNavigateToExercises = { _, _ -> }, onCreate = { _, _ -> }, onUpdate = {}, onDelete = {}, onClearMessages = {}
        )
    }

    @Test
    fun exerciseDetail() = shots("grafico") {
        val pts = listOf(60.0, 62.5, 62.5, 65.0, 67.5, 70.0, 70.0, 75.0)
        ExerciseDetailScreenContent(
            exerciseName = "Supino Reto",
            uiState = ExerciseDetailUiState(
                progression = pts.mapIndexed { i, w ->
                    ExerciseProgression("1", "Supino Reto", "p", "2026-09-${(i * 3 + 1).toString().padStart(2, '0')}", w, 30, 3)
                },
                maxWeightKg = 75.0, lastWeightKg = 75.0, totalSessions = pts.size
            )
        )
    }

    @Test
    fun profile() = shots("perfil") {
        ProfileScreenContent(
            uiState = ProfileUiState(profile = Profile(id = "1", username = "thiago", weeklyGoal = 4)),
            onUpdateWeeklyGoal = {}, onSignOut = {}, onClearMessages = {}
        )
    }

    @Test
    fun login() = shots("login") {
        LoginScreenContent(
            uiState = AuthUiState(), onNavigateToSignUp = {}, onLoginSuccess = {}, onSignIn = { _, _ -> }, onClearError = {}
        )
    }
}
