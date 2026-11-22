package com.vitoroliveira.gymtracker.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.vitoroliveira.gymtracker.R

/**
 * Widget da tela inicial (#357): mostra o **próximo treino** sugerido e, com um treino aberto, o **cronômetro**.
 * Tocar abre o app. Os dados vêm de [WorkoutWidgetState], que a Home atualiza (o widget não consulta o servidor).
 * RemoteViews puro (sem Glance): sem dependência nova, atualiza só quando o app escreve o estado.
 */
class WorkoutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    companion object {
        fun build(context: Context): RemoteViews {
            val s = WorkoutWidgetState.read(context)
            val v = RemoteViews(context.packageName, R.layout.widget_workout)
            val active = s.activeSinceMs > 0
            v.setTextViewText(R.id.widget_label, if (active) "TREINO EM ANDAMENTO" else "PRÓXIMO TREINO")
            v.setTextViewText(R.id.widget_title, (if (active) s.activeName else s.nextName).ifBlank { if (active) "Treino livre" else "Escolher treino" })
            if (active) {
                v.setViewVisibility(R.id.widget_timer, View.VISIBLE)
                v.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime() - (System.currentTimeMillis() - s.activeSinceMs), null, true)
            } else {
                v.setViewVisibility(R.id.widget_timer, View.GONE)
                v.setChronometer(R.id.widget_timer, SystemClock.elapsedRealtime(), null, false)
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                val pi = PendingIntent.getActivity(context, 2, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                v.setOnClickPendingIntent(R.id.widget_root, pi)
            }
            return v
        }

        fun refresh(context: Context) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, WorkoutWidgetProvider::class.java))
            if (ids.isNotEmpty()) ids.forEach { mgr.updateAppWidget(it, build(app)) }
        }
    }
}

/** Estado que o widget mostra: o app grava, o widget lê. */
object WorkoutWidgetState {
    private const val PREFS = "gymtracker_widget"
    class Data(val nextName: String, val activeSinceMs: Long, val activeName: String)

    fun read(context: Context): Data {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Data(p.getString("next", "") ?: "", p.getLong("since", 0L), p.getString("active", "") ?: "")
    }

    /** [activeStartedAtIso] null = sem treino aberto. */
    fun save(context: Context, nextName: String?, activeStartedAtIso: String?, activeName: String?) {
        val since = activeStartedAtIso?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() ?: System.currentTimeMillis() } ?: 0L
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val before = Triple(p.getString("next", ""), p.getLong("since", 0L), p.getString("active", ""))
        val after = Triple(nextName ?: "", since, activeName ?: "")
        if (before == after) return
        p.edit().putString("next", after.first).putLong("since", after.second).putString("active", after.third).apply()
        WorkoutWidgetProvider.refresh(context)
    }
}
