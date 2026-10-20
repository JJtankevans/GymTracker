package com.vitoroliveira.gymtracker.ui.body

import android.content.Context
import com.vitoroliveira.gymtracker.presentation.body.BodyData
import com.vitoroliveira.gymtracker.presentation.body.BodyStore
import kotlinx.serialization.json.Json

/** Guarda perfil corporal e histórico de peso no aparelho (SharedPreferences). */
class PrefsBodyStore(context: Context) : BodyStore {
    private val prefs = context.applicationContext.getSharedPreferences("body_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): BodyData =
        runCatching { json.decodeFromString<BodyData>(prefs.getString("data", null) ?: return BodyData()) }.getOrDefault(BodyData())

    override fun save(data: BodyData) {
        prefs.edit().putString("data", json.encodeToString(BodyData.serializer(), data)).apply()
    }
}
