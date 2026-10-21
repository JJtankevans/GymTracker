package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.domain.AvatarRules
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class MemProfiles(var profile: Profile = Profile(id = "u1", username = "thiago")) : ProfileRepository {
    var failUpload: Throwable? = null
    var uploads = 0
    var removals = 0
    override suspend fun getCurrentProfile() = profile
    override suspend fun updateProfile(profile: Profile) { this.profile = profile }
    override suspend fun uploadAvatar(jpeg: ByteArray): String {
        failUpload?.let { throw it }
        uploads++
        return "https://x.supabase.co/storage/v1/object/public/user_profile_images/u1/$uploads.jpg"
            .also { profile = profile.copy(avatarUrl = it) }
    }
    override suspend fun removeAvatar() { removals++; profile = profile.copy(avatarUrl = null) }
}

private class NoAuth : AuthRepository {
    override suspend fun signUp(email: String, password: String, username: String) {}
    override suspend fun signIn(email: String, password: String) {}
    override suspend fun signOut() {}
    override suspend fun awaitSession() = true
    override fun isLoggedIn() = true
    override fun currentUserId(): String? = "u1"
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val jpeg = ByteArray(1000) { 1 }

    @Test fun enviarFotoAtualizaOPerfilEMostraMensagem() {
        val repo = MemProfiles()
        val vm = ProfileViewModel(repo, NoAuth())
        assertNull(vm.uiState.value.profile?.avatarUrl)

        vm.uploadAvatar(jpeg)

        val s = vm.uiState.value
        assertEquals(1, repo.uploads)
        assertNotNull(s.profile?.avatarUrl)
        assertEquals("Foto de perfil atualizada", s.successMessage)
        assertFalse(s.isUpdatingAvatar)
        assertNull(s.errorMessage)
    }

    @Test fun fotoVaziaOuGrandeDemaisNaoChegaAoRepositorio() {
        val repo = MemProfiles()
        val vm = ProfileViewModel(repo, NoAuth())

        vm.uploadAvatar(ByteArray(0))
        assertEquals("Não foi possível ler essa imagem", vm.uiState.value.errorMessage)

        vm.clearMessages()
        vm.uploadAvatar(ByteArray(AvatarRules.MAX_BYTES + 1))
        assertEquals("A foto é grande demais (máximo 2 MB)", vm.uiState.value.errorMessage)
        assertEquals(0, repo.uploads)
    }

    @Test fun falhaNoEnvioMostraErroEMantemAFotoAnterior() {
        val repo = MemProfiles(Profile(id = "u1", username = "thiago", avatarUrl = "https://antiga"))
        repo.failUpload = RuntimeException("Unable to resolve host")
        val vm = ProfileViewModel(repo, NoAuth())

        vm.uploadAvatar(jpeg)

        val s = vm.uiState.value
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", s.errorMessage)
        assertEquals("https://antiga", s.profile?.avatarUrl)
        assertNull(s.successMessage)
        assertFalse(s.isUpdatingAvatar)
    }

    @Test fun removerFotoLimpaAUrl() {
        val repo = MemProfiles(Profile(id = "u1", username = "thiago", avatarUrl = "https://antiga"))
        val vm = ProfileViewModel(repo, NoAuth())

        vm.removeAvatar()

        assertEquals(1, repo.removals)
        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertEquals("Foto de perfil removida", vm.uiState.value.successMessage)
    }

    @Test fun removerSemFotoNaoFazNada() {
        val repo = MemProfiles()
        val vm = ProfileViewModel(repo, NoAuth())
        vm.removeAvatar()
        assertEquals(0, repo.removals)
        assertNull(vm.uiState.value.successMessage)
    }

    @Test fun imagemIlegivelMostraErro() {
        val vm = ProfileViewModel(MemProfiles(), NoAuth())
        vm.onAvatarReadFailed()
        assertEquals("Não foi possível ler essa imagem", vm.uiState.value.errorMessage)
    }
}
