package com.vitoroliveira.gymtracker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.presentation.viewmodel.SessionState
import com.vitoroliveira.gymtracker.presentation.viewmodel.SessionViewModel
import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import org.koin.compose.viewmodel.koinViewModel

/** Tela inicial: espera a sessão salva carregar e decide Home x Login. */
@Composable
fun SplashScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SessionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            SessionState.SignedIn -> onSignedIn()
            SessionState.SignedOut -> onSignedOut()
            SessionState.Loading -> Unit
        }
    }

    ThemedScaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GymTracker", style = MaterialTheme.typography.displaySmall)
            CircularProgressIndicator()
        }
    }
}
