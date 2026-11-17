package n7.snake.game

import android.content.Context
import n7.glyph.ui.support.Supporter

/**
 * How the snake is drawn: the brightness of each segment, the head first. [CLASSIC] is everybody's; the others are the
 * supporters' extra. The game is the same in every skin.
 */
public enum class SnakeSkin(public val supporter: Boolean) {
    /** A bright head on a dim body. */
    CLASSIC(false),

    /** The body brightens from the tail to the head. */
    GRADIENT(true),

    /** Bright and dim segments by turns, like beads. */
    BEADS(true),

    /** A bright head with a short fading tail of light, the rest of the body faint. */
    COMET(true),
    ;

    /** Brightness 0..4095 of segment [index] (0 = the head) of a snake [length] segments long. */
    public fun level(index: Int, length: Int): Int = when (this) {
        CLASSIC -> if (index == 0) 209 else 100
        GRADIENT -> if (index == 0) HEAD else {
            val toward = 1f - index.toFloat() / length.coerceAtLeast(2)
            (TAIL + (BODY_TOP - TAIL) * toward).toInt()
        }
        BEADS -> if (index == 0) HEAD else if (index % 2 == 1) BEAD_ON else BEAD_OFF
        COMET -> when (index) {
            0 -> HEAD
            1 -> 2600
            2 -> 1600
            3 -> 900
            else -> FAINT
        }
    }

    public companion object {
        private const val HEAD = 4000
        private const val TAIL = 250
        private const val BODY_TOP = 2000
        private const val BEAD_ON = 1600
        private const val BEAD_OFF = 220
        private const val FAINT = 160

        private const val PREFS = "snake"
        private const val KEY_SKIN = "skin"

        /** The skin to draw: the one picked in the app, or [CLASSIC] if it is a supporters' skin and the purchase is gone. */
        public fun current(context: Context): SnakeSkin {
            val stored = stored(context)
            return if (stored.supporter && !Supporter.owned(context)) CLASSIC else stored
        }

        public fun stored(context: Context): SnakeSkin {
            val name = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SKIN, null)
            return entries.firstOrNull { it.name == name } ?: CLASSIC
        }

        public fun save(context: Context, skin: SnakeSkin) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SKIN, skin.name).apply()
        }
    }
}
