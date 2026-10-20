package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.CapturedTrack
import com.vitoroliveira.gymtracker.data.model.WorkoutTrack
import com.vitoroliveira.gymtracker.domain.repository.TrackRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class TrackRepositoryImpl(private val supabase: SupabaseClient) : TrackRepository {
    override suspend fun uploadTracks(workoutId: String, tracks: List<CapturedTrack>) {
        if (tracks.isEmpty()) return
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Usuário não autenticado")
        supabase.postgrest["workout_tracks"].insert(
            tracks.map {
                WorkoutTrack(workoutId, userId, it.title.take(200), it.artist?.take(200), it.album?.take(200), it.sourceApp, it.playedAtIso)
            }
        )
    }
}
