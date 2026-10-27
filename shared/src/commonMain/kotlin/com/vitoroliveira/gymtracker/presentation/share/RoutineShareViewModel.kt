package com.vitoroliveira.gymtracker.presentation.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.MuscleGroup
import com.vitoroliveira.gymtracker.data.model.NewSharedRoutine
import com.vitoroliveira.gymtracker.data.model.SharedExercise
import com.vitoroliveira.gymtracker.data.model.SharedRoutine
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.RoutineShareRepository
import com.vitoroliveira.gymtracker.domain.usecase.ImportSharedRoutine
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Código pronto para mandar: o grupo e o código gerado. */
data class SharedCode(val groupName: String, val code: String)

data class RoutineShareUiState(
    val isWorking: Boolean = false,
    val shared: SharedCode? = null,
    /** Treino achado pelo código, esperando a pessoa confirmar a importação. */
    val preview: SharedRoutine? = null,
    /** Muda a cada importação concluída: a tela de grupos recarrega a lista. */
    val importedCount: Int = 0,
    val message: String? = null,
    val error: String? = null
)

/**
 * Acha o código no que a pessoa digitou ou colou: aceita minúsculas, traço ("K7F2-Q9XA"), espaços nas pontas e a frase
 * inteira do convite (o código é a última "palavra" de 8 letras/números). Null se não achar.
 */
fun normalizeShareCode(input: String): String? {
    val words = input.split(Regex("[^A-Za-z0-9-]+")).map { it.replace("-", "") }
    words.lastOrNull { it.length == 8 && it.all { c -> c.isLetterOrDigit() } }?.let { return it.uppercase() }
    // digitado com espaço no meio ("K7F2 Q9XA")
    val compact = input.filter { it.isLetterOrDigit() && it.code < 128 }
    return compact.takeIf { it.length == 8 }?.uppercase()
}

const val MAX_SHARED_EXERCISES = 40

class RoutineShareViewModel(
    private val shares: RoutineShareRepository,
    private val groups: MuscleGroupRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RoutineShareUiState())
    val state: StateFlow<RoutineShareUiState> = _state.asStateFlow()

    private fun update(f: RoutineShareUiState.() -> RoutineShareUiState) { _state.value = _state.value.f() }

    /** Gera o código do grupo (cópia do nome, cor e exercícios de agora). */
    fun share(group: MuscleGroup) {
        val id = group.id ?: return
        if (_state.value.isWorking) return
        viewModelScope.launch {
            update { copy(isWorking = true, error = null) }
            try {
                val list = exercises.getByMuscleGroup(id)
                if (list.isEmpty()) {
                    update { copy(isWorking = false, error = "Adicione exercícios ao grupo antes de compartilhar") }
                    return@launch
                }
                val snapshot = list.take(MAX_SHARED_EXERCISES).map { SharedExercise(it.name, it.defaultSets, it.defaultReps, it.notes) }
                val code = shares.share(NewSharedRoutine(group.name, group.colorHex, snapshot))
                update { copy(isWorking = false, shared = SharedCode(group.name, code)) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(isWorking = false, error = e.toUserMessage("Erro ao compartilhar o treino")) }
            }
        }
    }

    /** Procura o treino pelo código digitado ou colado. */
    fun lookup(input: String) {
        val code = normalizeShareCode(input)
        if (code == null) {
            update { copy(error = "Digite o código de 8 letras/números que seu amigo mandou") }
            return
        }
        viewModelScope.launch {
            update { copy(isWorking = true, error = null, preview = null) }
            try {
                val found = shares.find(code)
                update {
                    if (found == null) copy(isWorking = false, error = "Nenhum treino com o código $code")
                    else copy(isWorking = false, preview = found)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(isWorking = false, error = e.toUserMessage("Erro ao buscar o treino")) }
            }
        }
    }

    /** Importa o treino da prévia como um grupo novo. */
    fun confirmImport() {
        val routine = _state.value.preview ?: return
        viewModelScope.launch {
            update { copy(isWorking = true, error = null) }
            try {
                val names = groups.getAll().map { it.name }
                val result = ImportSharedRoutine(groups, exercises)(routine, names)
                // conta a importação para quem compartilhou; se falhar, o treino já foi importado mesmo assim
                runCatching { shares.find(routine.code, countImport = true) }
                update {
                    copy(
                        isWorking = false, preview = null, importedCount = importedCount + 1,
                        message = "Treino '${result.groupName}' importado com ${result.exercises} exercícios"
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(isWorking = false, error = e.toUserMessage("Erro ao importar o treino")) }
            }
        }
    }

    fun closeShared() = update { copy(shared = null) }
    fun closePreview() = update { copy(preview = null) }
    fun clearMessages() = update { copy(message = null, error = null) }
}

/** Convite pronto para o WhatsApp etc. */
fun shareInvite(code: SharedCode): String =
    "Bora treinar junto! Importe meu treino \"${code.groupName}\" no GymTracker: Grupos > Importar treino > código ${code.code}"
