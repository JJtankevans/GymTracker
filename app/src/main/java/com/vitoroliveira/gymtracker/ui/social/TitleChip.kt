package com.vitoroliveira.gymtracker.ui.social

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.ProfileTitle
import com.vitoroliveira.gymtracker.presentation.social.ProfileTitles
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme

/**
 * Título de desafio no perfil: honra (Alpha, Chad, Giga Chad, Mr. Olimpia) com troféu, ou falha (uma das frases) com alerta.
 * Mr. Olimpia, o máximo, ganha o destaque da cor primária cheia.
 */
@Composable
fun TitleChip(title: ProfileTitle, modifier: Modifier = Modifier) {
    val honor = ProfileTitles.isHonor(title)
    val max = ProfileTitles.isMax(title)
    val container = when {
        max -> MaterialTheme.colorScheme.primary
        honor -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when {
        max -> MaterialTheme.colorScheme.onPrimary
        honor -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(modifier, color = container, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (honor) Icons.Default.EmojiEvents else Icons.Default.Warning, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(ProfileTitles.text(title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = content, maxLines = 2)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleChipPreview() {
    GymTrackerTheme {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { TitleChip(ProfileTitle("p", "honor", level = it)) }
            TitleChip(ProfileTitle("p", "fail", failures = 3, challengeId = "c"))
        }
    }
}
