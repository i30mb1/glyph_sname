package n7.snake

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import n7.snake.game.Direction
import n7.snake.game.GameLoop
import n7.snake.game.SnakeGame
import n7.snake.game.SnakeRenderer
import n7.snake.game.TiltController

public class SnakeController(context: Context) {

    private enum class GameState {
        WAITING_TO_START,
        PLAYING,
        GAME_OVER
    }

    public interface FrameCallback {
        public fun onFrame(frame: GlyphMatrixFrame)
    }

    private companion object {
        const val TAG = "SnakeController"
    }

    private val game = SnakeGame()
    private val renderer = SnakeRenderer(context)
    private val tiltController = TiltController(context)

    private var frameCallback: FrameCallback? = null
    private var isActive = false
    private var gameState = GameState.WAITING_TO_START

    private val gameLoop = GameLoop(
        onTick = { onGameTick() },
        getIntervalMs = { game.getSpeedMs() }
    )

    init {
        tiltController.onDirectionChanged = { direction ->
            Log.d(TAG, "onDirectionChanged: $direction, gameState: $gameState")
            if (gameState == GameState.PLAYING) {
                game.changeDirection(direction)
            }
        }

        game.onGameOver = {
            Log.d(TAG, "Game Over!")
            gameState = GameState.GAME_OVER
            gameLoop.stop()
            renderFrame()
        }
    }

    public fun setFrameCallback(callback: FrameCallback) {
        this.frameCallback = callback
    }

    public fun start() {
        Log.d(TAG, "start()")
        if (isActive) return
        isActive = true
        gameState = GameState.WAITING_TO_START
        renderFrame()
    }

    public fun stop() {
        Log.d(TAG, "stop()")
        if (!isActive) return
        isActive = false

        gameLoop.stop()
        tiltController.stop()
        clearDisplay()
    }

    public fun onLongPress() {
        Log.d(TAG, "onLongPress(), gameState: $gameState")
        when (gameState) {
            GameState.WAITING_TO_START -> {
                startGame(resetGame = false)
            }
            GameState.GAME_OVER -> {
                startGame(resetGame = true)
            }
            GameState.PLAYING -> {
                // Do nothing during gameplay
            }
        }
    }

    private fun startGame(resetGame: Boolean) {
        Log.d(TAG, "startGame(resetGame=$resetGame)")
        if (resetGame) {
            game.reset()
        }
        renderer.reset()
        tiltController.setDirection(Direction.RIGHT)
        tiltController.start()
        gameState = GameState.PLAYING
        gameLoop.start()
        renderFrame()
    }

    private fun onGameTick() {
        game.update()
        renderFrame()
    }

    private fun renderFrame() {
        try {
            val frame = when (gameState) {
                GameState.WAITING_TO_START -> renderer.renderStartScreen(game)
                GameState.PLAYING -> renderer.render(game)
                GameState.GAME_OVER -> renderer.renderGameOver(game)
            }
            frameCallback?.onFrame(frame)
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering frame", e)
        }
    }

    private fun clearDisplay() {
        val emptyFrame = renderer.renderEmpty()
        frameCallback?.onFrame(emptyFrame)
    }
}
