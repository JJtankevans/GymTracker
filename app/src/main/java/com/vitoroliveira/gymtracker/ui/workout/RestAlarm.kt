package com.vitoroliveira.gymtracker.ui.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vitoroliveira.gymtracker.presentation.RestAlertClaim
import com.vitoroliveira.gymtracker.ui.utils.restFinishedFeedback

/**
 * Alarme do sistema para o fim do descanso. Com a tela bloqueada o celular dorme e a contagem do app pode atrasar ou nem
 * rodar: o alarme (AlarmManager, que acorda o aparelho) garante a vibração e a notificação "Descanso acabou" na hora.
 *
 * Quem avisa: se o treino está na tela, o próprio app (o alarme fica quieto); senão, quem chegar primeiro entre o app
 * (ainda vivo em segundo plano) e o alarme, e o outro não repete ([claim]).
 */
object RestAlarm {
    private const val REQUEST_CODE = 4202
    internal const val ACTION = "com.vitoroliveira.gymtracker.REST_ALARM"
    internal const val EXTRA_ENDS_AT = "ends_at"

    /** O treino está na tela (RESUMED). Atualizado pela tela do treino. */
    @Volatile var screenVisible = false

    /** Quando o descanso agendado acaba (ms desde 1970); 0 se não há alarme. */
    @Volatile var scheduledEndsAt = 0L
        private set

    private val claims = RestAlertClaim()

    /** true se este fim de descanso ainda não foi avisado por ninguém. */
    fun claim(endsAt: Long): Boolean = synchronized(claims) { claims.claim(endsAt) }

    private fun pending(context: Context, endsAt: Long): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, RestAlarmReceiver::class.java).setAction(ACTION).putExtra(EXTRA_ENDS_AT, endsAt),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Agenda (ou reagenda) o alarme para daqui a [seconds]. Exato quando o sistema permite; senão o melhor possível. */
    fun schedule(context: Context, seconds: Int) {
        if (seconds <= 0) return cancel(context)
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val endsAt = System.currentTimeMillis() + seconds * 1000L
        scheduledEndsAt = endsAt
        val pi = pending(app, endsAt)
        try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pi) // permissão de alarme exato revogada no meio do caminho
        }
    }

    /** Cancela o alarme (descanso pulado, pausado, acabou na tela ou o treino fechou). */
    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSystemService(AlarmManager::class.java)?.cancel(pending(app, scheduledEndsAt))
        scheduledEndsAt = 0L
    }
}

/** Recebe o alarme: vibra (e apita) e mostra a notificação, a menos que o treino esteja na tela ou o app já tenha avisado. */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RestAlarm.ACTION) return
        if (RestAlarm.screenVisible) return // o app na tela avisa sozinho
        val endsAt = intent.getLongExtra(RestAlarm.EXTRA_ENDS_AT, 0L)
        if (!RestAlarm.claim(endsAt)) return // o app, ainda vivo, já avisou
        context.restFinishedFeedback()
        RestNotifier(context).showFinished()
    }
}
