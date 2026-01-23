package n7.snake.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class GyroSensorController(
    context: Context
) {

    private companion object {
        const val TAG = "GyroSensorController"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val sensorFlow: Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val data = SensorData(
                        x = it.values[0],
                        y = it.values[1],
                        z = it.values[2]
                    )
                    trySend(data)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d(TAG, "Sensor accuracy changed: $accuracy")
            }
        }

        Log.d(TAG, "Gyroscope sensor registered")
        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            Log.d(TAG, "Gyroscope sensor unregistered")
            sensorManager.unregisterListener(listener)
        }
    }
}
