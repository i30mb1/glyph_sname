package n7.snake.game

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils
import kotlin.math.sin

public class SnakeRenderer(private val context: Context) {

    private companion object {
        const val TAG = "SnakeRenderer"
        const val HEAD_BRIGHTNESS = 209
        const val BODY_BRIGHTNESS = 100
        const val FOOD_BRIGHTNESS_HIGH = 4000
        const val FOOD_BRIGHTNESS_LOW = 400
        const val FOOD_PULSE_STEPS = 30 // Number of steps for full pulse cycle
        const val GRID_SIZE = GlyphMatrixUtils.MATRIX_SIZE
        const val PI = 3.14159265f
    }

    private var foodPulsePhase = 0

    public fun render(game: SnakeGame): GlyphMatrixFrame {
        val matrix = IntArray(GRID_SIZE * GRID_SIZE) { 0 }

        // Update pulse phase (smooth sine wave)
        foodPulsePhase = (foodPulsePhase + 1) % FOOD_PULSE_STEPS

        // Calculate smooth brightness using sine wave (0 to 1 range)
        val pulseValue = (sin(foodPulsePhase * 2 * PI / FOOD_PULSE_STEPS) + 1) / 2
        val foodBrightness = FOOD_BRIGHTNESS_LOW + ((FOOD_BRIGHTNESS_HIGH - FOOD_BRIGHTNESS_LOW) * pulseValue).toInt()
        setPixel(matrix, game.food.x, game.food.y, foodBrightness)

        // Render snake body (tail to head, so head overwrites if overlapping)
        val segments = game.segments
        for (i in segments.indices.reversed()) {
            val segment = segments[i]
            val brightness = if (i == 0) HEAD_BRIGHTNESS else BODY_BRIGHTNESS
            setPixel(matrix, segment.x, segment.y, brightness)
        }

        logMatrix(matrix)

        return GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .build(context)
    }

    public fun renderGameOver(game: SnakeGame): GlyphMatrixFrame {
        val matrix = IntArray(GRID_SIZE * GRID_SIZE) { 0 }

        // Render snake dimmed
        for ((index, segment) in game.segments.withIndex()) {
            val brightness = if (index == 0) 1500 else 800
            setPixel(matrix, segment.x, segment.y, brightness)
        }

        // "GO?" text overlay
        val textObject = GlyphMatrixObject.Builder()
            .setText("GO")
            .setBrightness(4095)
            .setPosition(8, 9)
            .build()

        return GlyphMatrixFrame.Builder()
            .addLow(matrix)      // snake on lower layer
            .addTop(textObject)  // text on top
            .build(context)
    }

    public fun renderStartScreen(game: SnakeGame): GlyphMatrixFrame {
        val matrix = IntArray(GRID_SIZE * GRID_SIZE) { 0 }

        // Render food (dimmed)
        setPixel(matrix, game.food.x, game.food.y, FOOD_BRIGHTNESS_LOW)

        // Render snake (dimmed)
        for ((index, segment) in game.segments.withIndex()) {
            val brightness = if (index == 0) HEAD_BRIGHTNESS else BODY_BRIGHTNESS
            setPixel(matrix, segment.x, segment.y, brightness)
        }

        val textObject = GlyphMatrixObject.Builder()
            .setText("GO")
            .setBrightness(2095)
            .setPosition(8, 9)
            .build()

        logMatrix(matrix)

        return GlyphMatrixFrame.Builder()
            .addLow(textObject)
            .addTop(matrix)
            .build(context)
    }

    public fun renderEmpty(): GlyphMatrixFrame {
        val matrix = IntArray(GRID_SIZE * GRID_SIZE) { 0 }
        return GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .build(context)
    }

    public fun reset() {
        foodPulsePhase = 0
    }

    private fun setPixel(matrix: IntArray, x: Int, y: Int, brightness: Int) {
        if (x in 0 until GRID_SIZE && y in 0 until GRID_SIZE) {
            matrix[y * GRID_SIZE + x] = brightness
        }
    }

    private fun logMatrix(matrix: IntArray) {
        Log.d(TAG, matrix.joinToString(","))
    }
}
