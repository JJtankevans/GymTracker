// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/domain/repository/ProfileRepository.kt

package com.vitoroliveira.gymtracker.domain.repository

import com.vitoroliveira.gymtracker.data.model.Profile

interface ProfileRepository {
    suspend fun getCurrentProfile(): Profile
    suspend fun updateProfile(profile: Profile)

    /** Envia a foto (JPEG já recortado e reduzido) para o Storage, grava a URL pública em `profiles.avatar_url`, apaga as fotos antigas e devolve a URL. */
    suspend fun uploadAvatar(jpeg: ByteArray): String

    /** Apaga as fotos do usuário no Storage e limpa `profiles.avatar_url`. */
    suspend fun removeAvatar()
}
