package n7.snake.game

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlin.math.abs

public class TiltController(context: Context) {

    private val gyroscope = GyroscopeController(context)
    private var currentDirection: Direction = Direction.RIGHT
    private var scope: CoroutineScope? = null

    public var onDirectionChanged: ((Direction) -> Unit)? = null

    private companion object {
        const val THRESHOLD = 3f
        const val DECAY = 0.95f
    }

    public fun start() {
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        gyroscope.flow
            .scan(SensorData(0f, 0f)) { prev, current ->
                SensorData(
                    x = (prev.x + current.x) * DECAY,
                    y = (prev.y + current.y) * DECAY
                )
            }
            .onEach { accumulated ->
                val newDirection = getDirection(accumulated.x, accumulated.y)
                if (newDirection != null && newDirection != currentDirection && !isOpposite(newDirection, currentDirection)) {
                    currentDirection = newDirection
                    onDirectionChanged?.invoke(newDirection)
                }
            }
            .launchIn(scope!!)
    }

    public fun stop() {
        scope?.cancel()
        scope = null
    }

    public fun getCurrentDirection(): Direction = currentDirection

    public fun setDirection(direction: Direction) {
        currentDirection = direction
    }

    private fun getDirection(x: Float, y: Float): Direction? {
        val absX = abs(x)
        val absY = abs(y)

        if (absX < THRESHOLD && absY < THRESHOLD) return null

        return if (absX > absY) {
            if (x > 0) Direction.UP else Direction.DOWN
        } else {
            if (y > 0) Direction.RIGHT else Direction.LEFT
        }
    }

    private fun isOpposite(a: Direction, b: Direction): Boolean {
        return (a == Direction.UP && b == Direction.DOWN) ||
                (a == Direction.DOWN && b == Direction.UP) ||
                (a == Direction.LEFT && b == Direction.RIGHT) ||
                (a == Direction.RIGHT && b == Direction.LEFT)
    }
}
