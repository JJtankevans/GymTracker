package com.vitoroliveira.gymtracker.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMessage
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMessages
import com.vitoroliveira.gymtracker.presentation.social.ChallengeMood
import com.vitoroliveira.gymtracker.presentation.social.MessageLayout
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme

/** Mensagem do desafio ("Você foi moggado!" e companhia) em um dos três layouts padrão. */
@Composable
fun ChallengeMessageCard(message: ChallengeMessage, modifier: Modifier = Modifier) {
    when (message.layout) {
        MessageLayout.ALERT -> AlertLayout(message, modifier)
        MessageLayout.BANNER -> BannerLayout(message, modifier)
        MessageLayout.STAMP -> StampLayout(message, modifier)
    }
}

/** Cartão de alerta: ícone à esquerda e a frase em destaque. */
@Composable
private fun AlertLayout(message: ChallengeMessage, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                message.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** Faixa larga com a frase grande, para quem está bem (líder / meta batida). */
@Composable
private fun BannerLayout(message: ChallengeMessage, modifier: Modifier) {
    val icon = if (message.mood == ChallengeMood.GOAL_REACHED) Icons.Default.EmojiEvents else Icons.Default.LocalFireDepartment
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                message.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Carimbo inclinado com borda grossa, para quem falhou. */
@Composable
private fun StampLayout(message: ChallengeMessage, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.error
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.rotate(-3f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(3.dp, color)
        ) {
            Text(
                message.text.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChallengeMessageCardPreview() {
    GymTrackerTheme {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChallengeMood.entries.forEach { ChallengeMessageCard(ChallengeMessages.pick(it, 0)) }
        }
    }
}
