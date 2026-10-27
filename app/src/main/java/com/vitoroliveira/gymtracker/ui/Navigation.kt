package com.vitoroliveira.gymtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vitoroliveira.gymtracker.ui.auth.LoginScreen
import com.vitoroliveira.gymtracker.ui.auth.SignUpScreen
import com.vitoroliveira.gymtracker.ui.auth.SplashScreen
import com.vitoroliveira.gymtracker.ui.exercise.ExerciseDetailScreen
import com.vitoroliveira.gymtracker.ui.exercise.ExerciseScreen
import com.vitoroliveira.gymtracker.ui.home.HomeScreen
import com.vitoroliveira.gymtracker.ui.muscleGroup.MuscleGroupScreen
import com.vitoroliveira.gymtracker.ui.profile.ProfileScreen
import com.vitoroliveira.gymtracker.ui.theme.ThemePickerScreen
import com.vitoroliveira.gymtracker.ui.workout.ActiveWorkoutScreen
import com.vitoroliveira.gymtracker.ui.workout.StartWorkoutSheet
import com.vitoroliveira.gymtracker.ui.history.HistoryScreen
import com.vitoroliveira.gymtracker.ui.workout.WorkoutDetailScreen
import android.net.Uri
import android.os.Bundle

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Themes : Screen("themes")
    object Evolution : Screen("evolution")
    object MuscleGroups : Screen("muscle_groups")
    object Records : Screen("records")
    object Achievements : Screen("achievements")
    object Social : Screen("social")

    // Preparado para o Passo 12 (Fase 4) — histórico de treinos.
    object History : Screen("history")

    // Novo — Detalhe do Treino (Passo 12.5), navegado a partir do Histórico
    object WorkoutDetail : Screen("workout_detail/{workoutId}/{groupName}") {
        fun createRoute(workoutId: String, groupName: String) =
            "workout_detail/$workoutId/${routeArg(groupName)}"
    }

    object Exercises : Screen("exercises/{groupId}/{groupName}") {
        fun createRoute(groupId: String, groupName: String) =
            "exercises/$groupId/${routeArg(groupName)}"
    }

    // Preparado para o Passo 13 (Fase 4) — gráfico de progressão de carga.
    // Ainda não tem tela implementada, só a rota reservada.
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}/{exerciseName}") {
        fun createRoute(exerciseId: String, exerciseName: String) =
            "exercise_detail/$exerciseId/${routeArg(exerciseName)}"
    }

    object Circuit : Screen("circuit/{muscleGroupId}/{muscleGroupName}") {
        fun createRoute(muscleGroupId: String?, muscleGroupName: String?) =
            "circuit/${routeArg(muscleGroupId)}/${routeArg(muscleGroupName ?: "Treino Livre")}"
    }

    object ActiveWorkout : Screen("active_workout/{muscleGroupId}/{muscleGroupName}") {
        fun createRoute(muscleGroupId: String?, muscleGroupName: String?) =
            "active_workout/${routeArg(muscleGroupId)}/${routeArg(muscleGroupName ?: "Treino Livre")}"
    }
}

/** Valor ausente (treino livre, nome vazio) dentro da rota: a rota não aceita um pedaço vazio. */
private const val NO_ROUTE_VALUE = "null"

/**
 * Texto livre (nome de grupo ou de exercício) como pedaço da rota. Sem codificar, um "/" no nome ("Peito/Tríceps")
 * criava um pedaço a mais e a rota não era encontrada (o app fechava); "?", "#" e "%" também quebravam.
 * O Navigation devolve o argumento já decodificado: leia com [routeText], nunca com URLDecoder de novo.
 */
internal fun routeArg(value: String?): String = Uri.encode(value?.takeIf { it.isNotBlank() } ?: NO_ROUTE_VALUE)

/** Argumento da rota montado com [routeArg]; null quando o valor era ausente. */
private fun Bundle?.routeText(key: String): String? = this?.getString(key)?.takeIf { it != NO_ROUTE_VALUE }

