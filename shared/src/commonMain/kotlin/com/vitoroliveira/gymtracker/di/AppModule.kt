// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/di/AppModule.kt

package com.vitoroliveira.gymtracker.di

import com.vitoroliveira.gymtracker.data.repository.AuthRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.ExerciseRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.MuscleGroupRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.ProfileRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.WorkoutRepositoryImpl
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.presentation.viewmodel.AuthViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseDetailViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.ExerciseViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.HistoryViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.HomeViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.MuscleGroupViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.ProfileViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.SessionViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutDetailViewModel
import com.vitoroliveira.gymtracker.presentation.viewmodel.WorkoutViewModel
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

fun appModule(supabaseClient: SupabaseClient) = module {
    // Cliente Supabase
    single { supabaseClient }

    // Repositórios
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<MuscleGroupRepository> { MuscleGroupRepositoryImpl(get()) }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<com.vitoroliveira.gymtracker.presentation.body.BodyCloud> { com.vitoroliveira.gymtracker.data.repository.BodyCloudImpl(get()) }
    single<com.vitoroliveira.gymtracker.domain.repository.SocialRepository> {
        com.vitoroliveira.gymtracker.data.repository.SocialRepositoryImpl(get())
    }
    single<com.vitoroliveira.gymtracker.domain.repository.TrackRepository> { com.vitoroliveira.gymtracker.data.repository.TrackRepositoryImpl(get()) }

    // ViewModels
    viewModel { SessionViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    factory { com.vitoroliveira.gymtracker.domain.usecase.CreateWorkoutFromTemplate(get(), get()) }
    viewModel { MuscleGroupViewModel(get(), get()) }
    viewModel { ExerciseViewModel(get()) }
    // LocalUserData vem do módulo do app (dados guardados no aparelho, apagados ao sair da conta)
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { WorkoutViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.records.RecordsViewModel(get(), get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.social.SocialViewModel(get()) }
    single<com.vitoroliveira.gymtracker.domain.repository.RankedRepository> {
        com.vitoroliveira.gymtracker.data.repository.RankedRepositoryImpl(get())
    }
    viewModel { com.vitoroliveira.gymtracker.presentation.ranked.RankedViewModel(get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.achievements.AchievementsViewModel(get(), get(), get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.workout.ExportViewModel(get(), get(), get()) }
    single<com.vitoroliveira.gymtracker.domain.repository.RoutineShareRepository> { com.vitoroliveira.gymtracker.data.repository.RoutineShareRepositoryImpl(get()) }
    viewModel { com.vitoroliveira.gymtracker.presentation.share.RoutineShareViewModel(get(), get(), get()) }
    viewModel { (workoutId: String) -> WorkoutDetailViewModel(workoutId, get(), get()) }
    viewModel { (exerciseId: String) -> ExerciseDetailViewModel(exerciseId, get()) }
}