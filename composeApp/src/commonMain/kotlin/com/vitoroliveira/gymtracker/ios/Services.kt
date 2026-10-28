package com.vitoroliveira.gymtracker.ios

import com.vitoroliveira.gymtracker.data.repository.AuthRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.ExerciseRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.MuscleGroupRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.ProfileRepositoryImpl
import com.vitoroliveira.gymtracker.data.repository.WorkoutRepositoryImpl
import com.vitoroliveira.gymtracker.di.createGymTrackerSupabaseClient

/**
 * Repositórios do :shared para o app de iPhone (sem Koin: poucas dependências e nenhuma mágica).
 * O cliente do Supabase guarda a sessão no aparelho sozinho (NSUserDefaults no iOS).
 */
object Services {
    val configured: Boolean get() = AppConfig.SUPABASE_URL.isNotBlank() && AppConfig.SUPABASE_KEY.isNotBlank()

    private val client by lazy { createGymTrackerSupabaseClient(AppConfig.SUPABASE_URL, AppConfig.SUPABASE_KEY) }

    val auth by lazy { AuthRepositoryImpl(client) }
    val groups by lazy { MuscleGroupRepositoryImpl(client) }
    val exercises by lazy { ExerciseRepositoryImpl(client) }
    val workouts by lazy { WorkoutRepositoryImpl(client) }
    val profiles by lazy { ProfileRepositoryImpl(client) }
}
