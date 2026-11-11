package com.example.ptloader

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.io.File

/** "Mover controles": arrastra cada control, cambia su tamaño y guarda el layout en el juego. */
class ControlsEditorActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Ui.fullscreen(this)
        val file = File(intent.getStringExtra("controls") ?: return finish())
        val preset = intent.getIntExtra("preset", 0)
        val start = if (preset == 2) ControlsConfig.default() else ControlsConfig.forPreset(preset, file)

        val overlay = ControlsOverlay(this, start) { _, _ -> }.apply { editMode = true }
        val root = FrameLayout(this).apply { setBackgroundColor(0xFF1E1A1A.toInt()) }
        root.addView(Ui.text(this, "Arrastra para mover · toca uno y usa + / − para su tamaño", 13f).apply {
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@ControlsEditorActivity, 8) })
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun add(label: String, onClick: () -> Unit) =
            bar.addView(Ui.button(this, label, onClick), Ui.lp(this, w = -2, start = 8))
        add("−") { overlay.resizeSelected(-0.02f) }
        add("+") { overlay.resizeSelected(0.02f) }
        add("Restablecer") { overlay.setLayout(ControlsConfig.default()) }
        add("Cancelar") { finish() }
        add("Guardar") {
            ControlsConfig.save(file, overlay.current())
            Prefs.get(this).edit().putInt("preset", 3).apply()
            finish()
        }
        root.addView(bar, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            .apply { bottomMargin = Ui.dp(this@ControlsEditorActivity, 10) })
        setContentView(root)
    }
}
