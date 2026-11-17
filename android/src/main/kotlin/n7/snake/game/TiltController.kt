package n7.snake.game

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

/**
 * Turns phone tilt into snake turns using the gravity vector.
 *
 * The pose at [start] is the reference. Only the axis perpendicular to the current
 * movement turns the snake: while moving vertically, left/right tilt turns it, and vice versa.
 *
 * The signal is smoothed with a low-pass filter. A turn happens when the perpendicular
 * lean enters POS or NEG (hysteresis) and is strong enough compared with the lean along
 * the movement axis.
 *
 * After a turn, another turn needs either a release (the perpendicular lean returns to
 * neutral), or a strong lean ([STRONG]) held for at least [LOCK_MS]. This stops residual
 * lean from turning the snake back and forth.
 */
public class TiltController(
    context: Context,
    private val currentDirection: () -> Direction
) {

    private enum class Side { NONE, NEG, POS }

    private val gravity = GravityController(context)
    private var scope: CoroutineScope? = null

    private var baseX: Float? = null
    private var baseY: Float? = null
    private var filteredX: Float? = null
    private var filteredY: Float? = null
    private var side = Side.NONE
    private var awaitingRelease = false
    private var turnedAtNs = 0L
    private var sampleCounter = 0

    public var onDirectionChanged: ((Direction) -> Unit)? = null

    private companion object {
        const val TAG = "TiltController"
        // m/s². Enter a side above this, return to NONE below EXIT
        const val ENTER = 1.2f
        const val EXIT = 0.6f
        // m/s². A lean this strong turns again after LOCK_MS, even without release
        const val STRONG = 3.0f
        const val LOCK_MS = 300L
        // The perpendicular lean must be at least this share of the movement-axis lean
        const val CROSS_RATIO = 0.5f
        // Low-pass smoothing factor per sensor sample (about 100 ms at fastest sampling)
        const val ALPHA = 0.2f
    }

    public fun start() {
        scope?.cancel()
        resetState()
        if (!gravity.isAvailable) {
            Log.w(TAG, "No gravity or accelerometer sensor, tilt controls disabled")
            return
        }
        val newScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope = newScope

        gravity.flow
            .onEach { sample -> onSample(sample.x, sample.y) }
            .launchIn(newScope)
    }

    public fun stop() {
        scope?.cancel()
        scope = null
        resetState()
    }

    private fun resetState() {
        baseX = null
        baseY = null
        filteredX = null
        filteredY = null
        side = Side.NONE
        awaitingRelease = false
        turnedAtNs = 0L
        sampleCounter = 0
    }

    private fun onSample(x: Float, y: Float) {
        // Exponential low-pass filter: removes jitter around the thresholds
        val fx = filteredX?.let { it + ALPHA * (x - it) } ?: x
        val fy = filteredY?.let { it + ALPHA * (y - it) } ?: y
        filteredX = fx
        filteredY = fy

        val bx = baseX ?: fx.also { baseX = it }
        val by = baseY ?: fy.also { baseY = it }

        val dx = fx - bx
        val dy = fy - by

        if (++sampleCounter % 5 == 0) {
            Log.v(TAG, "sample dx=%.2f dy=%.2f".format(dx, dy))
        }

        val current = currentDirection()
        val vertical = current == Direction.UP || current == Direction.DOWN
        val perpendicular = if (vertical) dx else dy
        val parallel = if (vertical) dy else dx

        // A release is the perpendicular lean returning to neutral
        if (abs(perpendicular) < EXIT) awaitingRelease = false

        // Hysteresis on the perpendicular axis; sideways leakage of a strong lean along the
        // movement axis is ignored
        val dominant = abs(perpendicular) >= CROSS_RATIO * abs(parallel)
        side = when {
            !dominant -> Side.NONE
            perpendicular >= ENTER -> Side.POS
            perpendicular <= -ENTER -> Side.NEG
            abs(perpendicular) < EXIT -> Side.NONE
            else -> side
        }

        if (side == Side.NONE) return

        if (awaitingRelease) {
            val timerOver = System.nanoTime() - turnedAtNs >= LOCK_MS * 1_000_000L
            if (!(timerOver && abs(perpendicular) >= STRONG)) return
        }

        val turn = if (vertical) {
            if (side == Side.POS) Direction.RIGHT else Direction.LEFT
        } else {
            if (side == Side.POS) Direction.DOWN else Direction.UP
        }

        awaitingRelease = true
        turnedAtNs = System.nanoTime()
        side = Side.NONE
        Log.d(TAG, "tilt $turn perpendicular=%.2f".format(perpendicular))
        onDirectionChanged?.invoke(turn)
    }
}
