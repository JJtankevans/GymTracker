package com.vitoroliveira.gymtracker.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitoroliveira.gymtracker.presentation.viewmodel.AuthViewModel

/** Entrar ou criar conta (mesmo AuthViewModel do Android). */
@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val vm = remember { AuthViewModel(Services.auth) }
    val state by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) { if (state.isLoggedIn) onSignedIn() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("GYMTRACKER", color = Lime, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 3.sp)
        Text(if (creating) "Criar conta" else "Entrar", color = Muted)
        Spacer(Modifier.height(28.dp))
        if (creating) {
            OutlinedTextField(username, { username = it }, label = { Text("Nome de usuário") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            email, { email = it.trim() }, label = { Text("E-mail") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("Senha") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        state.errorMessage?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        state.confirmationEmail?.let {
            Spacer(Modifier.height(10.dp))
            Text("Conta criada. Confirme o e-mail enviado para $it e depois entre.", color = Gold)
        }
        state.infoMessage?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Muted) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (creating) vm.signUp(email, password, username.trim()) else vm.signIn(email, password) },
            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank() && (!creating || username.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
            else Text(if (creating) "CRIAR CONTA" else "ENTRAR", fontWeight = FontWeight.Black)
        }
        TextButton(onClick = { creating = !creating; vm.clearError() }) {
            Text(if (creating) "Já tenho conta" else "Criar uma conta")
        }
    }
}
