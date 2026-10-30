package com.vitoroliveira.gymtracker.ui.social

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vitoroliveira.gymtracker.MainActivity
import com.vitoroliveira.gymtracker.R
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.SocialRepository
import com.vitoroliveira.gymtracker.presentation.social.socialAlerts
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Confere o Social a cada ~6 h (com internet) e avisa por notificação: pedido para seguir e desafio que terminou.
 * Sem servidor de push: o próprio app olha. Cada aviso aparece uma vez só (chaves guardadas no aparelho).
 */
class SocialAlertsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val auth: AuthRepository by inject()
    private val social: SocialRepository by inject()
    private val workouts: com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository by inject()
    private val profiles: com.vitoroliveira.gymtracker.domain.repository.ProfileRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            if (!auth.awaitSession()) return Result.success()
            val me = social.currentUserId()
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val seen = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty()
            val alerts = socialAlerts(
                me = me,
                follows = social.follows(),
                people = social.people(),
                challenges = runCatching { social.challenges() }.getOrDefault(emptyList()),
                participations = runCatching { social.myParticipations() }.getOrDefault(emptyList()),
                seen = seen,
                now = Clock.System.now()
            )
            // fechamento da semana (domingo à noite ou segunda), mesmo sem nada do Social
            val week = runCatching { weekAlert(seen) }.getOrNull()
            val all = alerts.take(3) + listOfNotNull(week)
            if (all.isNotEmpty()) {
                // só marca como visto o que foi mesmo mostrado: sem a permissão de notificação, o aviso fica para depois
                val shown = all.filterIndexed { i, a -> notify(a.title, a.text, i) }
                if (shown.isNotEmpty()) {
                    // guarda as chaves avisadas (as 200 mais recentes bastam)
                    prefs.edit().putStringSet(KEY_SEEN, (seen + shown.map { it.key }).toList().takeLast(200).toSet()).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }

    private suspend fun weekAlert(seen: Set<String>): com.vitoroliveira.gymtracker.presentation.social.SocialAlert? {
        val zone = kotlinx.datetime.TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        val sessions = workouts.getHistory().mapNotNull { w ->
            val start = w.startedAt?.let { runCatching { kotlin.time.Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
            val end = w.finishedAt?.let { runCatching { kotlin.time.Instant.parse(it) }.getOrNull() }
            start.toLocalDateTime(zone).date to end?.let { (it - start).inWholeMinutes }
        }
        val goal = runCatching { profiles.getCurrentProfile().weeklyGoal }.getOrDefault(0)
        return com.vitoroliveira.gymtracker.presentation.social.weekRecapAlert(sessions, goal, now, seen)
    }

    /** True se a notificação foi enviada ao sistema (false sem a permissão de notificação). */
    private fun notify(title: String, text: String, index: Int): Boolean {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        ctx.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(NotificationChannel(CHANNEL, "Social (seguir e desafios)", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            ctx, 70, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_BASE + index, n)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val CHANNEL = "social"
        private const val NOTIF_BASE = 300
        private const val PREFS = "social_alerts"
        private const val KEY_SEEN = "seen"
        private const val WORK = "avisos-do-social"

        /** Agenda a conferência periódica (chamado ao abrir o app; não duplica). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SocialAlertsWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Ao sair da conta: esquece o que já foi avisado (a próxima conta começa do zero). */
        fun clearSeen(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
