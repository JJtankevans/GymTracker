package com.vitoroliveira.gymtracker.ui.music

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String? = null,
    val art: Bitmap?,
    val accent: Color?,
    val isPlaying: Boolean,
    val app: String
)

data class MusicState(
    val hasAccess: Boolean = false,
    val nowPlaying: NowPlaying? = null
)

/**
 * Música desligada (14/11/2026): ler a mídia que está tocando exigia o serviço de leitura de
 * notificações (BIND_NOTIFICATION_LISTENER_SERVICE), e o Play Protect bloqueia a instalação de
 * APK de fora da Play Store que declara essa permissão. O serviço saiu do manifesto; esta classe
 * ficou só como casca para o resto do app compilar: nunca há acesso nem música tocando, então o
 * mini-player não aparece, o tema não troca de cor e nenhuma faixa é gravada.
 * Para voltar: publicar pela Play Store e recuperar a versão anterior deste arquivo no Git.
 */
@Suppress("UNUSED_PARAMETER")
class MusicController(context: Context) {
    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    fun hasAccess(): Boolean = false
    fun refresh() { _state.value = MusicState(hasAccess = false) }
    fun playPause() {}
    fun next() {}
    fun previous() {}
}

/**
 * Cor de destaque da capa: média das cores saturadas e nem escuras nem claras
 * demais, numa miniatura 24×24. Sem dependências externas.
 */
internal fun dominantColor(bmp: Bitmap): Color? {
    val s = Bitmap.createScaledBitmap(bmp, 24, 24, true)
    var r = 0.0; var g = 0.0; var b = 0.0; var wSum = 0.0
    val hsv = FloatArray(3)
    for (y in 0 until 24) for (x in 0 until 24) {
        val px = s.getPixel(x, y)
        android.graphics.Color.colorToHSV(px, hsv)
        val w = (hsv[1] * hsv[2] * (1f - kotlin.math.abs(hsv[2] - 0.6f))).toDouble()
        if (w > 0.02) {
            r += android.graphics.Color.red(px) * w; g += android.graphics.Color.green(px) * w; b += android.graphics.Color.blue(px) * w; wSum += w
        }
    }
    if (wSum <= 0.0) return null
    val avg = android.graphics.Color.rgb((r / wSum).toInt(), (g / wSum).toInt(), (b / wSum).toInt())
    android.graphics.Color.colorToHSV(avg, hsv)
    hsv[1] = hsv[1].coerceAtLeast(0.55f)   // garante cor viva
    hsv[2] = hsv[2].coerceIn(0.7f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
