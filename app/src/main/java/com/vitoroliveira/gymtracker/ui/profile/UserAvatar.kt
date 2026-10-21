package com.vitoroliveira.gymtracker.ui.profile

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Foto de perfil redonda. Sem [url] (ou enquanto baixa / se falhar) mostra a inicial de [fallbackName] ou um ícone de pessoa.
 * A URL muda a cada troca de foto (ver ProfileRepositoryImpl), então o cache em memória por URL nunca mostra foto velha.
 */
@Composable
fun UserAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackName: String? = null
) {
    val image by produceState<ImageBitmap?>(initialValue = url?.let { AvatarCache.get(it) }, url) {
        value = if (url.isNullOrBlank()) null else AvatarCache.get(url) ?: withContext(Dispatchers.IO) { download(url) }
            ?.also { AvatarCache.put(url, it) }
    }

    Surface(modifier.size(size), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center) {
                val initial = fallbackName?.trim()?.firstOrNull()?.uppercaseChar()
                if (initial != null) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = fontFor(size.value)
                    )
                } else {
                    Icon(Icons.Default.Person, null, Modifier.size(size * 0.6f), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

private fun fontFor(sizeDp: Float): TextUnit = (sizeDp * 0.42f).sp

private object AvatarCache {
    private val cache = LruCache<String, ImageBitmap>(24)
    fun get(url: String): ImageBitmap? = cache.get(url)
    fun put(url: String, image: ImageBitmap) { cache.put(url, image) }
}

/** Baixa a foto pública do Storage (JPEG de ~512 px, algumas dezenas de KB). Null se falhar: a tela fica com a inicial. */
private fun download(url: String): ImageBitmap? = try {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
    }
    try {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        } else null
    } finally {
        conn.disconnect()
    }
} catch (e: Exception) {
    null
}
