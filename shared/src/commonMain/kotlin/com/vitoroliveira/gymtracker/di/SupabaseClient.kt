// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/di/SupabaseClient.kt

package com.vitoroliveira.gymtracker.di

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.SupabaseClient

fun createGymTrackerSupabaseClient(
    supabaseUrl: String,
    supabaseKey: String
): SupabaseClient = createSupabaseClient(
    supabaseUrl = supabaseUrl,
    supabaseKey = supabaseKey
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}