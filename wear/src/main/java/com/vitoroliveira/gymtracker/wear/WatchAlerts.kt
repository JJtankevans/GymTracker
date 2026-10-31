package com.vitoroliveira.gymtracker.wear

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

// ───────────────────────── vibração ─────────────────────────

enum class Buzz(val pattern: LongArray) {
    /** Série registrada. */
    TAP(longArrayOf(0, 50)),
    /** Fim do descanso: três batidas fortes. */
    REST_DONE(longArrayOf(0, 300, 150, 300, 150, 600)),
    /** Faltam 10 s. */
    WARN(longArrayOf(0, 80, 100, 80)),
    /** Recorde: batidas crescentes. */
    RECORD(longArrayOf(0, 60, 60, 60, 60, 120, 60, 400)),
    /** Intervalos: começou o esforço (uma batida longa). */
    GO(longArrayOf(0, 500)),
    /** Intervalos: começou a pausa (duas curtas). */
    PAUSE(longArrayOf(0, 120, 100, 120)),
    /** Intervalos: contagem 3, 2, 1. */
    TICK(longArrayOf(0, 40))
}

fun Context.buzz(kind: Buzz) {
    val v: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    v.vibrate(VibrationEffect.createWaveform(kind.pattern, -1))
}

// ───────────────────────── alarme do descanso ─────────────────────────

private const val CHANNEL_REST = "descanso"
private const val CHANNEL_WORKOUT = "treino"
private const val NOTIF_REST = 41
private const val NOTIF_WORKOUT = 42

private fun Context.openAppIntent(): PendingIntent =
    PendingIntent.getActivity(
        this, 0,
        Intent(this, WearActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

private fun Context.restIntent(): PendingIntent =
    PendingIntent.getBroadcast(
        this, 1, Intent(this, RestAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

private fun Context.ensureChannels() {
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(CHANNEL_REST, "Fim do descanso", NotificationManager.IMPORTANCE_HIGH))
    nm.createNotificationChannel(NotificationChannel(CHANNEL_WORKOUT, "Treino em andamento", NotificationManager.IMPORTANCE_LOW))
}

object RestAlarm {
    /**
     * Agenda a vibração do fim do descanso daqui a [inMs]. Alarme exato (vibra na hora mesmo com a tela apagada);
     * se o sistema não deixar, cai num alarme aproximado e a própria tela do app vibra quando estiver aberta.
     * Devolve true se o alarme é exato.
     */
    fun schedule(context: Context, inMs: Long): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        val at = SystemClock.elapsedRealtime() + inMs.coerceAtLeast(0)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, context.restIntent())
            else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, context.restIntent())
        } catch (_: SecurityException) {
            return false
        }
        return exact
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(context.restIntent())
        NotificationManagerCompat.from(context).cancel(NOTIF_REST)
    }
}

/** Fim do descanso com o app fechado ou a tela apagada: vibra e mostra um aviso que abre o app. */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = WatchStore(context)
        val rest = store.rest ?: return               // descanso pulado/cancelado nesse meio-tempo
        if (rest.endsAt - System.currentTimeMillis() > 2_000) return
        store.rest = null                             // a tela, se estiver aberta, vê isso e não vibra de novo
        context.buzz(Buzz.REST_DONE)
        context.ensureChannels()
        val n = NotificationCompat.Builder(context, CHANNEL_REST)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle("Descanso acabou")
            .setContentText("Bora para a próxima série")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(context.openAppIntent())
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_REST, n)
        } catch (_: SecurityException) {
            // sem permissão de notificação: a vibração já avisou
        }
    }
}

// ───────────────────────── atalho do treino no mostrador ─────────────────────────

object OngoingWorkout {
    /** Mostra o treino no mostrador (Ongoing Activity) com o cronômetro; tocar volta ao app. */
    fun show(context: Context, title: String, startedAtWallMs: Long) {
        context.ensureChannels()
        val open = context.openAppIntent()
        val builder = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle(title)
            .setContentText("Treino em andamento")
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
        // o cronômetro do Status usa a base do elapsedRealtime
        val zero = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAtWallMs).coerceAtLeast(0)
        val status = Status.Builder()
            .addTemplate("#nome# · #tempo#")
            .addPart("nome", Status.TextPart(title))
            .addPart("tempo", Status.StopwatchPart(zero))
            .build()
        OngoingActivity.Builder(context, NOTIF_WORKOUT, builder)
            .setStaticIcon(R.drawable.ic_stat_dumbbell)
            .setTouchIntent(open)
            .setStatus(status)
            .build()
            .apply(context)
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_WORKOUT, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_WORKOUT)
    }
}
