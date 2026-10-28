package com.vitoroliveira.gymtracker.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExerciseCatalogTest {
    @Test fun temPeloMenos50() = assertTrue(ExerciseCatalog.all.size >= 50, "${ExerciseCatalog.all.size}")

    @Test fun chavesDeImagemValidasEUnicas() {
        val keys = ExerciseCatalog.all.map { it.imageKey }
        assertEquals(keys.size, keys.toSet().size, "chave repetida")
        assertTrue(keys.all { Regex("ex_[a-z0-9]+(_[a-z0-9]+)*").matches(it) }, keys.toString())
        assertEquals("ex_leg_press_45", CatalogExercise("Leg Press 45°", CatalogMuscle.QUADRICEPS, Equipment.MAQUINA, 3, 10).imageKey)
        assertEquals("elevacao_pelvica", slug("Elevação Pélvica"))
    }

    @Test fun nomesUnicos() {
        val names = ExerciseCatalog.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun todoGrupoTemExercicio() =
        CatalogMuscle.entries.forEach { m -> assertTrue(ExerciseCatalog.all.any { it.muscle == m }, m.name) }

    @Test fun buscaIgnoraAcentoECaixa() {
        assertTrue(ExerciseCatalog.search("gluteo").any { it.muscle == CatalogMuscle.GLUTEOS })
        assertTrue(ExerciseCatalog.search("AGACHAMENTO").size >= 3)
    }

    @Test fun filtraPorMusculo() =
        assertTrue(ExerciseCatalog.search("", CatalogMuscle.PEITO).all { it.muscle == CatalogMuscle.PEITO })

    @Test fun recentesNaoRepetemELimitam() {
        assertEquals(listOf("C", "A", "B"), ExerciseCatalog.pushRecent(listOf("A", "B"), listOf("C", "A")))
        assertEquals(3, ExerciseCatalog.pushRecent(listOf("A", "B", "C"), listOf("D"), max = 3).size)
        assertEquals("D", ExerciseCatalog.pushRecent(listOf("A", "B", "C"), listOf("D"), max = 3).first())
    }

    @Test fun favoritosVemPrimeiroMantendoAOrdem() {
        val list = ExerciseCatalog.search("supino")
        val fav = setOf(list[2].name, list[4].name)
        val out = ExerciseCatalog.favoritesFirst(list, fav)
        assertEquals(listOf(list[2].name, list[4].name), out.take(2).map { it.name })
        assertEquals(list.size, out.size)
    }

    @Test fun soRecentesNaOrdemDeUso() {
        val names = listOf(ExerciseCatalog.all[5].name, ExerciseCatalog.all[1].name, "Inexistente")
        assertEquals(names.take(2), ExerciseCatalog.recentOnly(ExerciseCatalog.all, names).map { it.name })
    }

    @Test fun mesmoNomeIgnoraAcento() {
        assertTrue(ExerciseCatalog.isSameName("Elevação Pélvica", "elevacao pelvica"))
        assertTrue(ExerciseCatalog.isSameName("Crossover Pegada Ñandú", "crossover pegada nandu"))   // ñ e ú
        assertTrue(ExerciseCatalog.isSameName("Glúteo Müller", "gluteo muller"))                   // ü (erro nº 22)
    }

    @Test fun apelidoAchaOExercicioDoCatalogoSemMisturarNomes() {
        assertEquals("Hack Squat", ExerciseCatalog.find("Agachamento Hack")?.name)
        assertEquals("Leg Press 45°", ExerciseCatalog.find("leg press 45")?.name)
        assertEquals("Rosca Scott", ExerciseCatalog.find(" Rosca Scot ")?.name)
        assertEquals("Elevação Frontal", ExerciseCatalog.find("Elevacao Frontral")?.name)
        assertEquals("Crucifixo Inverso", ExerciseCatalog.find("Crucifixo Invertido na Máquina")?.name)
        assertEquals("Hack Squat", ExerciseCatalog.find("hack squat")?.name) // nome certo continua valendo
        assertNull(ExerciseCatalog.find("aaaaa"))
        // o apelido não vira "mesmo nome": quem junta exercícios continua sendo o nome exato
        assertTrue(!ExerciseCatalog.isSameName("Agachamento Hack", "Hack Squat"))
    }

    @Test fun apelidosNaoColidemComNomesNemEntreSi() {
        val names = ExerciseCatalog.all.map { it.name.lowercase().trim() }.toSet()
        val aliases = ExerciseCatalog.all.flatMap { it.aliases }.map { it.lowercase().trim() }
        assertTrue(aliases.none { it in names }, "apelido igual a um nome do catálogo")
        assertEquals(aliases.size, aliases.toSet().size, "apelido repetido")
    }

    @Test fun cardioEMobilidadeContamEmMinutosOuSegundos() {
        val cardio = ExerciseCatalog.search("", CatalogMuscle.CARDIO)
        assertTrue(cardio.size >= 6)
        assertTrue(cardio.filter { it.equipment == Equipment.MAQUINA }.all { it.hint?.contains("minutos") == true })
        assertTrue(ExerciseCatalog.search("", CatalogMuscle.MOBILIDADE).all { it.hint != null })
        assertEquals("Passada", ExerciseCatalog.find("Avanço")?.name)
    }

    @Test fun exerciciosNovosDosUsuariosEstaoNoCatalogo() {
        listOf("Face Pull", "Remada Cavalinho", "Rosca Concentrada", "Cadeira Adutora", "Supino Guiado", "Tríceps na Máquina")
            .forEach { assertTrue(ExerciseCatalog.find(it) != null, it) }
    }
}
