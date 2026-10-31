package com.vitoroliveira.gymtracker.wear

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Tile "GymTracker" no carrossel do relógio (deslize a partir do mostrador): semana (treinos x meta),
 * sequência, o grupo sugerido para hoje e um botão que já abre o treino dele. Com treino aberto, mostra o
 * treino e "Continuar". Lê o que o app guarda no relógio ([WatchStore]); o app pede atualização quando muda.
 */
class WorkoutTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val store = WatchStore(this)
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES)
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(store)))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build())

    private fun layout(store: WatchStore): LayoutElementBuilders.LayoutElement {
        val accent = ACCENT_ARGB.getOrElse(store.accent) { ACCENT_ARGB[0] }
        val session = store.session
        val routines = store.routines
        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        if (session != null) {
            column.addContent(text("TREINO ABERTO", 10f, MUTED))
                .addContent(space(2f))
                .addContent(text(session.title, 16f, WHITE, bold = true))
                .addContent(space(2f))
                .addContent(text("${session.totalSets} séries · ${session.doneExercises}/${session.exercises.size}", 12f, MUTED))
                .addContent(space(10f))
                .addContent(button("Continuar", accent, null))
        } else {
            val week = routines?.takeIf { it.weekGoal > 0 }
            column.addContent(text("GYMTRACKER", 10f, accent, bold = true))
            if (week != null) {
                column.addContent(space(2f))
                    .addContent(text("${week.weekDone} de ${week.weekGoal}", 22f, WHITE, bold = true))
                    .addContent(text("treinos na semana", 10f, MUTED))
            }
            val streak = routines?.streakDays ?: 0
            if (streak > 1) column.addContent(text("$streak dias seguidos", 10f, GOLD))
            val suggested = routines?.let { r ->
                r.routines.firstOrNull { it.groupId != null && it.groupId == r.suggestedGroupId }
                    ?: r.routines.firstOrNull { it.groupId != null }
            }
            column.addContent(space(6f))
            if (suggested != null) {
                column.addContent(text("Hoje: ${suggested.name}", 13f, WHITE))
                    .addContent(space(8f))
                    .addContent(button("Treinar", accent, suggested.groupId))
            } else {
                column.addContent(text("Abra o app no celular", 12f, MUTED))
                    .addContent(space(8f))
                    .addContent(button("Abrir", accent, null))
            }
        }

        val root = LayoutElementBuilders.Box.Builder()
            .setWidth(expand()).setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        // arco da semana na borda (só sem treino aberto e com meta)
        routines?.takeIf { session == null && it.weekGoal > 0 }?.let { r ->
            val frac = (r.weekDone.toFloat() / r.weekGoal).coerceIn(0f, 1f)
            root.addContent(
                LayoutElementBuilders.Arc.Builder()
                    .setAnchorAngle(degrees(0f))
                    .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                    .addContent(
                        LayoutElementBuilders.ArcLine.Builder()
                            .setLength(degrees(360f * frac.coerceAtLeast(0.01f)))
                            .setThickness(dp(5f))
                            .setColor(argb(accent))
                            .build()
                    )
                    .build()
            )
        }
        root.addContent(column.build())
        return root.build()
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(2)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build()
            )
            .build()

    private fun space(heightDp: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(heightDp)).build()

    /** Botão que abre o app; com [groupId], o app já inicia o treino desse grupo. */
    private fun button(label: String, color: Int, groupId: String?): LayoutElementBuilders.LayoutElement {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(WearActivity::class.java.name)
        if (groupId != null) activity.addKeyToExtraMapping(WearActivity.EXTRA_START_GROUP, ActionBuilders.stringExtra(groupId))
        val click = ModifiersBuilders.Clickable.Builder()
            .setId("abrir_${groupId ?: "app"}")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()
        val modifiers = ModifiersBuilders.Modifiers.Builder()
            .setClickable(click)
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(argb(color))
                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(20f)).build())
                    .build()
            )
            .setPadding(
                ModifiersBuilders.Padding.Builder()
                    .setStart(dp(22f)).setEnd(dp(22f)).setTop(dp(9f)).setBottom(dp(9f))
                    .build()
            )
            .build()
        return LayoutElementBuilders.Box.Builder()
            .setModifiers(modifiers)
            .addContent(text(label.uppercase(), 13f, BLACK, bold = true))
            .build()
    }

    companion object {
        private const val RESOURCES = "1"
        private val WHITE = 0xFFFFFFFF.toInt()
        private val BLACK = 0xFF000000.toInt()
        private val MUTED = 0xFF9AA09A.toInt()
        private val GOLD = 0xFFFFC53D.toInt()
        /** Mesmas cores de [Accents] (o tile não usa Compose). */
        private val ACCENT_ARGB = listOf(0xFFC6FF00, 0xFFFF2BD6, 0xFF22E3FF, 0xFFFF7A1A, 0xFFFFC53D, 0xFFE8F1FF).map { it.toInt() }

        /** Pede ao sistema para redesenhar o tile (treino aberto/fechado, rotinas novas, cor). */
        fun refresh(context: Context) {
            runCatching { getUpdater(context).requestUpdate(WorkoutTileService::class.java) }
            WeekComplicationService.refresh(context)
        }
    }
}
