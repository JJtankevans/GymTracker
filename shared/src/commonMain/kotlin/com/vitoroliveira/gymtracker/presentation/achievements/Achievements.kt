package com.vitoroliveira.gymtracker.presentation.achievements

import com.vitoroliveira.gymtracker.presentation.DashboardCalculator
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Uma medalha. Cada família (treinos, sequência...) tem vários níveis, um [Achievement] por nível. */
data class Achievement(
    val id: String,
    val family: AchievementFamily,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int
) {
    val unlocked: Boolean get() = progress >= target
    val fraction: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)
}

enum class AchievementFamily(val label: String) {
    WORKOUTS("Treinos"), STREAK("Dias seguidos"), WEEKS("Semanas com a meta"), VOLUME("Volume total"), RECORDS("Recordes")
}

/** O que a tela sabe do usuário. Null = dado indisponível (a família some em vez de mostrar 0 errado). */
data class AchievementInput(
    val workoutDates: List<LocalDate>,
    val weeklyGoal: Int,
    val totalVolumeKg: Double? = null,
    val exercisesWithRecord: Int? = null
)

object Achievements {
    private val workouts = listOf(1 to "Primeiro passo", 10 to "Frequentador", 50 to "Rato de academia", 100 to "Morador da academia", 250 to "Lenda viva")
    private val streaks = listOf(3 to "Embalado", 7 to "Semana perfeita", 14 to "Imparável", 30 to "Mês de ferro")
    private val weeks = listOf(4 to "Constância", 12 to "Trimestre firme", 26 to "Meio ano no foco", 52 to "Ano de ferro")
    private val tonnes = listOf(10 to "10 toneladas", 100 to "100 toneladas", 500 to "500 toneladas", 1000 to "Mil toneladas")
    private val records = listOf(1 to "Primeiro recorde", 10 to "Colecionador de recordes", 25 to "Máquina de recordes")

    /** Maior sequência de dias seguidos com treino, em toda a história. */
    fun longestStreak(dates: List<LocalDate>): Int {
        val days = dates.distinct().sorted()
        if (days.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until days.size) {
            run = if (days[i] == days[i - 1].plus(1, DateTimeUnit.DAY)) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    /** Semanas (segunda a domingo) com pelo menos [goal] treinos. */
    fun weeksMeetingGoal(dates: List<LocalDate>, goal: Int): Int =
        dates.groupingBy { DashboardCalculator.weekStart(it) }.eachCount().count { it.value >= goal.coerceAtLeast(1) }

    fun compute(input: AchievementInput): List<Achievement> {
        fun family(f: AchievementFamily, tiers: List<Pair<Int, String>>, progress: Int, describe: (Int) -> String) =
            tiers.map { (target, title) -> Achievement("${f.name}_$target", f, title, describe(target), progress, target) }

        return buildList {
            addAll(family(AchievementFamily.WORKOUTS, workouts, input.workoutDates.size) {
                if (it == 1) "Termine seu primeiro treino" else "Termine $it treinos"
            })
            addAll(family(AchievementFamily.STREAK, streaks, longestStreak(input.workoutDates)) { "Treine $it dias seguidos" })
            addAll(family(AchievementFamily.WEEKS, weeks, weeksMeetingGoal(input.workoutDates, input.weeklyGoal)) {
                "Bata sua meta semanal (${input.weeklyGoal} treinos) em $it semanas"
            })
            input.totalVolumeKg?.let { kg ->
                addAll(family(AchievementFamily.VOLUME, tonnes, (kg / 1000).toInt()) { "Levante $it toneladas somando todas as séries" })
            }
            input.exercisesWithRecord?.let { n ->
                addAll(family(AchievementFamily.RECORDS, records, n) {
                    if (it == 1) "Tenha um recorde de carga registrado" else "Tenha recorde de carga em $it exercícios"
                })
            }
        }
    }
}
