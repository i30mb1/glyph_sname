package n7.snake.game

public enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    public fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    public fun isOpposite(other: Direction): Boolean = this == other.opposite()
}
