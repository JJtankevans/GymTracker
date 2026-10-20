package com.vitoroliveira.gymtracker.data.catalog

/**
 * Treino pronto: vira um grupo (ex.: "Push") já com os exercícios do catálogo.
 * [exerciseNames] usa os nomes exatos de [ExerciseCatalog] (um teste garante isso).
 */
data class WorkoutTemplate(
    val name: String,
    val description: String,
    val colorHex: String,
    val exerciseNames: List<String>
) {
    val exercises: List<CatalogExercise>
        get() = exerciseNames.mapNotNull { n -> ExerciseCatalog.all.firstOrNull { it.name == n } }
}

object WorkoutTemplates {
    val all: List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            "Push (empurrar)", "Peito, ombros e tríceps", "#F44336",
            listOf(
                "Supino Reto com Barra", "Supino Inclinado com Halteres", "Desenvolvimento com Halteres",
                "Elevação Lateral", "Tríceps Pulley", "Tríceps Testa"
            )
        ),
        WorkoutTemplate(
            "Pull (puxar)", "Costas e bíceps", "#2196F3",
            listOf(
                "Puxada Frontal na Polia", "Remada Curvada com Barra", "Remada Baixa na Polia",
                "Crucifixo Inverso", "Rosca Direta com Barra", "Rosca Martelo"
            )
        ),
        WorkoutTemplate(
            "Pernas", "Quadríceps, posterior, glúteos e panturrilha", "#4CAF50",
            listOf(
                "Agachamento Livre", "Leg Press 45°", "Cadeira Extensora", "Mesa Flexora",
                "Elevação Pélvica", "Panturrilha em Pé"
            )
        ),
        WorkoutTemplate(
            "Superior", "Parte de cima do corpo", "#9C27B0",
            listOf(
                "Supino Reto com Halteres", "Puxada Frontal na Polia", "Desenvolvimento com Halteres",
                "Remada Unilateral com Halter", "Rosca Alternada com Halteres", "Tríceps Corda"
            )
        ),
        WorkoutTemplate(
            "Inferior", "Parte de baixo do corpo e abdômen", "#FF9800",
            listOf(
                "Agachamento no Smith", "Stiff com Barra", "Agachamento Búlgaro",
                "Cadeira Abdutora", "Panturrilha Sentado", "Prancha"
            )
        ),
        WorkoutTemplate(
            "Full body", "Corpo todo, ideal pra 3x por semana", "#00BCD4",
            listOf(
                "Agachamento Livre", "Supino Reto com Barra", "Remada Curvada com Barra",
                "Desenvolvimento com Barra", "Levantamento Terra", "Abdominal Supra"
            )
        ),
        WorkoutTemplate(
            "Peito e tríceps", "Divisão ABC, dia A", "#E91E63",
            listOf(
                "Supino Reto com Barra", "Supino Inclinado com Barra", "Crucifixo com Halteres",
                "Crossover na Polia", "Tríceps Testa", "Tríceps Corda"
            )
        ),
        WorkoutTemplate(
            "Costas e bíceps", "Divisão ABC, dia B", "#3F51B5",
            listOf(
                "Barra Fixa", "Remada Curvada com Barra", "Pulldown na Polia",
                "Remada na Máquina", "Rosca Scott", "Rosca Martelo"
            )
        ),
        WorkoutTemplate(
            "Ombros e abdômen", "Divisão ABC, dia C", "#FF5722",
            listOf(
                "Desenvolvimento com Barra", "Elevação Lateral", "Elevação Frontal",
                "Encolhimento com Halteres", "Elevação de Pernas", "Abdominal na Polia"
            )
        )
    )
}
