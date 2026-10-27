package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
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
import kotlin.test.assertTrue

/** [confirmEmail] = true imita o Supabase com "Confirm email" ligado: o cadastro não devolve sessão. */
private class SignUpAuth(private val confirmEmail: Boolean) : AuthRepository {
    var loggedIn = false
    val resent = mutableListOf<String>()
    var failResend: Throwable? = null
    override suspend fun signUp(email: String, password: String, username: String) { if (!confirmEmail) loggedIn = true }
    override suspend fun resendSignUpConfirmation(email: String) { failResend?.let { throw it }; resent += email }
    override suspend fun signIn(email: String, password: String) { loggedIn = true }
    override suspend fun signOut() { loggedIn = false }
    override suspend fun awaitSession() = loggedIn
    override fun isLoggedIn() = loggedIn
    override fun currentUserId(): String? = if (loggedIn) "u1" else null
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun cadastroSemConfirmacaoEntraDireto() {
        val vm = AuthViewModel(SignUpAuth(confirmEmail = false))
        vm.signUp("a@b.com", "123456", "ana")
        assertTrue(vm.uiState.value.isLoggedIn)
        assertNull(vm.uiState.value.confirmationEmail)
    }

    @Test fun cadastroComConfirmacaoNaoEntraEPedeParaConfirmar() {
        val vm = AuthViewModel(SignUpAuth(confirmEmail = true))
        vm.signUp(" a@b.com ", "123456", "ana")
        assertFalse(vm.uiState.value.isLoggedIn, "entrou na Home sem sessão")
        assertEquals("a@b.com", vm.uiState.value.confirmationEmail)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun reenviarConfirmacaoAvisa() {
        val auth = SignUpAuth(confirmEmail = true)
        val vm = AuthViewModel(auth)
        vm.signUp("a@b.com", "123456", "ana")
        vm.resendConfirmation()
        assertEquals(listOf("a@b.com"), auth.resent)
        assertNotNull(vm.uiState.value.infoMessage)
    }

    @Test fun erroAoReenviarViraMensagem() {
        val auth = SignUpAuth(confirmEmail = true).apply { failResend = RuntimeException("over_email_send_rate_limit") }
        val vm = AuthViewModel(auth)
        vm.signUp("a@b.com", "123456", "ana")
        vm.resendConfirmation()
        assertEquals("Muitas tentativas. Aguarde um pouco e tente de novo.", vm.uiState.value.errorMessage)
    }

    @Test fun fecharOAvisoLimpaOEmail() {
        val vm = AuthViewModel(SignUpAuth(confirmEmail = true))
        vm.signUp("a@b.com", "123456", "ana")
        vm.dismissConfirmation()
        assertNull(vm.uiState.value.confirmationEmail)
    }
}
