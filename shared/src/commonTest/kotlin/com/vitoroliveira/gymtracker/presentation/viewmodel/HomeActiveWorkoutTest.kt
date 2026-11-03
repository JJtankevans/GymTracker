package com.vitoroliveira.gymtracker.presentation.viewmodel

import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.data.model.Workout
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private class OneProfile : ProfileRepository {
    override suspend fun getCurrentProfile() = Profile(id = "u1", username = "thiago")
    override suspend fun updateProfile(profile: Profile) {}
    override suspend fun uploadAvatar(jpeg: ByteArray) = ""
    override suspend fun removeAvatar() {}
}

private class Groups(val rows: List<MuscleGroup>) : MuscleGroupRepository {
    override suspend fun getAll() = rows
    override suspend fun insert(muscleGroup: MuscleGroup) = muscleGroup
    override suspend fun update(muscleGroup: MuscleGroup) {}
    override suspend fun delete(id: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeActiveWorkoutTest {
    private lateinit var workouts: FakeWorkoutRepository
    private val peito = MuscleGroup(id = "g1", name = "Peito", colorHex = "#FF0000")

    @BeforeTest fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        workouts = FakeWorkoutRepository()
    }

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(vararg groups: MuscleGroup) = HomeViewModel(workouts, OneProfile(), Groups(groups.toList()))

    private fun startedMinutesAgo(m: Int) = (Clock.System.now() - m.minutes).toString()

    @Test fun semTreinoEmAndamentoNaoAvisa() {
        assertNull(vm(peito).uiState.value.activeWorkout)
    }

    @Test fun treinoEmAndamentoApareceComGrupoETempo() {
        workouts.activeWorkout = Workout(id = "w9", muscleGroupId = "g1", startedAt = startedMinutesAgo(45))
        val a = assertNotNull(vm(peito).uiState.value.activeWorkout)
        assertEquals("w9", a.workoutId)
        assertEquals("g1", a.groupId)
        assertEquals("Peito", a.groupName)
        assertTrue(a.minutes in 44..46, "minutos: ${a.minutes}")
    }

    @Test fun grupoApagadoUsaONomeGuardadoNoTreino() {
        workouts.activeWorkout = Workout(id = "w9", muscleGroupId = "g-apagado", muscleGroupName = "Costas", startedAt = startedMinutesAgo(5))
        assertEquals("Costas", assertNotNull(vm(peito).uiState.value.activeWorkout).groupName)
    }

    @Test fun treinoLivreTemNomeProprio() {
        workouts.activeWorkout = Workout(id = "w9", startedAt = startedMinutesAgo(5))
        assertEquals("Treino livre", assertNotNull(vm().uiState.value.activeWorkout).groupName)
    }

    @Test fun descartarApagaOTreinoEEscondeOAviso() {
        workouts.activeWorkout = Workout(id = "w9", muscleGroupId = "g1", startedAt = startedMinutesAgo(10))
        val vm = vm(peito)
        vm.discardActiveWorkout()
        assertNull(workouts.activeWorkout)
        assertNull(vm.uiState.value.activeWorkout)
    }

    @Test fun descartarComFalhaMantemOAvisoEMostraOErro() {
        workouts.activeWorkout = Workout(id = "w9", muscleGroupId = "g1", startedAt = startedMinutesAgo(10))
        val vm = vm(peito)
        workouts.failDelete = RuntimeException("Unable to resolve host")
        vm.discardActiveWorkout()
        assertNotNull(vm.uiState.value.activeWorkout)
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente de novo.", vm.uiState.value.errorMessage)
    }

    @Test fun falhaNaConsultaNaoDerrubaAHome() {
        workouts.failWith = RuntimeException("Unable to resolve host")
        val s = vm(peito).uiState.value
        assertNull(s.activeWorkout)
        assertNull(s.errorMessage)
    }
}
