package com.vitoroliveira.gymtracker.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vitoroliveira.gymtracker.data.model.Challenge
import com.vitoroliveira.gymtracker.data.model.FeedPost
import com.vitoroliveira.gymtracker.data.model.SocialGroup
import com.vitoroliveira.gymtracker.data.model.SocialProfile
import com.vitoroliveira.gymtracker.data.model.SocialSettings
import com.vitoroliveira.gymtracker.presentation.records.RecordRow
import com.vitoroliveira.gymtracker.presentation.records.RecordsUiState
import com.vitoroliveira.gymtracker.presentation.social.ChallengeItem
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMetric
import com.vitoroliveira.gymtracker.presentation.social.ChallengeStatus
import com.vitoroliveira.gymtracker.presentation.social.FeedItem
import com.vitoroliveira.gymtracker.presentation.social.PersonItem
import com.vitoroliveira.gymtracker.presentation.social.ReactionKind
import com.vitoroliveira.gymtracker.presentation.social.ReactionSummary
import com.vitoroliveira.gymtracker.presentation.social.Relation
import com.vitoroliveira.gymtracker.presentation.social.SocialUiState
import com.vitoroliveira.gymtracker.ui.records.RecordsContent
import com.vitoroliveira.gymtracker.ui.social.SocialActions
import com.vitoroliveira.gymtracker.ui.social.SocialContent
import com.vitoroliveira.gymtracker.ui.theme.AppThemes
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class SocialGalleryTest {
    @get:Rule val rule = createComposeRule()

    private val challenge = Challenge(
        id = "c1", creatorId = "x", groupId = "g1", title = "Mês do supino", metric = "exercise_max_weight",
        exerciseName = "Supino Reto com Barra", startsAt = "2026-10-01T00:00:00Z", endsAt = "2026-11-01T00:00:00Z",
        target = 100.0, minMinutesPerWorkout = 20
    )
    private val item = ChallengeItem(challenge, joined = true, ChallengeStatus.ACTIVE, 12, "Turma da manhã", ChallengeMetric.EXERCISE_MAX)

    private val state = SocialUiState(
        me = "me",
        settings = SocialSettings(shareWorkouts = false),
        feed = listOf(
            FeedItem(
                FeedPost(
                    "p1", "a", "w1", "Dia de peito!", "public",
                    JsonObject(mapOf("minutes" to JsonPrimitive(52), "sets" to JsonPrimitive(18), "volume_kg" to JsonPrimitive(5240), "group" to JsonPrimitive("Push"))),
                    "2026-10-19T10:00:00Z"
                ),
                "Ana Souza", false, ReactionSummary(mapOf(ReactionKind.FIRE to 3, ReactionKind.FLEX to 1), setOf(ReactionKind.FIRE))
            )
        ),
        people = listOf(
            PersonItem(SocialProfile("a", "ana", "Ana Souza", bio = "Treino de manhã, cafe depois."), Relation.NONE, followsMe = true),
            PersonItem(SocialProfile("b", "beto"), Relation.FOLLOWING, false),
            PersonItem(SocialProfile("c", "cris"), Relation.REQUESTED, false)
        ),
        followRequests = listOf(SocialProfile("d", "duda", "Duda Lima")),
        myGroups = listOf(SocialGroup("g1", "me", "Turma da manhã", "Treino às 6h", "invite", "a1b2c3d4", 20)),
        publicGroups = listOf(SocialGroup("g2", "z", "Hipertrofia BR", privacy = "public")),
        challenges = listOf(item, item.copy(challenge = challenge.copy(id = "c2", title = "Mais treinos", metric = "workouts", exerciseName = null), joined = false, metric = ChallengeMetric.WORKOUTS))
    )

    @Test
    fun social() {
        rule.setContent { GymTrackerTheme(spec = AppThemes.byId("carbon")) { SocialContent(state, SocialActions(), {}) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/social-gallery/feed.png")
        listOf("Grupos", "Desafios", "Pessoas").forEach { tab ->
            rule.onNodeWithText(tab).performClick()
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("build/outputs/social-gallery/${tab.lowercase()}.png")
        }
    }


    @Test
    fun challengeSheetEmEquipes() {
        val team = item.copy(challenge = challenge.copy(mode = "team", teams = listOf("Azuis", "Vermelhos"), target = 150.0), joined = true)
        val standings = listOf(
            com.vitoroliveira.gymtracker.data.model.Standing("a", "ana", team = "Azuis", score = 100.0, rank = 1),
            com.vitoroliveira.gymtracker.data.model.Standing("me", "eu", team = "Azuis", score = 90.0, rank = 2),
            com.vitoroliveira.gymtracker.data.model.Standing("c", "cris", team = "Vermelhos", score = 60.0, rank = 3)
        )
        rule.setContent {
            GymTrackerTheme(spec = AppThemes.byId("carbon")) {
                com.vitoroliveira.gymtracker.ui.social.ChallengeSheet(team, standings, "me", {}, {}, {}, {})
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/social-gallery/challenge_sheet.png")
    }
    @Test
    fun records() {
        val rows = listOf(
            RecordRow("1", "Supino Reto com Barra", 100.0, "2026-10-18", 128.0, 95.0, 12),
            RecordRow("2", "Agachamento Livre", 140.0, "2026-09-02", 170.0, null, 8),
            RecordRow("3", "Exercício que não está no catálogo", 22.5, "2026-08-10", 30.0, 20.0, 3)
        )
        rule.setContent {
            GymTrackerTheme(spec = AppThemes.byId("carbon")) {
                RecordsContent(RecordsUiState(records = rows, today = LocalDate(2026, 10, 20)), {})
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/social-gallery/records.png")
    }
}
