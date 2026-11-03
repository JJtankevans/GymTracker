package com.vitoroliveira.gymtracker.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.vitoroliveira.gymtracker.domain.repository.AuthRepository
import com.vitoroliveira.gymtracker.domain.repository.ExerciseRepository
import com.vitoroliveira.gymtracker.domain.repository.MuscleGroupRepository
import com.vitoroliveira.gymtracker.domain.repository.WorkoutRepository
import com.vitoroliveira.gymtracker.domain.usecase.BuildWatchRoutines
import com.vitoroliveira.gymtracker.domain.usecase.ImportWatchWorkout
import com.vitoroliveira.gymtracker.watch.WatchJson
import com.vitoroliveira.gymtracker.watch.WatchPaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * O lado do celular da conversa com o relógio (Data Layer), usado pelo [WearSyncService] (acordado pelo relógio)
 * e pelo cartão "Relógio" do Perfil (sincronizar na hora).
 */
class WatchBridge(
    context: Context,
    private val build: BuildWatchRoutines,
    private val auth: AuthRepository,
    private val groups: MuscleGroupRepository,
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository
) {
    private val app = context.applicationContext
    private val data: DataClient by lazy { Wearable.getDataClient(app) }

    /** Nomes dos relógios conectados agora (vazio = nenhum ou sem os serviços do Google). */
    suspend fun connectedWatches(): List<String> =
        Wearable.getNodeClient(app).connectedNodes.await().map { it.displayName }

    private var lastAutoPublish = 0L

    /**
     * Ao abrir o app: se há relógio conectado e a última publicação automática foi há mais de 15 min, publica em
     * segundo plano. Sem relógio (ou sem os serviços do Google) não faz nada e não mostra erro.
     */
    fun publishInBackgroundIfStale(scope: CoroutineScope) {
        val now = System.currentTimeMillis()
        if (now - lastAutoPublish < 15 * 60_000L) return
        lastAutoPublish = now
        scope.launch {
            try {
                if (connectedWatches().isNotEmpty()) {
                    importPending()
                    publishRoutines()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    /** Aviso no celular de que o treino feito no relógio entrou no Histórico. */
    private fun notifySaved(title: String, sets: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        app.getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(
            android.app.NotificationChannel("relogio", "Treinos do relógio", android.app.NotificationManager.IMPORTANCE_LOW)
        )
        val open = android.app.PendingIntent.getActivity(
            app, 71, android.content.Intent(app, com.vitoroliveira.gymtracker.MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = androidx.core.app.NotificationCompat.Builder(app, "relogio")
            .setSmallIcon(com.vitoroliveira.gymtracker.R.drawable.ic_launcher_foreground)
            .setContentTitle("Treino do relógio salvo")
            .setContentText("$title · $sets ${if (sets == 1) "série" else "séries"} no Histórico")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            androidx.core.app.NotificationManagerCompat.from(app).notify(400, n)
        } catch (_: SecurityException) {
        }
    }

    /** Publica grupos, exercícios, última carga, semana e sugestão para o relógio. */
    suspend fun publishRoutines() {
        val routines = build()
        val request = PutDataMapRequest.create(WatchPaths.ROUTINES).apply {
            dataMap.putString(WatchPaths.KEY_JSON, WatchJson.encodeRoutines(routines))
        }.asPutDataRequest().setUrgent()
        data.putDataItem(request).await()
    }

    /** Treinos do relógio que ficaram para trás (ex.: o celular estava sem internet quando chegaram). Devolve quantos salvou. */
    suspend fun importPending(): Int {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(WatchPaths.WORKOUT_PREFIX).build()
        val buffer = data.getDataItems(uri, DataClient.FILTER_PREFIX).await()
        val pending = try {
            buffer.map { it.uri to DataMapItem.fromDataItem(it).dataMap.getString(WatchPaths.KEY_JSON) }
        } finally {
            buffer.release()
        }
        return pending.count { (itemUri, json) -> runCatching { importOne(itemUri, json) }.getOrDefault(false) }
    }

    /** Grava o treino no Supabase e apaga o item (é assim que o relógio sabe que foi salvo). false = ficou para depois. */
    suspend fun importOne(uri: Uri, json: String?): Boolean {
        val workout = json?.let(WatchJson::decodeWorkout)
        if (workout == null) {
            data.deleteDataItems(uri).await()          // item ilegível: não adianta tentar de novo
            return false
        }
        if (!auth.awaitSession()) return false        // sem conta no celular: espera até alguém entrar
        val groupIds = groups.getAll().mapNotNull { it.id }.toSet()
        val exerciseIds = exercises.getAll().mapNotNull { it.id }.toSet()
        val result = ImportWatchWorkout(workouts)(workout, groupIds, exerciseIds)
        data.deleteDataItems(uri).await()
        if (result == ImportWatchWorkout.Result.SAVED) notifySaved(workout.title, workout.sets.size)
        uri.host?.let { node ->
            runCatching {
                Wearable.getMessageClient(app).sendMessage(node, WatchPaths.WORKOUT_SAVED, workout.id.toByteArray(Charsets.UTF_8)).await()
            }
        }
        return true
    }
}
