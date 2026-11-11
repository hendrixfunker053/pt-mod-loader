package com.example.ptloader

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.TextView
import java.io.File

class GameActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Ui.fullscreen(this)

        val gameDir = File(intent.getStringExtra("gameDir") ?: return finish())
        val controls = File(intent.getStringExtra("controls") ?: "")
        val layout = ControlsConfig.forPreset(intent.getIntExtra("preset", 0), controls)
        val lowRes = intent.getBooleanExtra("lowRes", true) // lo usará el runner

        val root = FrameLayout(this)
        // TODO: aquí va la superficie/runner de GameMaker cargando gameDir/data.win (lowRes = resolución rápida)
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
