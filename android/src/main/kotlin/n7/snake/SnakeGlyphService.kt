package n7.snake

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

public class SnakeGlyphService : Service(), GlyphMatrixManager.Callback {

    private companion object {
        const val TAG = "SnakeGlyphService"
    }

    private lateinit var controller: SnakeController
    private lateinit var glyphManager: GlyphMatrixManager

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val event = msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                Log.d(TAG, "MSG_GLYPH_TOY event: $event")
                when (event) {
                    GlyphToy.EVENT_CHANGE -> {
                        Log.d(TAG, "Long press detected")
                        controller.onLongPress()
                    }
                    GlyphToy.EVENT_AOD -> Log.d(TAG, "AOD event")
                }
                true
            }
            else -> false
        }
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind()")
        init()
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind()")
        clear()
        return false
    }

    private fun init() {
        Log.d(TAG, "init()")
        glyphManager = GlyphMatrixManager.getInstance(this)
        glyphManager.init(this)

        controller = SnakeController(this)
        controller.setFrameCallback(object : SnakeController.FrameCallback {
            override fun onFrame(frame: GlyphMatrixFrame) {
                sendFrame(frame)
            }
        })
    }

    private fun clear() {
        Log.d(TAG, "clear()")
        controller.stop()
        glyphManager.turnOff()
        glyphManager.closeAppMatrix()
        glyphManager.unInit()
    }

    override fun onServiceConnected(componentName: ComponentName?) {
        Log.d(TAG, "onServiceConnected: $componentName")
        glyphManager.setGlyphMatrixTimeout(false)
        controller.start()
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected: $componentName")
    }

    private fun sendFrame(frame: GlyphMatrixFrame) {
        try {
            glyphManager.setMatrixFrame(frame)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending frame", e)
        }
    }
}
