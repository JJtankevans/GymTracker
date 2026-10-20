package com.vitoroliveira.gymtracker.ui.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.ui.theme.ThemedCard
import org.koin.compose.koinInject

/** Mini-player: some sozinho quando não há nada tocando ou não há permissão. */
@Composable
fun MiniPlayer(modifier: Modifier = Modifier, controller: MusicController = koinInject()) {
    val state by controller.state.collectAsState()
    val np = state.nowPlaying ?: return
    MiniPlayerContent(np, modifier, controller::previous, controller::playPause, controller::next)
}

@Composable
fun MiniPlayerContent(
    np: NowPlaying,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit = {}, onPlayPause: () -> Unit = {}, onNext: () -> Unit = {}
) {
    val accent = np.accent ?: MaterialTheme.colorScheme.primary
    ThemedCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(MaterialTheme.shapes.small).background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (np.art != null) Image(np.art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.MusicNote, null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(np.title.ifBlank { "Sem título" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(np.artist.ifBlank { np.app }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (com.vitoroliveira.gymtracker.ui.theme.LocalIntensity.current != com.vitoroliveira.gymtracker.ui.theme.Intensity.CALM) {
                com.vitoroliveira.gymtracker.ui.components.EqualizerBars(np.isPlaying, accent)
            }
            IconButton(onPrev) { Icon(Icons.Default.SkipPrevious, "Anterior") }
            IconButton(onPlayPause) { Icon(if (np.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (np.isPlaying) "Pausar" else "Tocar", tint = accent) }
            IconButton(onNext) { Icon(Icons.Default.SkipNext, "Próxima") }
        }
    }
}
