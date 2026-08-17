// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/ProfileRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Profile

interface ProfileRepository {
    suspend fun getCurrentProfile(): Profile
    suspend fun updateProfile(profile: Profile)
}