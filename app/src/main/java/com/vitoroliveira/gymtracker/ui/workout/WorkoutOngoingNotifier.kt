package com.vitoroliveira.gymtracker.ui.workout

import android.Manifest
import android.annotation.SuppressLint
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
import com.vitoroliveira.gymtracker.ui.components.prettyGroupName

/**
 * Notificação viva do treino (#357): enquanto há um treino aberto, um aviso fixo com **cronômetro** do tempo
 * de treino aparece na barra e na tela de bloqueio; tocar nele volta ao app. Silenciosa (importância baixa), sem
 * vibrar nem apitar. Some quando o treino termina, é descartado ou encerrado automaticamente por esquecimento.
 * Sem a permissão de notificações (Android 13+) não faz nada.
 */
class WorkoutOngoingNotifier(context: Context) {
    private val app = context.applicationContext
    private val manager = NotificationManagerCompat.from(app)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Treino em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Mostra o cronômetro do treino aberto e leva de volta ao app"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun canNotify() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** [startedAtIso] = início do treino (instante ISO); [groupName] aparece como texto. */
    @SuppressLint("MissingPermission") // conferido em canNotify()
    fun show(startedAtIso: String?, groupName: String?) {
        if (!canNotify()) return
        val startMs = runCatching { java.time.OffsetDateTime.parse(startedAtIso).toInstant().toEpochMilli() }.getOrNull()
            ?: System.currentTimeMillis()
        val open = app.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }?.let { PendingIntent.getActivity(app, 1, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(com.vitoroliveira.gymtracker.R.drawable.ic_launcher_monochrome)
            .setContentTitle("Treino em andamento")
            .setContentText(groupName?.takeIf { it.isNotBlank() }?.let { prettyGroupName(it) } ?: "Treino livre")
            .setWhen(startMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .build()
        manager.notify(ID, n)
    }

    fun cancel() = manager.cancel(ID)

    private companion object {
        const val CHANNEL = "workout_ongoing"
        const val ID = 4107
    }
}
