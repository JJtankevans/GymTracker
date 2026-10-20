package com.vitoroliveira.gymtracker.data.catalog


enum class CatalogMuscle(val label: String) {
    PEITO("Peito"), COSTAS("Costas"), OMBROS("Ombros"), BICEPS("Bíceps"), TRICEPS("Tríceps"),
    QUADRICEPS("Quadríceps"), POSTERIOR("Posterior"), GLUTEOS("Glúteos"),
    PANTURRILHA("Panturrilha"), ABDOMEN("Abdômen")
}

enum class Equipment(val label: String) {
    BARRA("Barra"), HALTERES("Halteres"), MAQUINA("Máquina"), POLIA("Polia"),
    PESO_CORPO("Peso do corpo"), SMITH("Smith")
}

private typealias M = CatalogMuscle
private typealias Q = Equipment

/**
 * Exercício do catálogo. O nome é o padrão do app: quem escolhe daqui grava sempre o mesmo texto,
 * então dá pra comparar cargas entre treinos e entre pessoas pelo nome.
 */
data class CatalogExercise(
    val name: String,
    val muscle: CatalogMuscle,
    val equipment: Equipment,
    val sets: Int,
    val reps: Int,
    /** Vai pro campo de observações ao criar (ex.: reps contadas em segundos). */
    val hint: String? = null
)

object ExerciseCatalog {

    private fun e(
        name: String, muscle: M, equipment: Q,
        sets: Int, reps: Int, hint: String? = null
    ) = CatalogExercise(name, muscle, equipment, sets, reps, hint)

    val all: List<CatalogExercise> = listOf(
        // Peito
        e("Supino Reto com Barra", M.PEITO, Q.BARRA, 4, 8),
        e("Supino Inclinado com Barra", M.PEITO, Q.BARRA, 3, 10),
        e("Supino Reto com Halteres", M.PEITO, Q.HALTERES, 3, 10),
        e("Supino Inclinado com Halteres", M.PEITO, Q.HALTERES, 3, 10),
        e("Crucifixo com Halteres", M.PEITO, Q.HALTERES, 3, 12),
        e("Supino na Máquina", M.PEITO, Q.MAQUINA, 3, 12),
        e("Crucifixo na Máquina (Peck Deck)", M.PEITO, Q.MAQUINA, 3, 12),
        e("Crossover na Polia", M.PEITO, Q.POLIA, 3, 12),
        e("Flexão de Braço", M.PEITO, Q.PESO_CORPO, 3, 15),
        // Costas
        e("Barra Fixa", M.COSTAS, Q.PESO_CORPO, 4, 8),
        e("Puxada Frontal na Polia", M.COSTAS, Q.POLIA, 4, 10),
        e("Remada Baixa na Polia", M.COSTAS, Q.POLIA, 3, 12),
        e("Remada Curvada com Barra", M.COSTAS, Q.BARRA, 4, 8),
        e("Levantamento Terra", M.COSTAS, Q.BARRA, 4, 5),
        e("Remada Unilateral com Halter", M.COSTAS, Q.HALTERES, 3, 10),
        e("Remada na Máquina", M.COSTAS, Q.MAQUINA, 3, 12),
        e("Pulldown na Polia", M.COSTAS, Q.POLIA, 3, 12),
        // Ombros
        e("Desenvolvimento com Barra", M.OMBROS, Q.BARRA, 4, 8),
        e("Desenvolvimento com Halteres", M.OMBROS, Q.HALTERES, 4, 10),
        e("Desenvolvimento na Máquina", M.OMBROS, Q.MAQUINA, 3, 12),
        e("Elevação Lateral", M.OMBROS, Q.HALTERES, 3, 15),
        e("Elevação Frontal", M.OMBROS, Q.HALTERES, 3, 12),
        e("Crucifixo Inverso", M.OMBROS, Q.MAQUINA, 3, 15),
        e("Encolhimento com Halteres", M.OMBROS, Q.HALTERES, 3, 12),
        // Bíceps
        e("Rosca Direta com Barra", M.BICEPS, Q.BARRA, 3, 10),
        e("Rosca Alternada com Halteres", M.BICEPS, Q.HALTERES, 3, 12),
        e("Rosca Martelo", M.BICEPS, Q.HALTERES, 3, 12),
        e("Rosca Scott", M.BICEPS, Q.MAQUINA, 3, 10),
        e("Rosca na Polia", M.BICEPS, Q.POLIA, 3, 12),
        // Tríceps
        e("Tríceps Testa", M.TRICEPS, Q.BARRA, 3, 10),
        e("Tríceps Pulley", M.TRICEPS, Q.POLIA, 3, 12),
        e("Tríceps Corda", M.TRICEPS, Q.POLIA, 3, 12),
        e("Tríceps Francês", M.TRICEPS, Q.HALTERES, 3, 12),
        e("Mergulho nas Paralelas", M.TRICEPS, Q.PESO_CORPO, 3, 10),
        e("Supino Fechado", M.TRICEPS, Q.BARRA, 3, 8),
        // Quadríceps
        e("Agachamento Livre", M.QUADRICEPS, Q.BARRA, 4, 8),
        e("Agachamento no Smith", M.QUADRICEPS, Q.SMITH, 4, 10),
        e("Leg Press 45°", M.QUADRICEPS, Q.MAQUINA, 4, 12),
        e("Hack Squat", M.QUADRICEPS, Q.MAQUINA, 3, 10),
        e("Cadeira Extensora", M.QUADRICEPS, Q.MAQUINA, 3, 15),
        e("Agachamento Búlgaro", M.QUADRICEPS, Q.HALTERES, 3, 10),
        e("Afundo com Halteres", M.QUADRICEPS, Q.HALTERES, 3, 12),
        // Posterior
        e("Mesa Flexora", M.POSTERIOR, Q.MAQUINA, 3, 12),
        e("Cadeira Flexora", M.POSTERIOR, Q.MAQUINA, 3, 12),
        e("Stiff com Barra", M.POSTERIOR, Q.BARRA, 3, 10),
        e("Levantamento Terra Romeno", M.POSTERIOR, Q.BARRA, 3, 8),
        // Glúteos
        e("Elevação Pélvica", M.GLUTEOS, Q.BARRA, 4, 10),
        e("Cadeira Abdutora", M.GLUTEOS, Q.MAQUINA, 3, 15),
        e("Glúteo na Polia (Coice)", M.GLUTEOS, Q.POLIA, 3, 12),
        // Panturrilha
        e("Panturrilha em Pé", M.PANTURRILHA, Q.MAQUINA, 4, 15),
        e("Panturrilha Sentado", M.PANTURRILHA, Q.MAQUINA, 4, 15),
        e("Panturrilha no Leg Press", M.PANTURRILHA, Q.MAQUINA, 4, 15),
        // Abdômen
        e("Abdominal Supra", M.ABDOMEN, Q.PESO_CORPO, 3, 20),
        e("Elevação de Pernas", M.ABDOMEN, Q.PESO_CORPO, 3, 12),
        e("Prancha", M.ABDOMEN, Q.PESO_CORPO, 3, 30, "Repetições = segundos por série"),
        e("Abdominal na Polia", M.ABDOMEN, Q.POLIA, 3, 15)
    )

