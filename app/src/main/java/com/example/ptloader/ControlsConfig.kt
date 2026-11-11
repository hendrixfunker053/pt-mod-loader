package com.example.ptloader

import android.view.KeyEvent
import org.json.JSONObject
import java.io.File

/** Botón con sprite spr_<sprite>_button (2 frames: suelto | pulsado). x/y = fracción de pantalla, size = alto como fracción del alto de pantalla. */
data class TouchButton(val sprite: String, val keyCode: Int, val x: Float, val y: Float, val size: Float)

data class Joystick(
    val x: Float, val y: Float, val size: Float, val deadzone: Float,
    val left: Int, val right: Int, val up: Int, val down: Int,
)

data class ControlsLayout(val joystick: Joystick?, val buttons: List<TouchButton>)

object ControlsConfig {
    // Teclas por defecto: verifícalas en el menú de opciones del juego.
    fun default() = ControlsLayout(
        Joystick(
            0.15f, 0.68f, 0.55f, 0.25f,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        ),
        listOf(
            TouchButton("z", KeyEvent.KEYCODE_Z, 0.90f, 0.80f, 0.22f),
            TouchButton("x", KeyEvent.KEYCODE_X, 0.77f, 0.86f, 0.22f),
            TouchButton("c", KeyEvent.KEYCODE_C, 0.77f, 0.64f, 0.22f),
            TouchButton("shift", KeyEvent.KEYCODE_SHIFT_LEFT, 0.90f, 0.56f, 0.22f),
            TouchButton("pause", KeyEvent.KEYCODE_ESCAPE, 0.50f, 0.10f, 0.14f),
            TouchButton("debug", KeyEvent.KEYCODE_F1, 0.62f, 0.10f, 0.14f),
        ),
    )

    private fun key(s: String) = KeyEvent.keyCodeFromString(s)

    /**
     * controls.json:
     * {
     *   "joystick": {"x":0.15,"y":0.68,"size":0.55,"deadzone":0.25,
     *                "left":"KEYCODE_DPAD_LEFT","right":"...","up":"...","down":"..."},   // o null para ocultarlo
     *   "buttons": [{"sprite":"z","key":"KEYCODE_Z","x":0.9,"y":0.8,"size":0.22}, ...]
     * }
     * sprites: z, x, c, shift, pause, debug
     */
    fun load(file: File): ControlsLayout {
        val def = default()
        if (!file.exists()) return def
        val o = JSONObject(file.readText())

        val joy = if (!o.has("joystick")) def.joystick
        else if (o.isNull("joystick")) null
        else o.getJSONObject("joystick").let { j ->
            val d = def.joystick!!
            Joystick(
                j.optDouble("x", d.x.toDouble()).toFloat(),
                j.optDouble("y", d.y.toDouble()).toFloat(),
                j.optDouble("size", d.size.toDouble()).toFloat(),
                j.optDouble("deadzone", d.deadzone.toDouble()).toFloat(),
                j.optString("left").takeIf { it.isNotEmpty() }?.let(::key) ?: d.left,
                j.optString("right").takeIf { it.isNotEmpty() }?.let(::key) ?: d.right,
                j.optString("up").takeIf { it.isNotEmpty() }?.let(::key) ?: d.up,
                j.optString("down").takeIf { it.isNotEmpty() }?.let(::key) ?: d.down,
            )
        }

        val buttons = o.optJSONArray("buttons")?.let { arr ->
            (0 until arr.length()).mapNotNull {
                val b = arr.getJSONObject(it)
                val k = key(b.getString("key"))
                if (k == KeyEvent.KEYCODE_UNKNOWN) null
                else TouchButton(
                    b.getString("sprite"), k,
                    b.getDouble("x").toFloat(), b.getDouble("y").toFloat(),
                    b.optDouble("size", 0.22).toFloat(),
                )
            }
        } ?: def.buttons

        return ControlsLayout(joy, buttons)
    }
}
