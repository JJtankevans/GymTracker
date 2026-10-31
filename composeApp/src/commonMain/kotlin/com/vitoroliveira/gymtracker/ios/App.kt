package com.vitoroliveira.gymtracker.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/** Raiz do app de iPhone: decide entre Login e o app (sessão salva no aparelho). */
@Composable
fun App() {
    GymTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!Services.configured) {
                Centered("Chaves do Supabase não configuradas neste build.\nConfigure SUPABASE_URL e SUPABASE_KEY.")
                return@Surface
            }
            // null = carregando a sessão
            var signedIn by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                signedIn = try {
                    Services.auth.awaitSession()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    false
                }
            }
            when (signedIn) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                false -> LoginScreen(onSignedIn = { signedIn = true })
                true -> MainScreen(onSignedOut = { signedIn = false })
            }
        }
    }
}

@Composable
fun Centered(text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
