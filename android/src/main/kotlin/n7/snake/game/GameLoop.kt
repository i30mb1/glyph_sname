package n7.snake.game

import android.os.Handler
import android.os.Looper

public class GameLoop(
    private val onTick: () -> Unit,
    private val getIntervalMs: () -> Long
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                onTick()
                handler.postDelayed(this, getIntervalMs())
            }
        }
    }

    public fun start() {
        if (!isRunning) {
            isRunning = true
            handler.postDelayed(tickRunnable, getIntervalMs())
        }
    }

    public fun stop() {
        isRunning = false
        handler.removeCallbacks(tickRunnable)
    }

    public fun isRunning(): Boolean = isRunning

    public fun restart() {
        stop()
        start()
    }
}
