package com.vitoroliveira.gymtracker.presentation

/**
 * Fim do descanso: tanto o app (se estiver vivo) quanto o alarme do sistema querem avisar. Só um dos dois deve:
 * quem chega primeiro "reivindica" o fim daquele descanso ([endsAt], o instante em que ele acaba) e o outro fica quieto.
 * Sem [endsAt] (0) não há como reconhecer duplicata, então sempre avisa.
 */
class RestAlertClaim {
    private var handled = 0L

    /** true se este fim de descanso ainda não foi avisado (e passa a contar como avisado). */
    fun claim(endsAt: Long): Boolean {
        if (endsAt != 0L && endsAt == handled) return false
        handled = endsAt
        return true
    }
}
