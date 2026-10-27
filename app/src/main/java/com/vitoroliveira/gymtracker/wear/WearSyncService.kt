package com.vitoroliveira.gymtracker.wear

import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.domain.usecase.ImportWatchWorkout
import com.vitoroliveira.gymtracker.watch.WatchJson
import com.vitoroliveira.gymtracker.watch.WatchPaths
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.buildWatchRoutines
import com.vitoroliveira.gymtracker.watch.epochMsToIso
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject

/**
 * Ponte celular ↔ relógio (Data Layer). O relógio não fala com o Supabase: pede as rotinas e manda os treinos
 * feitos para cá, e este serviço usa os mesmos repositórios do app (com a sessão logada no celular).
 * O sistema acorda o serviço mesmo com o app fechado. Só conversa com o app do relógio que tem o mesmo
 * pacote e a mesma assinatura (instalar os dois APKs do mesmo build).
 */
class WearSyncService : WearableListenerService() {

    private val auth: AuthRepository by inject()
    private val groups: MuscleGroupRepository by inject()
    private val exercises: ExerciseRepository by inject()
    private val workouts: WorkoutRepository by inject()

    private val data: DataClient by lazy { Wearable.getDataClient(this) }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchPaths.REQUEST_ROUTINES) return
        // os callbacks rodam numa thread de fundo; segurar aqui mantém o serviço vivo até terminar
        runBlocking {
            withTimeoutOrNull(45_000) {
                runCatching { importPending() }
                runCatching { publishRoutines() }
            }
        }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        // ler tudo antes de sair do callback (o buffer é liberado depois)
        val arrived = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path?.startsWith(WatchPaths.WORKOUT_PREFIX) == true }
            .map { it.dataItem.uri to DataMapItem.fromDataItem(it.dataItem).dataMap.getString(WatchPaths.KEY_JSON) }
        if (arrived.isEmpty()) return
        runBlocking {
            withTimeoutOrNull(60_000) {
                var saved = false
                for ((uri, json) in arrived) saved = runCatching { importOne(uri, json) }.getOrDefault(false) || saved
                // cargas novas: o relógio já recebe a "última vez" atualizada
                if (saved) runCatching { publishRoutines() }
            }
        }
    }

    /** Treinos do relógio que ficaram para trás (ex.: o celular estava sem internet quando chegaram). */
    private suspend fun importPending() {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(WatchPaths.WORKOUT_PREFIX).build()
        val buffer = data.getDataItems(uri, DataClient.FILTER_PREFIX).await()
        val pending = try {
            buffer.map { it.uri to DataMapItem.fromDataItem(it).dataMap.getString(WatchPaths.KEY_JSON) }
        } finally {
            buffer.release()
        }
        for ((itemUri, json) in pending) runCatching { importOne(itemUri, json) }
    }

    /** Grava o treino no Supabase e apaga o item (é assim que o relógio sabe que foi salvo). false = ficou para depois. */
    private suspend fun importOne(uri: Uri, json: String?): Boolean {
        val workout = json?.let(WatchJson::decodeWorkout)
        if (workout == null) {
            data.deleteDataItems(uri).await()          // item ilegível: não adianta tentar de novo
            return false
        }
        if (!auth.awaitSession()) return false        // sem conta no celular: espera até alguém entrar
        val groupIds = groups.getAll().mapNotNull { it.id }.toSet()
        val exerciseIds = exercises.getAll().mapNotNull { it.id }.toSet()
        ImportWatchWorkout(workouts)(workout, groupIds, exerciseIds)
        data.deleteDataItems(uri).await()
        uri.host?.let { node ->
            runCatching {
                Wearable.getMessageClient(this).sendMessage(node, WatchPaths.WORKOUT_SAVED, workout.id.toByteArray(Charsets.UTF_8)).await()
            }
        }
        return true
    }

    /** Publica grupos, exercícios e a última carga de cada um para o relógio. */
    private suspend fun publishRoutines() {
        val now = System.currentTimeMillis()
        val routines = if (!auth.awaitSession()) {
            WatchRoutines(loggedIn = false, generatedAt = now)
        } else {
            // 45 dias: o Supabase devolve no máximo 1000 séries por chamada
            val since = epochMsToIso(now - 45L * 24 * 60 * 60 * 1000)
            buildWatchRoutines(
                groups = groups.getAll(),
                exercises = exercises.getAll(),
                recentSets = runCatching { workouts.getSetsSince(since) }.getOrDefault(emptyList()),
                records = runCatching { workouts.getRecordsSummary() }.getOrDefault(emptyList()),
                nowMs = now
            )
        }
        val request = PutDataMapRequest.create(WatchPaths.ROUTINES).apply {
            dataMap.putString(WatchPaths.KEY_JSON, WatchJson.encodeRoutines(routines))
        }.asPutDataRequest().setUrgent()
        data.putDataItem(request).await()
    }
}