    /** Procura sem diferenciar maiúsculas nem acentos no nome, músculo e equipamento. */
    fun search(query: String, muscle: M? = null): List<CatalogExercise> {
        val q = query.normalized()
        return all.filter { ex ->
            (muscle == null || ex.muscle == muscle) &&
                (q.isEmpty() || (ex.name + " " + ex.muscle.label + " " + ex.equipment.label).normalized().contains(q))
        }
    }

    /** Põe [added] no começo da lista de recentes (sem repetir, no máximo [max]). */
    fun pushRecent(current: List<String>, added: List<String>, max: Int = 8): List<String> =
        (added + current).distinct().take(max)

    /** Favoritos primeiro, mantendo a ordem do catálogo dentro de cada grupo. */
    fun favoritesFirst(list: List<CatalogExercise>, favorites: Set<String>): List<CatalogExercise> =
        list.sortedBy { it.name !in favorites }

    /** Só os recentes, na ordem de uso (mais novo primeiro). */
    fun recentOnly(list: List<CatalogExercise>, recent: List<String>): List<CatalogExercise> =
        recent.mapNotNull { n -> list.firstOrNull { it.name == n } }

    /** Já existe um exercício com esse nome (ignorando caixa e acento)? */
    fun isSameName(a: String, b: String) = a.normalized() == b.normalized()

    private fun String.normalized(): String = lowercase().trim()
        .replace('á', 'a').replace('à', 'a').replace('â', 'a').replace('ã', 'a')
        .replace('é', 'e').replace('ê', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ô', 'o').replace('õ', 'o')
        .replace('ú', 'u').replace('ç', 'c')
}
