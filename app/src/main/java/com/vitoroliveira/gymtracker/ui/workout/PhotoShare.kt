package com.vitoroliveira.gymtracker.ui.workout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max

private const val W = 1080
private const val H = 1350

/** Lê a foto do [uri] reduzida (lado maior ≤ 2200 px) e já girada pela orientação gravada no arquivo (EXIF). */
fun decodePhoto(context: Context, uri: Uri, maxSide: Int = 2200): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@runCatching null
    val orientation = resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
    }
    if (m.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}.getOrNull()

/**
 * Foto do treino com os números por cima, no estilo dos apps de corrida: a foto ocupa o cartão 1080×1350 (corte central),
 * um degradê escuro embaixo (e leve no topo) garante a leitura em qualquer foto, e por cima ficam título, data,
 * três números grandes e o selo de recorde (na cor de destaque do tema). Texto sempre branco: não depende do tema.
 */
fun renderPhotoOverlay(photo: Bitmap, stats: ShareStats, c: ShareColors): Bitmap {
    val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val cv = Canvas(out)
    // foto em "cover": preenche o cartão cortando o que sobra
    val scale = max(W / photo.width.toFloat(), H / photo.height.toFloat())
    val dw = photo.width * scale
    val dh = photo.height * scale
    cv.drawBitmap(photo, null, RectF((W - dw) / 2, (H - dh) / 2, (W + dw) / 2, (H + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))

    cv.drawRect(0f, 0f, W.toFloat(), 360f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, 360f, 0xAA000000.toInt(), 0x00000000, Shader.TileMode.CLAMP) })
    cv.drawRect(0f, 560f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 560f, 0f, H.toFloat(), 0x00000000, 0xEE000000.toInt(), Shader.TileMode.CLAMP) })

    val white = 0xFFFFFFFF.toInt()
    val soft = 0xCCFFFFFF.toInt()
    fun paint(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; letterSpacing = spacing
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        setShadowLayer(8f, 0f, 3f, 0x99000000.toInt())
    }
    val pad = 72f

    // topo: marca com barrinha na cor de destaque, e a data à direita
    cv.drawRect(pad, 76f, pad + 64f, 84f, Paint().apply { color = c.accent })
    cv.drawText("GYMTRACKER", pad, 132f, paint(34f, white, bold = true, spacing = 0.22f))
    val dp = paint(34f, soft)
    cv.drawText(stats.dateText, W - pad - dp.measureText(stats.dateText), 132f, dp)

    // base: título, três números lado a lado e o recorde
    val titlePaint = paint(104f, white, bold = true)
    var title = stats.title
    while (title.length > 1 && titlePaint.measureText(title) > W - 2 * pad) title = title.dropLast(1)
    if (title != stats.title) title = title.trimEnd() + "…"
    cv.drawText(title, pad, 1010f, titlePaint)

    val blocks = stats.blocks.take(3)
    val colW = (W - 2 * pad) / max(blocks.size, 1)
    blocks.forEachIndexed { i, (label, value) ->
        val x = pad + i * colW
        var vp = paint(96f, white, bold = true)
        while (vp.measureText(value) > colW - 44f && vp.textSize > 40f) vp = paint(vp.textSize - 6f, white, bold = true)
        cv.drawText(value, x, 1130f, vp)
        cv.drawText(label, x, 1180f, paint(28f, soft, spacing = 0.16f))
    }

    if (stats.records.isNotEmpty()) {
        val chipText = paint(36f, c.onAccent, bold = true).apply { clearShadowLayer() }
        val text = (if (stats.records.size == 1) "Novo recorde: " else "Novos recordes: ") + stats.records.joinToString(", ")
        var shown = text
        while (shown.length > 1 && chipText.measureText(shown) > W - 2 * pad - 64f) shown = shown.dropLast(1)
        if (shown != text) shown = shown.trimEnd() + "…"
        val tw = chipText.measureText(shown)
        cv.drawRoundRect(RectF(pad, 1214f, pad + tw + 64f, 1300f), 43f, 43f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.accent })
        cv.drawText(shown, pad + 32f, 1271f, chipText)
    } else {
        cv.drawText(stats.footer, pad, 1290f, paint(30f, soft))
    }
    return out
}

/**
 * Botão "Foto com estatísticas": tirar uma foto (câmera do sistema, sem permissão extra) ou escolher da galeria;
 * mostra a prévia com o overlay e só então compartilha.
 */
@Composable
fun PhotoShareButton(stats: ShareStats, colors: ShareColors, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var pick by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // rememberSaveable: com a câmera aberta o sistema pode recriar a Activity; sem isso o endereço da foto se perderia
    var cameraUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Uri?>(null) }

    fun handle(uri: Uri?) {
        if (uri == null) return
        val photo = decodePhoto(ctx, uri)
        if (photo == null) error = "Não consegui abrir essa foto." else preview = renderPhotoOverlay(photo, stats, colors)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) handle(cameraUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> handle(uri) }

    OutlinedButton(onClick = { pick = true }, modifier = modifier.fillMaxWidth()) { Text("Foto com estatísticas") }

    if (pick) AlertDialog(
        onDismissRequest = { pick = false },
        title = { Text("Foto do treino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    pick = false
                    val file = File(File(ctx.cacheDir, "exports").apply { mkdirs() }, "foto_treino.jpg")
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                    cameraUri = uri
                    runCatching { camera.launch(uri) }.onFailure { error = "Não consegui abrir a câmera." }
                }) { Text("Tirar foto agora") }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    pick = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Escolher da galeria") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ pick = false }) { Text("Cancelar") } }
    )

    preview?.let { bmp ->
        AlertDialog(
            onDismissRequest = { preview = null },
            text = { Image(bmp.asImageBitmap(), contentDescription = "Prévia da foto com as estatísticas", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton({ runCatching { ctx.shareWorkoutImage(bmp, "gymtracker_foto.png") }; preview = null }) { Text("Compartilhar") }
            },
            dismissButton = { TextButton({ preview = null }) { Text("Descartar") } }
        )
    }
    error?.let { msg -> AlertDialog(onDismissRequest = { error = null }, text = { Text(msg) }, confirmButton = { TextButton({ error = null }) { Text("Ok") } }) }
}
