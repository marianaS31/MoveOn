package pt.ipp.estg.moveon.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorManagerHelper(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    // Flow que escuta o sensor de luz ambiente (em Lux)
    fun getAmbientLightFlow(): Flow<Float> = callbackFlow {
        if (lightSensor == null) {
            trySend(100f) // Valor por omissão caso o emulador/dispositivo não tenha sensor
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { luxValue ->
                    trySend(luxValue)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Flow que escuta alterações no nível da bateria (%)
    fun getBatteryLevelFlow(): Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    trySend(batteryPct)
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}