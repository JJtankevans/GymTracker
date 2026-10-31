package com.vitoroliveira.gymtracker.presentation.ranked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitoroliveira.gymtracker.data.model.RankedBoardRow
import com.vitoroliveira.gymtracker.data.model.RankedDuel
import com.vitoroliveira.gymtracker.data.model.RankedLastResult
import com.vitoroliveira.gymtracker.data.model.RankedPlayer
import com.vitoroliveira.gymtracker.domain.repository.RankedRepository
import com.vitoroliveira.gymtracker.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

data class RankedUiState(
    val isLoading: Boolean = false,
    val me: String? = null,
    /** Meu RP; null = ainda não entrei na ranqueada. */
    val player: RankedPlayer? = null,
    val board: List<RankedBoardRow> = emptyList(),
    val lastResult: RankedLastResult? = null,
    val duels: List<RankedDuel> = emptyList(),
    /** Segunda-feira da semana de agora (fuso de Brasília, como o banco). */
    val weekStart: LocalDate? = null,
    val today: LocalDate? = null,
    val message: String? = null,
    val error: String? = null,
    /** A carga inicial falhou (rede ou migração 018 não rodada): a aba mostra o motivo e "Tentar de novo". */
    val loadError: String? = null,
    /** Uma ação (entrar, desafiar, responder) está em andamento. */
    val busy: Boolean = false
) {
    val rank: Rank? get() = player?.let { rankFor(it.rp) }
    val season: SeasonInfo? get() = weekStart?.let(::seasonOf)
    val myRow: RankedBoardRow? get() = board.firstOrNull { it.isMe }
    val analysis: BoardAnalysis? get() = myRow?.let { me -> analyzeBoard(board.map { it.points }, me.points) }
    val daysLeft: Int? get() = today?.let(::weekDaysLeft)
}

/** Mensagens de regra que o banco devolve em português (raise exception): mostradas como vieram em vez do texto genérico. */
private val RULE_MESSAGES = listOf(
    "x1 valendo RP é só entre pessoas do mesmo elo",
    "Já existe um x1 em aberto entre vocês",
    "Essa pessoa ainda não está na ranqueada",
    "Entre na ranqueada primeiro",
    "Não dá para desafiar a si mesmo",
    "Esse pedido de x1 não está mais aberto",
    "Você não pode responder a esse x1"
)

internal fun ruleMessage(text: String?): String? = RULE_MESSAGES.firstOrNull { text?.contains(it, ignoreCase = true) == true }

class RankedViewModel(
    private val repo: RankedRepository,
    private val clock: Clock = Clock.System
) : ViewModel() {
    private val _state = MutableStateFlow(RankedUiState())
    val state: StateFlow<RankedUiState> = _state.asStateFlow()

    private val zone = TimeZone.of("America/Sao_Paulo")

    init { refresh() }

    private fun update(block: RankedUiState.() -> RankedUiState) { _state.value = _state.value.block() }

    /** Um extra que pode falhar sem derrubar a aba. */
    private suspend fun <T> quietly(block: suspend () -> T): T? = try { block() } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }

    fun refresh() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null, loadError = null) }
            val today = clock.todayIn(zone)
            val monday = mondayOf(today)
            val me = runCatching { repo.currentUserId() }.getOrNull()
            val player = try { repo.player() } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(isLoading = false, me = me, weekStart = monday, today = today, loadError = e.toUserMessage("Erro ao carregar a ranqueada")) }
                return@launch
            }
            if (player == null) {
                update { copy(isLoading = false, me = me, player = null, board = emptyList(), duels = emptyList(), lastResult = null, weekStart = monday, today = today) }
                return@launch
            }
            // fecha a semana passada e os x1 vencidos (idempotente: só a primeira pessoa a abrir faz algo)
            quietly { repo.settleWeek(monday.minus(7, DateTimeUnit.DAY).toString()) }
            quietly { repo.settleDuels() }
            val fresh = quietly { repo.player() } ?: player
            val board = quietly { repo.board() }.orEmpty()
            val last = quietly { repo.lastResult() }
            val duels = quietly { repo.duels() }.orEmpty()
            update { copy(isLoading = false, me = me, player = fresh, board = board, lastResult = last, duels = duels, weekStart = monday, today = today) }
        }
    }

    private fun act(errorFallback: String, success: String?, block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            update { copy(busy = true, error = null) }
            try {
                block()
                update { copy(busy = false, message = success) }
                refresh()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { copy(busy = false, error = ruleMessage(e.message) ?: e.toUserMessage(errorFallback)) }
            }
        }
    }

    fun join() = act("Erro ao entrar na ranqueada", "Você entrou na ranqueada! Bora subir de elo.") { repo.join() }

    /** [ranked] = valendo RP (só entre pessoas do mesmo elo) ou amistoso. */
    fun challenge(opponentId: String, ranked: Boolean) =
        act("Erro ao desafiar", if (ranked) "x1 valendo RP enviado" else "x1 amistoso enviado") { repo.createDuel(opponentId, ranked) }

    fun respond(duelId: String, accept: Boolean) =
        act("Erro ao responder o x1", if (accept) "x1 aceito: valem 7 dias!" else "x1 recusado") { repo.respondDuel(duelId, accept) }

    fun clearMessages() = update { copy(message = null, error = null) }
}
