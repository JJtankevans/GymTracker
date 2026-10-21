package com.vitoroliveira.gymtracker.ui.profile

import com.vitoroliveira.gymtracker.ui.theme.ThemedScaffold
import com.vitoroliveira.gymtracker.ui.theme.ThemedTopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitoroliveira.gymtracker.data.model.Profile
import com.vitoroliveira.gymtracker.presentation.viewmodel.ProfileUiState
import com.vitoroliveira.gymtracker.presentation.viewmodel.ProfileViewModel
import com.vitoroliveira.gymtracker.ui.theme.GymTrackerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    onNavigateToThemes: () -> Unit = {},
    onNavigateToEvolution: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Galeria do sistema (Photo Picker): não pede permissão de armazenamento
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val jpeg = withContext(Dispatchers.Default) { AvatarImageProcessor.toAvatarJpeg(context, uri) }
                if (jpeg != null) viewModel.uploadAvatar(jpeg) else viewModel.onAvatarReadFailed()
            }
        }
    }

    // Observa o isSignedOut para navegar automaticamente
    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) onLogout()
    }

    ProfileScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToThemes = onNavigateToThemes,
        onNavigateToEvolution = onNavigateToEvolution,
        onUpdateWeeklyGoal = { viewModel.updateWeeklyGoal(it) },
        onSignOut = { viewModel.signOut() },
        onClearMessages = { viewModel.clearMessages() },
        onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = { viewModel.removeAvatar() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onBack: () -> Unit = {},
    onNavigateToThemes: () -> Unit = {},
    onNavigateToEvolution: () -> Unit = {},
    onUpdateWeeklyGoal: (Int) -> Unit,
    onSignOut: () -> Unit,
    onClearMessages: () -> Unit,
    onPickPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {}
) {
    var weeklyGoal by remember(uiState.profile) {
        mutableStateOf(uiState.profile?.weeklyGoal ?: 3)
    }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    ThemedScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ThemedTopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->

        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Foto de perfil: toque abre "escolher da galeria" / "remover"
                Box(modifier = Modifier.size(96.dp)) {
                    UserAvatar(
                        url = uiState.profile?.avatarUrl,
                        size = 96.dp,
                        fallbackName = uiState.profile?.username,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !uiState.isUpdatingAvatar) { showPhotoSheet = true }
                    )
                    if (uiState.isUpdatingAvatar) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = Color.White)
                        }
                    } else {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(30.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Alterar foto",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.profile?.username ?: "",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(32.dp))

                // Meta semanal
                Text(
                    text = "Meta semanal de treinos",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$weeklyGoal ${if (weeklyGoal == 1) "dia" else "dias"} por semana",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = weeklyGoal.toFloat(),
                    onValueChange = { weeklyGoal = it.toInt() },
                    valueRange = 1f..7f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 dia", style = MaterialTheme.typography.labelSmall)
                    Text("7 dias", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onUpdateWeeklyGoal(weeklyGoal) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && weeklyGoal != uiState.profile?.weeklyGoal
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Salvar meta")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = onNavigateToEvolution,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evolução e meus dados")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onNavigateToThemes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aparência e temas")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sair da conta")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            ListItem(
                headlineContent = { Text("Escolher da galeria") },
                leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                modifier = Modifier.clickable { showPhotoSheet = false; onPickPhoto() }
            )
            if (uiState.profile?.avatarUrl != null) {
                ListItem(
                    headlineContent = { Text("Remover foto", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showPhotoSheet = false; onRemovePhoto() }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta") },
            text = { Text("Tem certeza que deseja sair?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onSignOut()
                }) {
                    Text("Sair", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    GymTrackerTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                profile = Profile(
                    id = "1",
                    username = "vitoroliveira",
                    weeklyGoal = 4
                )
            ),
            onBack = {},
            onUpdateWeeklyGoal = {},
            onSignOut = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenLoadingPreview() {
    GymTrackerTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(isLoading = true),
            onUpdateWeeklyGoal = {},
            onSignOut = {},
            onClearMessages = {}
        )
    }
}