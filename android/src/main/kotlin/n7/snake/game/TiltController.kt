package n7.snake.game

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

public class TiltController(context: Context) {

    private val gyroController = GyroSensorController(context)
    private var currentDirection: Direction = Direction.RIGHT
    private var scope: CoroutineScope? = null

    public var onDirectionChanged: ((Direction) -> Unit)? = null

    private companion object {
        const val TAG = "TiltController"
        // Threshold for gyroscope angular velocity (rad/s)
        const val GYRO_THRESHOLD = 1f
    }

    public fun start() {
        Log.d(TAG, "start()")
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope?.launch {
            gyroController.sensorFlow.collectLatest { data ->
                processGyroData(data)
            }
        }
    }

    public fun stop() {
        Log.d(TAG, "stop()")
        scope?.cancel()
        scope = null
    }

    public fun getCurrentDirection(): Direction = currentDirection

    public fun setDirection(direction: Direction) {
        Log.d(TAG, "setDirection($direction)")
        currentDirection = direction
    }

    private fun processGyroData(data: SensorData) {
        val absX = abs(data.x)
        val absY = abs(data.y)

        // Log for debugging
        Log.d(TAG, "Gyro X:${String.format("%.2f", data.x)} Y:${String.format("%.2f", data.y)} Z:${String.format("%.2f", data.z)}")

        // Check if we're above threshold
        if (absX < GYRO_THRESHOLD && absY < GYRO_THRESHOLD) {
            return // Not enough movement
        }

        // Determine dominant axis and direction
        val newDirection = if (absX > absY) {
            // X dominant - pitch - UP or DOWN
            // Positive X = tilting forward = UP
            // Negative X = tilting backward = DOWN
            if (data.x > GYRO_THRESHOLD) Direction.UP else Direction.DOWN
        } else {
            // Y dominant - roll - LEFT or RIGHT
            // Positive Y = tilting right = RIGHT
            // Negative Y = tilting left = LEFT
            if (data.y > GYRO_THRESHOLD) Direction.RIGHT else Direction.LEFT
        }

        // Only send if direction changed (let SnakeGame validate opposite check)
        if (newDirection != currentDirection) {
            Log.d(TAG, ">>> DIRECTION: $currentDirection -> $newDirection")
            currentDirection = newDirection
            onDirectionChanged?.invoke(newDirection)
        }
    }
}
