package com.vitoroliveira.gymtracker.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.vitoroliveira.gymtracker.watch.WatchJson
import com.vitoroliveira.gymtracker.watch.WatchPaths
import com.vitoroliveira.gymtracker.watch.WatchRoutines
import com.vitoroliveira.gymtracker.watch.WatchWorkout
import kotlinx.coroutines.tasks.await

/**
 * Data Layer do lado do relógio (ver [WatchPaths]). Só funciona com o app do celular instalado a partir
 * do MESMO build (mesmo pacote e mesma assinatura); sem isso, os pedidos simplesmente não têm resposta.
 */
class WatchSync(context: Context) {
    private val data = Wearable.getDataClient(context)
    private val messages = Wearable.getMessageClient(context)
    private val nodes = Wearable.getNodeClient(context)

    private fun uri(path: String): Uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(path).build()

    /** As rotinas que o celular já deixou na Data Layer (lidas localmente, sem esperar o celular). */
    suspend fun readRoutines(): WatchRoutines? {
        val buffer = data.getDataItems(uri(WatchPaths.ROUTINES)).await()
        try {
            for (item in buffer) {
                val json = DataMapItem.fromDataItem(item).dataMap.getString(WatchPaths.KEY_JSON) ?: continue
                WatchJson.decodeRoutines(json)?.let { return it }
            }
            return null
        } finally {
            buffer.release()
        }
    }

    /** Pede rotinas novas ao celular. false = nenhum celular conectado agora. */
    suspend fun requestRoutines(): Boolean {
        val connected = nodes.connectedNodes.await()
        connected.forEach { messages.sendMessage(it.id, WatchPaths.REQUEST_ROUTINES, ByteArray(0)).await() }
        return connected.isNotEmpty()
    }

    /** Entrega o treino à Data Layer (vai para o celular agora ou quando ele aparecer). */
    suspend fun sendWorkout(w: WatchWorkout) {
        val request = PutDataMapRequest.create(WatchPaths.WORKOUT_PREFIX + w.id).apply {
            dataMap.putString(WatchPaths.KEY_JSON, WatchJson.encodeWorkout(w))
            dataMap.putLong("enviadoEm", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        data.putDataItem(request).await()
    }

    /** O item do treino ainda está na Data Layer? Quando o celular grava no Supabase, ele apaga o item. */
    suspend fun isQueued(workoutId: String): Boolean {
        val buffer = data.getDataItems(uri(WatchPaths.WORKOUT_PREFIX + workoutId)).await()
        try {
            return buffer.count > 0
        } finally {
            buffer.release()
        }
    }

    /**
     * Ouve o celular enquanto o app está aberto: rotinas novas, treino salvo (mensagem) e item de treino apagado.
     * Devolve uma função que para de ouvir.
     */
    fun listen(onRoutines: (WatchRoutines) -> Unit, onWorkoutSaved: (String) -> Unit): () -> Unit {
        val dataListener = DataClient.OnDataChangedListener { events ->
            for (event in events) {
                val path = event.dataItem.uri.path ?: continue
                when {
                    path == WatchPaths.ROUTINES && event.type == DataEvent.TYPE_CHANGED ->
                        DataMapItem.fromDataItem(event.dataItem).dataMap.getString(WatchPaths.KEY_JSON)
                            ?.let(WatchJson::decodeRoutines)?.let(onRoutines)
                    path.startsWith(WatchPaths.WORKOUT_PREFIX) && event.type == DataEvent.TYPE_DELETED ->
                        onWorkoutSaved(path.removePrefix(WatchPaths.WORKOUT_PREFIX))
                }
            }
        }
        val messageListener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == WatchPaths.WORKOUT_SAVED) onWorkoutSaved(String(event.data, Charsets.UTF_8))
        }
        data.addListener(dataListener)
        messages.addListener(messageListener)
        return {
            data.removeListener(dataListener)
            messages.removeListener(messageListener)
        }
    }
}
