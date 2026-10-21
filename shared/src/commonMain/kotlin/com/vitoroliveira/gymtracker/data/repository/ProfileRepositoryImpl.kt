// shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/repository/ProfileRepositoryImpl.kt

package com.vitoroliveira.gymtracker.data.repository

import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.domain.AvatarRules
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class ProfileRepositoryImpl(
    private val supabase: SupabaseClient
) : ProfileRepository {

    override suspend fun getCurrentProfile(): Profile {
        val userId = currentUserId()

        return supabase.postgrest["profiles"]
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle()
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id
            ?: throw IllegalStateException("Perfil sem id — carregue o perfil antes de atualizar")

        supabase.postgrest["profiles"]
            .update(profile) {
                filter { eq("id", id) }
            }
    }

    /**
     * A foto vai para `<id do usuário>/<milissegundos>.jpg`: nome novo a cada troca, então a URL muda e nenhum cache
     * (do CDN ou do aparelho) mostra a foto antiga. A RLS do Storage só deixa gravar na pasta do próprio usuário.
     */
    override suspend fun uploadAvatar(jpeg: ByteArray): String {
        val userId = currentUserId()
        val bucket = supabase.storage.from(AvatarRules.BUCKET)
        val path = "$userId/${Clock.System.now().toEpochMilliseconds()}.jpg"

        bucket.upload(path, jpeg) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        val url = bucket.publicUrl(path)
        setAvatarUrl(userId, url)
        deleteAvatarFiles(bucket, userId, keep = path)
        return url
    }

    override suspend fun removeAvatar() {
        val userId = currentUserId()
        // primeiro limpa a URL: se apagar os arquivos falhar, o perfil já não aponta para a foto
        setAvatarUrl(userId, null)
        deleteAvatarFiles(supabase.storage.from(AvatarRules.BUCKET), userId, keep = null)
    }

    private fun currentUserId(): String =
        supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Usuário não autenticado")

    private suspend fun setAvatarUrl(userId: String, url: String?) {
        supabase.postgrest["profiles"].update({ set<String?>("avatar_url", url) }) {
            filter { eq("id", userId) }
        }
    }

    /** Apaga as fotos da pasta do usuário, menos [keep]. Faxina: se falhar, a foto nova já está valendo, então não propaga o erro. */
    private suspend fun deleteAvatarFiles(bucket: BucketApi, userId: String, keep: String?) {
        try {
            val old = bucket.list(userId)
                .map { "$userId/${it.name}" }
                .filter { it != keep }
            if (old.isNotEmpty()) bucket.delete(old)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }
}
