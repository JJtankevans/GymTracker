package com.vitoroliveira.gymtracker.ui.workout

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

/**
 * Notificações do descanso entre séries, para quem bloqueia a tela ou sai do app:
 *  - durante o descanso: notificação fixa e silenciosa com contagem regressiva (o próprio Android anima o relógio);
 *  - no fim, com o app fora da tela: alerta com som/vibração "Descanso acabou".
 * Sem a permissão de notificações (Android 13+) não faz nada: o bipe/vibração dentro do app continuam.
 */
class RestNotifier(context: Context) {
    private val app = context.applicationContext
    private val manager = NotificationManagerCompat.from(app)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = app.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CH_COUNTDOWN, "Descanso em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Contagem regressiva do descanso entre séries"
                    setShowBadge(false)
                }
            )
            nm?.createNotificationChannel(
                NotificationChannel(CH_DONE, "Fim do descanso", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisa quando o descanso entre séries termina"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 220, 120, 220, 120, 380)
                }
            )
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(): PendingIntent? {
        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        } ?: return null
        return PendingIntent.getActivity(app, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Descanso começou (ou foi retomado) com [remainingSeconds] pela frente. */
    fun showCountdown(remainingSeconds: Int, exerciseHint: String? = null) {
        if (!canNotify() || remainingSeconds <= 0) return
        val endsAt = System.currentTimeMillis() + remainingSeconds * 1000L
        val n = NotificationCompat.Builder(app, CH_COUNTDOWN)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Descansando")
            .setContentText(exerciseHint ?: "A próxima série começa quando o relógio zerar")
            .setWhen(endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setTimeoutAfter(remainingSeconds * 1000L + 1500L)
            .setContentIntent(openApp())
            .build()
        post(ID, n)
    }

    /** Descanso acabou e o app não está na tela: alerta com som/vibração. */
    fun showFinished() {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(app, CH_DONE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Descanso acabou")
            .setContentText("Bora pra próxima série!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setContentIntent(openApp())
            .build()
        post(ID, n)
    }

    fun cancel() = manager.cancel(ID)

    @Suppress("MissingPermission") // conferido em canNotify()
    private fun post(id: Int, n: android.app.Notification) {
        try { manager.notify(id, n) } catch (_: SecurityException) { /* permissão revogada no meio do caminho */ }
    }

    private companion object {
        const val CH_COUNTDOWN = "rest_countdown"
        const val CH_DONE = "rest_done"
        const val ID = 4201
    }
}
