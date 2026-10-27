package com.vitoroliveira.gymtracker.ui.music

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.vitoroliveira.gymtracker.data.model.CapturedTrack
import com.vitoroliveira.gymtracker.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.Instant

/** Escolha do usuário: guardar as músicas que tocam durante os treinos (desligado por padrão). */
class SocialPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gymtracker_prefs", Context.MODE_PRIVATE)
    private val _recordMusic = MutableStateFlow(prefs.getBoolean("record_workout_music", false))
    val recordMusic: StateFlow<Boolean> = _recordMusic.asStateFlow()
    fun setRecordMusic(on: Boolean) { prefs.edit().putBoolean("record_workout_music", on).apply(); _recordMusic.value = on }
}

/**
 * Durante um treino, anota cada música nova que começa a tocar. No fim, tenta enviar
 * (só funciona depois da migração 002 no Supabase; se falhar, ignora sem atrapalhar o treino).
 */
class WorkoutTrackRecorder(
    private val music: MusicController,
    private val prefs: SocialPrefs,
    private val repo: TrackRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var workoutId: String? = null
    private val tracks = mutableListOf<CapturedTrack>()
    private var lastKey: String? = null

    val count: Int get() = tracks.size

    fun begin(id: String) {
        if (workoutId == id || !prefs.recordMusic.value) return
        workoutId = id; tracks.clear(); lastKey = null
        job?.cancel()
        job = scope.launch {
            music.state.collect { st ->
                val np = st.nowPlaying ?: return@collect
                if (!np.isPlaying || np.title.isBlank()) return@collect
                val key = np.title + "|" + np.artist
                if (key == lastKey) return@collect
                lastKey = key
                synchronized(tracks) {
                    tracks += CapturedTrack(np.title, np.artist.ifBlank { null }, np.album, np.app.ifBlank { null }, Instant.now().toString())
                }
            }
        }
    }

    fun finishAndUpload() {
        val id = workoutId ?: return
        job?.cancel(); job = null; workoutId = null
        val toSend = synchronized(tracks) { tracks.toList().also { tracks.clear() } }
        if (toSend.isEmpty()) return
        scope.launch { runCatching { repo.uploadTracks(id, toSend) } }
    }

    /** Joga fora as músicas anotadas sem enviar (ao sair da conta no meio de um treino). */
    fun discard() {
        job?.cancel(); job = null; workoutId = null; lastKey = null
        synchronized(tracks) { tracks.clear() }
    }
}

/** Liga a gravação ao ciclo do treino: começa quando há treino ativo, envia quando termina. */
@Composable
fun TrackRecording(workoutId: String?, finished: Boolean, recorder: WorkoutTrackRecorder = koinInject()) {
    LaunchedEffect(workoutId) { if (workoutId != null) recorder.begin(workoutId) }
    LaunchedEffect(finished) { if (finished) recorder.finishAndUpload() }
}
