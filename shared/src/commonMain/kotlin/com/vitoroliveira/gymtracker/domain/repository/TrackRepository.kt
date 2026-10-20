package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.CapturedTrack

interface TrackRepository {
    /** Guarda as músicas do treino. Exige a migração 002_social.sql no Supabase. */
    suspend fun uploadTracks(workoutId: String, tracks: List<CapturedTrack>)
}
