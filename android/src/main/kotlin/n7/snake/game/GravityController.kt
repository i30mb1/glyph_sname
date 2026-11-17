package n7.snake.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Streams the gravity vector (x, y) in device coordinates, in m/s².
 * Unlike the gyroscope, gravity reflects the phone's tilt angle, so a held
 * pose gives a steady value instead of a short spike.
 */
internal class GravityController(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = sensor != null

    val flow: Flow<SensorData> = callbackFlow {
        val currentSensor = sensor
        if (currentSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(SensorData(x = event.values[0], y = event.values[1]))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, currentSensor, SensorManager.SENSOR_DELAY_FASTEST)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
