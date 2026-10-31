package com.vitoroliveira.gymtracker.presentation.social

import com.vitoroliveira.gymtracker.data.model.GroupMemberRow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Cargos dentro de um grupo (os mesmos valores do banco). */
object GroupRole {
    const val OWNER = "owner"
    const val ADMIN = "admin"
    const val MEMBER = "member"

    fun label(role: String): String = when (role) { OWNER -> "Dono"; ADMIN -> "Admin"; else -> "Membro" }
}

/** Quem pode tirar quem do grupo: o dono tira qualquer um (menos ele mesmo); o admin só tira membros comuns. */
fun canRemoveMember(myRole: String?, targetRole: String): Boolean = when (myRole) {
    GroupRole.OWNER -> targetRole != GroupRole.OWNER
    GroupRole.ADMIN -> targetRole == GroupRole.MEMBER
    else -> false
}

/** Só o dono promove a admin ou rebaixa a membro. */
fun canChangeRole(myRole: String?, targetRole: String): Boolean = myRole == GroupRole.OWNER && targetRole != GroupRole.OWNER

/** O cargo para o qual o botão leva: admin vira membro; membro vira admin. */
fun toggledRole(current: String): String = if (current == GroupRole.ADMIN) GroupRole.MEMBER else GroupRole.ADMIN

/** O autor apaga a própria mensagem; dono e admins apagam qualquer uma. */
fun canDeleteMessage(myRole: String?, authorId: String, me: String?): Boolean =
    authorId == me || myRole == GroupRole.OWNER || myRole == GroupRole.ADMIN

const val MAX_MESSAGE_LENGTH = 280

/** A mensagem pronta para enviar (sem espaços nas pontas); null se vazia ou maior que [MAX_MESSAGE_LENGTH]. */
fun cleanMessage(text: String): String? = text.trim().takeIf { it.isNotEmpty() && it.length <= MAX_MESSAGE_LENGTH }

/** "agora", "5 min", "3 h", "ontem" ou "12/11" a partir do `created_at` do banco. */
fun messageAge(createdAt: String, now: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val at = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return ""
    val minutes = (now - at).inWholeMinutes
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> "${minutes / 60} h"
        minutes < 48 * 60 -> "ontem"
        else -> at.toLocalDateTime(zone).let { "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}" }
    }
}

/**
 * Posição de cada membro no ranking da semana: mais treinos, depois mais dias ativos, depois mais minutos; empate nos
 * três divide a posição. Quem não treinou na semana fica sem posição (null).
 */
fun rankMembers(rows: List<GroupMemberRow>): List<Pair<Int?, GroupMemberRow>> = rows.map { r ->
    val position = if (r.workouts <= 0) null else 1 + rows.count { o ->
        o.workouts > r.workouts ||
            (o.workouts == r.workouts && o.activeDays > r.activeDays) ||
            (o.workouts == r.workouts && o.activeDays == r.activeDays && o.minutes > r.minutes)
    }
    position to r
}.sortedWith(compareBy({ it.first ?: Int.MAX_VALUE }, { it.second.username.lowercase() }))
