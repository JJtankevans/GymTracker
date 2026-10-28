package com.vitoroliveira.gymtracker.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.vitoroliveira.gymtracker.watch.WatchPaths
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject

/**
 * Ponte celular ↔ relógio (Data Layer). O relógio não fala com o Supabase: pede as rotinas e manda os treinos
 * feitos para cá, e o [WatchBridge] usa os mesmos repositórios do app (com a sessão logada no celular).
 * O sistema acorda o serviço mesmo com o app fechado. Só conversa com o app do relógio que tem o mesmo
 * pacote e a mesma assinatura (instalar os dois APKs do mesmo build).
 */
class WearSyncService : WearableListenerService() {

    private val bridge: WatchBridge by inject()

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchPaths.REQUEST_ROUTINES) return
        // os callbacks rodam numa thread de fundo; segurar aqui mantém o serviço vivo até terminar
        runBlocking {
            withTimeoutOrNull(45_000) {
                runCatching { bridge.importPending() }
                runCatching { bridge.publishRoutines() }
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
                for ((uri, json) in arrived) saved = runCatching { bridge.importOne(uri, json) }.getOrDefault(false) || saved
                // cargas novas: o relógio já recebe a "última vez" e a semana atualizadas
                if (saved) runCatching { bridge.publishRoutines() }
            }
        }
    }
}
