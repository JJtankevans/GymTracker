package com.vitoroliveira.gymtracker.ui.workout

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/** Cores (ARGB) que o cartão usa; vêm do tema ativo para a imagem combinar com o app. */
data class ShareColors(val bgTop: Int, val bgBottom: Int, val text: Int, val muted: Int, val accent: Int, val onAccent: Int)

data class ShareStats(
    val title: String,
    val dateText: String,
    val durationMin: Long?,
    val sets: Int,
    val reps: Int,
    val volume: String,
    val records: List<String>
)

/**
 * Cartão do treino em 1080×1350 (formato vertical de Stories/Instagram/WhatsApp), desenhado direto no Canvas:
 * sem tela escondida nem captura de Compose, então funciona igual em qualquer aparelho.
 * Visual: fundo do tema, título grande, três números em destaque e os recordes em pílulas.
 */
fun renderWorkoutImage(stats: ShareStats, c: ShareColors): Bitmap {
    val w = 1080
    val h = 1350
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val pad = 84f

    val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), c.bgTop, c.bgBottom, Shader.TileMode.CLAMP) }
    cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

    fun paint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        letterSpacing = spacing
    }

    // marca
    cv.drawText("GYMTRACKER", pad, 120f, paint(34f, c.accent, bold = true, spacing = 0.22f))
    cv.drawText(stats.dateText, w - pad - paint(34f, c.muted).measureText(stats.dateText), 120f, paint(34f, c.muted))

    // título (até 2 linhas)
    val titlePaint = paint(112f, c.text, bold = true)
    val lines = wrap(stats.title, titlePaint, w - 2 * pad).take(2)
    var y = 300f
    lines.forEach { cv.drawText(it, pad, y, titlePaint); y += 126f }
    cv.drawText("Treino concluído", pad, y + 6f, paint(44f, c.muted))

    // três números
    val labelPaint = paint(30f, c.muted, spacing = 0.18f)
    val numPaint = paint(132f, c.accent, bold = true)
    val blocks = listOf(
        "DURAÇÃO" to (stats.durationMin?.let { "$it" } ?: "—") + (if (stats.durationMin != null) " min" else ""),
        "SÉRIES" to "${stats.sets}",
        "VOLUME" to stats.volume
    )
    var by = 540f
    blocks.forEach { (label, value) ->
        cv.drawText(label, pad, by, labelPaint)
        cv.drawText(value, pad, by + 128f, numPaint)
        by += 200f
    }

    // recordes
    if (stats.records.isNotEmpty()) {
        val chip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.accent }
        val chipText = paint(38f, c.onAccent, bold = true)
        val text = (if (stats.records.size == 1) "Novo recorde: " else "Novos recordes: ") + stats.records.joinToString(", ")
        val shown = wrap(text, chipText, w - 2 * pad - 72f).first().let { if (it == text) it else "$it…" }
        val tw = chipText.measureText(shown)
        val rect = RectF(pad, h - 230f, pad + tw + 72f, h - 140f)
        cv.drawRoundRect(rect, 45f, 45f, chip)
        cv.drawText(shown, pad + 36f, h - 170f, chipText)
    }
    cv.drawText("${stats.reps} repetições · feito no GymTracker", pad, h - 70f, paint(30f, c.muted))
    return bmp
}

private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
    val out = mutableListOf<String>()
    var line = ""
    for (word in text.split(" ")) {
        val next = if (line.isEmpty()) word else "$line $word"
        if (paint.measureText(next) <= maxWidth || line.isEmpty()) line = next else { out += line; line = word }
    }
    if (line.isNotEmpty()) out += line
    return out
}

/** Salva o PNG no cache do app e abre o compartilhar (WhatsApp, Instagram, Drive...). */
fun Context.shareWorkoutImage(bitmap: Bitmap, fileName: String = "gymtracker_treino.png") {
    val dir = File(cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, "Compartilhar treino").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
