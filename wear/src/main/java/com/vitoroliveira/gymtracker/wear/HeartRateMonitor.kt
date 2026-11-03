package com.vitoroliveira.gymtracker.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlin.math.roundToInt

/** Permissão dos batimentos: BODY_SENSORS até o Wear OS 5; a partir do Wear OS 6 (API 36), READ_HEART_RATE. */
val HEART_RATE_PERMISSION: String =
    if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

fun Context.hasHeartRatePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, HEART_RATE_PERMISSION) == PackageManager.PERMISSION_GRANTED

/** Batimentos ao vivo pelo Health Services (só enquanto a tela do treino está ligada, para poupar bateria). */
class HeartRateMonitor(private val context: Context) {
    private val client by lazy { HealthServices.getClient(context).measureClient }
    private val main = Handler(Looper.getMainLooper())
    private var callback: MeasureCallback? = null

    fun start(onBpm: (Int) -> Unit) {
        if (callback != null || !context.hasHeartRatePermission()) return
        val cb = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}
            override fun onDataReceived(data: DataPointContainer) {
                val bpm = data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: return
                if (bpm > 0) main.post { onBpm(bpm.roundToInt()) }
            }
        }
        try {
            client.registerMeasureCallback(DataType.HEART_RATE_BPM, cb)
            callback = cb
        } catch (_: Exception) {
            // relógio sem sensor ou Health Services indisponível: segue sem batimentos
        }
    }

    fun stop() {
        val cb = callback ?: return
        callback = null
        try {
            client.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, cb)
        } catch (_: Exception) {
        }
    }
}
