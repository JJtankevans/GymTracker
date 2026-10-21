package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.ProfileTitle
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.presentation.viewmodel.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SocialTitlesTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeSocialRepository) = SocialViewModel(repo, FixedClock(Instant.parse("2026-10-20T12:00:00Z")))

    @Test fun carregaOTituloDeCadaPessoaEOMeu() {
        val repo = FakeSocialRepository().apply {
            profiles += SocialProfile(id = "p1", username = "ana")
            profiles += SocialProfile(id = "p2", username = "beto")
            titleList += ProfileTitle("p1", "honor", level = 2, challengeId = "c")
            titleList += ProfileTitle("me", "honor", level = 4, challengeId = "c")
        }
        val s = vm(repo).state.value
        assertEquals(setOf("p1", "me"), s.titles.keys)
        assertEquals("Chad", ProfileTitles.text(s.titles.getValue("p1")))
        assertEquals("Mr. Olimpia", ProfileTitles.text(s.titles.getValue("me")))
        assertNull(s.titles["p2"])
    }

    @Test fun semAMigracaoOsTitulosFicamVaziosSemMostrarErro() {
        val repo = FakeSocialRepository().apply {
            profiles += SocialProfile(id = "p1", username = "ana")
            failTitles = true
        }
        val s = vm(repo).state.value
        assertEquals(emptyMap(), s.titles)
        assertNull(s.error)
        assertEquals(1, s.people.size) // o resto da tela carregou normalmente
    }
}
