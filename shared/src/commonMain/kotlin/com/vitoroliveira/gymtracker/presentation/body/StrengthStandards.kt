package com.vitoroliveira.gymtracker.presentation.body

enum class Lift(val label: String) {
    BENCH("Supino"), SQUAT("Agachamento"), DEADLIFT("Levantamento terra"), OVERHEAD("Desenvolvimento")
}

enum class StrengthLevel(val label: String, val percentile: Int) {
    BEGINNER("Iniciante", 5), NOVICE("Novato", 25), INTERMEDIATE("Intermediário", 50),
    ADVANCED("Avançado", 75), ELITE("Elite", 95)
}

/**
 * Padrões de força: quanto de 1RM (carga máxima em 1 repetição) vale cada nível, em múltiplos do peso corporal.
 *
 * Fonte: tabelas da StrengthLog (logs de ~17 mil homens e ~7 mil mulheres), convertidas em múltiplo do
 * peso corporal para ~80 kg (homens) e ~60 kg (mulheres). Os níveis são percentis: Iniciante = mais forte que
 * 5% das pessoas, Novato 25%, Intermediário 50%, Avançado 75%, Elite 95%.
 *
 * Limites: o múltiplo cai um pouco para quem é mais pesado e sobe para quem é mais leve, e os dados vêm de
 * pessoas que registram treino em app (mais ativas que a média da população). É uma referência, não um diagnóstico.
 */
object StrengthStandards {
    private val male = mapOf(
        Lift.BENCH to doubleArrayOf(0.68, 0.93, 1.13, 1.36, 1.79),
        Lift.SQUAT to doubleArrayOf(0.71, 1.11, 1.40, 1.71, 2.25),
        Lift.DEADLIFT to doubleArrayOf(0.91, 1.39, 1.78, 2.15, 2.70),
        Lift.OVERHEAD to doubleArrayOf(0.40, 0.56, 0.66, 0.78, 0.99)
    )
    private val female = mapOf(
        Lift.BENCH to doubleArrayOf(0.37, 0.55, 0.70, 0.85, 1.25),
        Lift.SQUAT to doubleArrayOf(0.40, 0.80, 1.12, 1.42, 1.95),
        Lift.DEADLIFT to doubleArrayOf(0.63, 1.07, 1.35, 1.72, 2.25),
        Lift.OVERHEAD to doubleArrayOf(0.23, 0.38, 0.47, 0.57, 0.72)
    )

    /** Sem sexo informado, usa a média dos dois (aproximação). */
    private fun ratios(lift: Lift, sex: Sex?): DoubleArray {
        val m = male.getValue(lift); val f = female.getValue(lift)
        return when (sex) {
            Sex.MALE -> m
            Sex.FEMALE -> f
            null -> DoubleArray(5) { (m[it] + f[it]) / 2 }
        }
    }

    /** 1RM de referência (kg) para cada nível. */
    fun oneRepMaxByLevel(lift: Lift, bodyKg: Double, sex: Sex?): Map<StrengthLevel, Double> {
        val r = ratios(lift, sex)
        return StrengthLevel.entries.mapIndexed { i, l -> l to r[i] * bodyKg }.toMap()
    }

    /** Maior nível já alcançado por [oneRm]; null se abaixo de Iniciante. */
    fun levelFor(lift: Lift, oneRm: Double, bodyKg: Double, sex: Sex?): StrengthLevel? =
        oneRepMaxByLevel(lift, bodyKg, sex).filterValues { oneRm >= it }.keys.lastOrNull()

    fun nextLevel(level: StrengthLevel?): StrengthLevel? =
        if (level == null) StrengthLevel.BEGINNER else StrengthLevel.entries.getOrNull(level.ordinal + 1)

    /** Reconhece o exercício pelo nome (pt-BR e inglês). Só os 4 básicos têm padrão. */
    fun liftFor(exerciseName: String): Lift? {
        val n = exerciseName.lowercase()
        fun has(vararg k: String) = k.any { n.contains(it) }
        return when {
            has("halter", "dumbbell", "máquina", "maquina", "machine", "smith") && !has("barra") -> null
            has("inclinado", "declinado", "incline", "decline", "crucifixo", "fly") -> null
            has("supino", "bench press") -> Lift.BENCH
            has("agachamento", "squat") && !has("búlgaro", "bulgaro", "bulgarian", "hack", "frontal", "front") -> Lift.SQUAT
            has("levantamento terra", "terra", "deadlift") && !has("romeno", "stiff", "sumô", "sumo", "romanian") -> Lift.DEADLIFT
            has("desenvolvimento", "overhead press", "militar", "shoulder press") -> Lift.OVERHEAD
            else -> null
        }
    }
}

/** Estimativa de 1RM (Epley). Para 1 rep é a própria carga. */
fun estimateOneRepMax(weightKg: Double, reps: Int): Double =
    if (reps <= 1) weightKg else weightKg * (1 + reps / 30.0)

/** Carga que se levantaria em [reps] repetições para um 1RM dado (inverso de Epley). */
fun loadForReps(oneRm: Double, reps: Int): Double =
    if (reps <= 1) oneRm else oneRm / (1 + reps / 30.0)