@Composable
fun GymTrackerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSignedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            var showStartWorkoutSheet by remember { mutableStateOf(false) }

            HomeScreen(
                onNavigateToMuscleGroups = {
                    navController.navigate(Screen.MuscleGroups.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onStartWorkoutClick = { showStartWorkoutSheet = true },
                onNavigateToThemes = { navController.navigate(Screen.Themes.route) },
                onNavigateToEvolution = { navController.navigate(Screen.Evolution.route) },
                onNavigateToRecords = { navController.navigate(Screen.Records.route) },
                onNavigateToSocial = { navController.navigate(Screen.Social.route) }
            )

            if (showStartWorkoutSheet) {
                StartWorkoutSheet(
                    onStartWorkout = { muscleGroupId, muscleGroupName ->
                        showStartWorkoutSheet = false
                        navController.navigate(
                            Screen.ActiveWorkout.createRoute(muscleGroupId, muscleGroupName)
                        )
                    },
                    onStartCircuit = { muscleGroupId, muscleGroupName ->
                        showStartWorkoutSheet = false
                        navController.navigate(Screen.Circuit.createRoute(muscleGroupId, muscleGroupName))
                    },
                    onDismiss = { showStartWorkoutSheet = false }
                )
            }
        }


        composable(Screen.MuscleGroups.route) {
            MuscleGroupScreen(
                onNavigateToExercises = { groupId, groupName ->
                    navController.navigate(
                        Screen.Exercises.createRoute(groupId, groupName)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Exercises.route) { backStackEntry ->
            val groupId = backStackEntry.arguments.routeText("groupId")
            val groupName = backStackEntry.arguments.routeText("groupName").orEmpty()

            ExerciseScreen(
                muscleGroupId = groupId,
                muscleGroupName = groupName,
                onNavigateToDetail = { exerciseId, exerciseName ->
                    navController.navigate(
                        Screen.ExerciseDetail.createRoute(exerciseId, exerciseName)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExerciseDetail.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments.routeText("exerciseId") ?: return@composable
            val exerciseName = backStackEntry.arguments.routeText("exerciseName").orEmpty()

            ExerciseDetailScreen(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToThemes = { navController.navigate(Screen.Themes.route) },
                onNavigateToEvolution = { navController.navigate(Screen.Evolution.route) },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Achievements.route) {
            com.vitoroliveira.gymtracker.ui.achievements.AchievementsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Social.route) {
            com.vitoroliveira.gymtracker.ui.social.SocialScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Records.route) {
            com.vitoroliveira.gymtracker.ui.records.RecordsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Evolution.route) {
            com.vitoroliveira.gymtracker.ui.body.EvolutionScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Themes.route) {
            ThemePickerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Circuit.route) { backStackEntry ->
            val gid = backStackEntry.arguments.routeText("muscleGroupId")
            val gname = backStackEntry.arguments.routeText("muscleGroupName")
            com.vitoroliveira.gymtracker.ui.circuit.CircuitScreen(
                muscleGroupId = gid, muscleGroupName = gname,
                onExit = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Circuit.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.ActiveWorkout.route) { backStackEntry ->
            val muscleGroupId = backStackEntry.arguments.routeText("muscleGroupId")
            val muscleGroupName = backStackEntry.arguments.routeText("muscleGroupName")

            ActiveWorkoutScreen(
                muscleGroupId = muscleGroupId,
                muscleGroupName = muscleGroupName,
                onWorkoutFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ActiveWorkout.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onWorkoutClick = { workoutId, groupName ->
                    navController.navigate(Screen.WorkoutDetail.createRoute(workoutId, groupName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkoutDetail.route) { backStackEntry ->
            val workoutId = backStackEntry.arguments.routeText("workoutId")
                ?: return@composable

            val groupName = backStackEntry.arguments.routeText("groupName").orEmpty()

            WorkoutDetailScreen(
                workoutId = workoutId,
                groupName = groupName,
                onBack = { navController.popBackStack() }
            )
        }

    }
}