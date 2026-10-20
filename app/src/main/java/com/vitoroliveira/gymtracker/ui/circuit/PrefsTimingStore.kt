package com.vitoroliveira.gymtracker.ui.circuit

import android.content.Context
import com.vitoroliveira.gymtracker.presentation.circuit.TimingSample
import com.vitoroliveira.gymtracker.presentation.circuit.TimingStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Guarda no aparelho o ritmo aprendido de cada exercício. */
class PrefsTimingStore(context: Context) : TimingStore {
    private val prefs = context.applicationContext.getSharedPreferences("circuit_timing", Context.MODE_PRIVATE)
    private val ser = MapSerializer(String.serializer(), ListSerializer(TimingSample.serializer()))

    override fun load(): Map<String, List<TimingSample>> =
        runCatching { Json.decodeFromString(ser, prefs.getString("data", null) ?: return emptyMap()) }.getOrDefault(emptyMap())

    override fun save(data: Map<String, List<TimingSample>>) {
        prefs.edit().putString("data", Json.encodeToString(ser, data)).apply()
    }
}
