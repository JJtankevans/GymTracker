package com.vitoroliveira.gymtracker.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/**
 * Complicação "Treinos na semana" para o mostrador: anel com treinos feitos x meta (ou "2/4" em texto curto).
 * Os números vêm do celular junto com as rotinas ([WatchStore.routines]); tocar abre o app.
 */
class WeekComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, done = 2, goal = 4)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val r = WatchStore(this).routines
        return build(request.complicationType, r?.weekDone ?: 0, r?.weekGoal ?: 0)
    }

    private fun build(type: ComplicationType, done: Int, goal: Int): ComplicationData? {
        val label = if (goal > 0) "$done/$goal" else "$done"
        val description = PlainComplicationText.Builder("$label treinos na semana").build()
        val text = PlainComplicationText.Builder(label).build()
        val title = PlainComplicationText.Builder("treinos").build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_stat_dumbbell)).build()
        val tap = PendingIntent.getActivity(
            this, 2, Intent(this, WearActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = done.toFloat().coerceAtMost(goal.coerceAtLeast(1).toFloat()),
                min = 0f,
                max = goal.coerceAtLeast(1).toFloat(),
                contentDescription = description
            ).setText(text).setMonochromaticImage(icon).setTapAction(tap).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, description)
                .setTitle(title).setMonochromaticImage(icon).setTapAction(tap).build()
            else -> null
        }
    }

    companion object {
        /** Pede ao mostrador para buscar os números de novo (rotinas novas do celular). */
        fun refresh(context: Context) {
            runCatching {
                ComplicationDataSourceUpdateRequester
                    .create(context, ComponentName(context, WeekComplicationService::class.java))
                    .requestUpdateAll()
            }
        }
    }
}
