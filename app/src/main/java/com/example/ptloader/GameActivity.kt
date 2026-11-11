package com.example.ptloader

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import java.io.File

class GameActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val gameDir = File(intent.getStringExtra("gameDir") ?: return finish())
        val layout = ControlsConfig.load(File(intent.getStringExtra("controls") ?: ""))

        val root = FrameLayout(this)
        // TODO: aquí va la superficie/runner de GameMaker cargando gameDir/data.win
        root.addView(TextView(this).apply { text = "Runner pendiente: ${gameDir.parentFile?.name}" })
        root.addView(ControlsOverlay(this, layout) { key, down -> sendKey(key, down) })
        setContentView(root)
    }

    private fun sendKey(code: Int, down: Boolean) {
        val ev = KeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
        // TODO: entregar `ev` al runner (dispatchKeyEvent sobre su vista).
        dispatchKeyEvent(ev)
    }
}
