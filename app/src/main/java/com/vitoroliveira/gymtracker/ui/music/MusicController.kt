package com.vitoroliveira.gymtracker.ui.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Serviço vazio: existir e ter permissão de "acesso a notificações" é o que
 * autoriza o app a enxergar as sessões de mídia (Spotify, YouTube Music...).
 * Nenhuma notificação é lida ou guardada.
 */
class MediaListenerService : NotificationListenerService()

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

/** Observa a mídia tocando no celular e expõe controles básicos. */
class MusicController(context: Context) {
    private val ctx = context.applicationContext
    private val manager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listener = ComponentName(ctx, MediaListenerService::class.java)

    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var started = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    fun hasAccess(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == ctx.packageName }
    }

    /** Chame ao abrir o app e ao voltar das configurações. */
    fun refresh() {
        val access = hasAccess()
        if (!access) {
            detach()
            _state.value = MusicState(hasAccess = false)
            return
        }
        try {
            if (!started) {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listener)
                started = true
            }
            pick(manager.getActiveSessions(listener))
        } catch (_: SecurityException) {
            _state.value = MusicState(hasAccess = false)
        }
    }

    private fun pick(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        publish()
    }

    private var lastArt: Bitmap? = null
    private var lastAccent: Color? = null

    private fun publish() {
        val c = controller
        val md = c?.metadata
        if (c == null || md == null) {
            _state.value = MusicState(hasAccess = true, nowPlaying = null)
            return
        }
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (art !== lastArt) { lastArt = art; lastAccent = art?.let(::dominantColor) }
        val pm = ctx.packageManager
        val appName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString() }.getOrDefault("")
        _state.value = MusicState(
            hasAccess = true,
            nowPlaying = NowPlaying(
                title = md.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
                art = art,
                accent = lastAccent,
                isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                app = appName
            )
        )
    }

    private fun detach() {
        controller?.unregisterCallback(callback); controller = null
        if (started) { runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }; started = false }
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
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
