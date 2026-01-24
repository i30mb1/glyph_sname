package n7.snake.game

import android.graphics.Point
import android.util.Log

public class SnakeGame(
    private val gridWidth: Int = GRID_SIZE,
    private val gridHeight: Int = GRID_SIZE
) {
    public companion object {
        public const val GRID_SIZE: Int = 25
        private const val INITIAL_LENGTH = 4
        private const val BASE_SPEED_MS = 200L
        private const val MIN_SPEED_MS = 80L
        private const val SPEED_DECREASE_PER_FOOD = 5L

        // Visible x-range for each row y (based on physical Glyph Matrix display)
        // Data from user's measurements
        private val VISIBLE_BOUNDS = arrayOf(
            Pair(9, 15),   // y=0:  7 pixels
            Pair(7, 17),   // y=1:  11 pixels
            Pair(5, 19),   // y=2:  15 pixels
            Pair(4, 20),   // y=3:  17 pixels
            Pair(3, 21),   // y=4:  19 pixels
            Pair(2, 22),   // y=5:  21 pixels
            Pair(2, 22),   // y=6:  21 pixels
            Pair(1, 23),   // y=7:  23 pixels
            Pair(1, 23),   // y=8:  23 pixels
            Pair(0, 24),   // y=9:  25 pixels (rows 9-15)
            Pair(0, 24),   // y=10: 25 pixels
            Pair(0, 24),   // y=11: 25 pixels
            Pair(0, 24),   // y=12: 25 pixels (center)
            Pair(0, 24),   // y=13: 25 pixels
            Pair(0, 24),   // y=14: 25 pixels
            Pair(0, 24),   // y=15: 25 pixels
            Pair(1, 23),   // y=16: 23 pixels
            Pair(1, 23),   // y=17: 23 pixels
            Pair(2, 22),   // y=18: 21 pixels
            Pair(2, 22),   // y=19: 21 pixels
            Pair(3, 21),   // y=20: 19 pixels
            Pair(4, 20),   // y=21: 17 pixels
            Pair(5, 19),   // y=22: 15 pixels
            Pair(7, 17),   // y=23: 11 pixels
            Pair(9, 15)    // y=24: 7 pixels
        )
    }

    private val _segments = mutableListOf<Point>()
    public val segments: List<Point> get() = _segments

    private var _direction: Direction = Direction.RIGHT
    public val direction: Direction get() = _direction

    // Direction applied in the last tick (for validating new directions)
    private var _committedDirection: Direction = Direction.RIGHT

    private var _food: Point = Point(0, 0)
    public val food: Point get() = _food

    private var _score: Int = 0
    public val score: Int get() = _score

    private var _isGameOver: Boolean = false
    public val isGameOver: Boolean get() = _isGameOver

    public var onGameOver: (() -> Unit)? = null
    public var onFoodEaten: (() -> Unit)? = null

    init {
        reset()
    }

    public fun reset() {
        _segments.clear()
        _direction = Direction.RIGHT
        _committedDirection = Direction.RIGHT
        _score = 0
        _isGameOver = false

        // Initialize snake above center (to not overlap with "GO" text)
        val startX = gridWidth / 2
        val startY = 5
        for (i in 0 until INITIAL_LENGTH) {
            _segments.add(Point(startX - i, startY))
        }

        spawnFood()

        Log.d("SnakeGame", "Game started. Snake at: ${_segments.map { "(${it.x},${it.y})" }}")
        Log.d("SnakeGame", "Food at: (${_food.x},${_food.y})")
    }

    public fun changeDirection(newDirection: Direction) {
        // Prevent 180-degree turns relative to COMMITTED direction (last tick)
        if (!newDirection.isOpposite(_committedDirection)) {
            _direction = newDirection
        }
    }

    public fun update() {
        if (_isGameOver) return

        val head = _segments.first()
        var newHead = when (_direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        // Commit the direction that was actually used this tick
        _committedDirection = _direction

        // Wrap position through visible area boundaries (teleportation)
        newHead = wrapPosition(newHead.x, newHead.y)
        Log.d("SnakeGame", "Head:(${head.x},${head.y})->${newHead.x},${newHead.y} Dir:$_direction")

        // Check self-collision (excluding tail which will move)
        val bodyWithoutTail = _segments.dropLast(1)
        val collision = bodyWithoutTail.find { it.x == newHead.x && it.y == newHead.y }
        if (collision != null) {
            val segmentIndex = bodyWithoutTail.indexOf(collision)
            Log.e("SnakeGame", "GAME OVER: Self-collision at (${newHead.x},${newHead.y})")
            Log.e("SnakeGame", "Head hit segment #$segmentIndex")
            Log.e("SnakeGame", "Snake segments: ${_segments.map { "(${it.x},${it.y})" }}")
            _isGameOver = true
            onGameOver?.invoke()
            return
        }

        // Move snake
        _segments.add(0, newHead)

        // Check if food eaten
        if (newHead.x == _food.x && newHead.y == _food.y) {
            _score++
            onFoodEaten?.invoke()
            spawnFood()
        } else {
            _segments.removeAt(_segments.size - 1)
        }
    }

    public fun getSpeedMs(): Long {
        val speedDecrease = _score * SPEED_DECREASE_PER_FOOD
        return (BASE_SPEED_MS - speedDecrease).coerceAtLeast(MIN_SPEED_MS)
    }

    private fun spawnFood() {
        val occupiedPositions = _segments.map { Pair(it.x, it.y) }.toSet()

        var newFood: Point
        do {
            newFood = Point(
                (0 until gridWidth).random(),
                (0 until gridHeight).random()
            )
        } while (
            Pair(newFood.x, newFood.y) in occupiedPositions ||
            !isInBounds(newFood.x, newFood.y)
        )

        _food = newFood
    }

    private fun isInBounds(x: Int, y: Int): Boolean {
        if (y !in 0 until GRID_SIZE) return false
        val (minX, maxX) = VISIBLE_BOUNDS[y]
        return x in minX..maxX
    }

    private fun wrapPosition(x: Int, y: Int): Point {
        val oldY = y.coerceIn(0, GRID_SIZE - 1)
        val (oldMinX, oldMaxX) = VISIBLE_BOUNDS[oldY]
        val oldWidth = oldMaxX - oldMinX + 1

        // Wrap y within 0..24
        val wrappedY = when {
            y < 0 -> GRID_SIZE - 1
            y >= GRID_SIZE -> 0
            else -> y
        }

        val (newMinX, newMaxX) = VISIBLE_BOUNDS[wrappedY]
        val newWidth = newMaxX - newMinX + 1

        // Calculate relative position (0.0 to 1.0) in old row and apply to new row
        val wrappedX = if (y != wrappedY) {
            val relativePos = (x - oldMinX).toFloat() / oldWidth
            (newMinX + (relativePos * newWidth).toInt()).coerceIn(newMinX, newMaxX)
        } else {
            // Same row - just wrap x within visible area
            when {
                x < newMinX -> newMaxX - (newMinX - x - 1) % newWidth
                x > newMaxX -> newMinX + (x - newMaxX - 1) % newWidth
                else -> x
            }
        }

        if (x != wrappedX || y != wrappedY) {
            Log.d("SnakeGame", "WRAP: ($x,$y) -> ($wrappedX,$wrappedY)")
        }

        return Point(wrappedX, wrappedY)
    }
}
