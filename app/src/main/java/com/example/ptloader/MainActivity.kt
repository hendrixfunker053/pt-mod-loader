package com.example.ptloader

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var gm: GameManager
    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private lateinit var gameLabel: TextView
    private lateinit var gamesPill: Button
    private lateinit var optionsBtn: Button
    private lateinit var ctrlBtn: Button
    private lateinit var resBtn: Button
    private lateinit var playBtn: Button
    private var target: GameManager.Game? = null

    private val presets = arrayOf("Pizza (joystick + botones)", "Solo botones", "Sin controles (mando)", "Personalizado")
    private val resolutions = arrayOf("rápida (más FPS)", "nativa (más nítida)")
    private val zipTypes = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

    private val addTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { work("Copiando juego...") { gm.addFromTree(it, ::progress).name } }
    }
    private val addZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { work("Extrayendo juego...") { gm.addFromZip(it, ::progress).name } }
    }
    private val modTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val g = target
        if (uri != null && g != null) work("Aplicando mod...") { gm.applyTree(g, uri, ::progress); null }
    }
    private val modZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val g = target
        if (uri != null && g != null) work("Aplicando mod...") { gm.applyZip(g, uri, ::progress); null }
    }

    private fun progress(name: String) = runOnUiThread { status.text = name }

    /** Trabajo pesado en segundo plano; si devuelve un nombre, ese juego queda seleccionado. */
    private fun work(msg: String, block: () -> String?) {
        status.text = msg
        thread {
            try {
                val sel = block()
                runOnUiThread { sel?.let { prefs.edit().putString("game", it).apply() }; refresh("Listo") }
            } catch (e: Throwable) {
                runOnUiThread { refresh("Error: ${e.message}") }
            }
        }
    }

    private fun selected(): GameManager.Game? {
        val all = gm.list()
        return all.firstOrNull { it.name == prefs.getString("game", null) } ?: all.firstOrNull()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Ui.fullscreen(this)
        gm = GameManager(this)
        prefs = Prefs.get(this)
        val c = this

        // Fondo: pon tu imagen en res/drawable-nodpi/bg_launcher.png (opcional)
        val bg = FrameLayout(this).apply { setBackgroundColor(0xFF2B4A5E.toInt()) }
        val bgId = resources.getIdentifier("bg_launcher", "drawable", packageName)
        if (bgId != 0) {
            bg.addView(ImageView(this).apply { setImageResource(bgId); scaleType = ImageView.ScaleType.CENTER_CROP })
            bg.addView(View(this).apply { setBackgroundColor(0x88000000.toInt()) })
        }

        // ----- Columna izquierda -----
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(Ui.card(c).apply { addView(Ui.text(c, "Launcher de mods de Pizza Tower", 20f, true)) })
        left.addView(Ui.card(c).apply {
            addView(Ui.text(c, "Añade tu copia de Pizza Tower (carpeta o .zip) y se abre aquí mismo, con controles táctiles."))
            addView(Ui.text(c, "El juego se queda en tu teléfono: no se sube a ningún sitio."), Ui.lp(c, top = 8))
        }, Ui.lp(c, top = 16))
        left.addView(Ui.card(c).apply {
            addView(Ui.text(c, "Para un mod: abre «Mod / opciones», duplica tu Pizza Tower y aplica el mod encima. Un fangame se añade como un juego más.", 12f))
            addView(Ui.text(c, "Mantén el original intacto: los mods se aplican solo a copias.", 12f), Ui.lp(c, top = 6))
        }, Ui.lp(c, top = 16))

        // ----- Panel derecho -----
        gamesPill = Ui.button(c, "Mis juegos") { showGames() }
        gameLabel = Ui.text(c, "")
        optionsBtn = Ui.button(c, "Mod / opciones") { selected()?.let { menu(it) } }
        ctrlBtn = Ui.button(c, "") { chooseControls() }
        resBtn = Ui.button(c, "") { chooseResolution() }
        playBtn = Ui.button(c, "Jugar") { play() }.apply { textSize = 20f }
        status = Ui.text(c, "", 12f)

        fun header(n: Int, title: String, extra: View? = null) = LinearLayout(c).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(Ui.step(c, n))
            addView(Ui.text(c, title, 17f, true), Ui.lp(c, w = 0, weight = 1f, start = 12))
            extra?.let { addView(it, Ui.lp(c, w = -2)) }
        }
        fun row(vararg v: View) = LinearLayout(c).apply {
            v.forEachIndexed { i, x -> addView(x, Ui.lp(c, w = 0, weight = 1f, start = if (i == 0) 0 else 10)) }
        }

        val panel = Ui.card(c).apply {
            addView(header(1, "Elige el juego", gamesPill))
            addView(gameLabel, Ui.lp(c, top = 8))
            addView(row(
                Ui.button(c, "Añadir juego") { addGameDialog() }, optionsBtn
            ), Ui.lp(c, top = 8))
            addView(header(2, "Controles"), Ui.lp(c, top = 18))
            addView(ctrlBtn, Ui.lp(c, top = 8))
            addView(row(resBtn, Ui.button(c, "Mover controles") { editControls() }), Ui.lp(c, top = 8))
            addView(header(3, "Jugar"), Ui.lp(c, top = 18))
            addView(playBtn, Ui.lp(c, top = 8))
            addView(status, Ui.lp(c, top = 8))
        }

        val pad = Ui.dp(c, 20)
        val content = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        content.addView(ScrollView(c).apply { addView(left) }, Ui.lp(c, w = 0, h = -1, weight = 1f))
        content.addView(ScrollView(c).apply { addView(panel) }, Ui.lp(c, w = 0, h = -1, weight = 1.15f, start = 16))
        content.setPadding(pad, pad, pad, pad)
        content.setOnApplyWindowInsetsListener { v, ins ->
            var l = 0; var r = 0
            if (Build.VERSION.SDK_INT >= 28) ins.displayCutout?.let { l = it.safeInsetLeft; r = it.safeInsetRight }
            v.setPadding(pad + l, pad, pad + r, pad)
            ins
        }
        bg.addView(content)
        setContentView(bg)
        refresh("Añade tu copia de Pizza Tower para empezar")
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh(status.text.toString()) }

    private fun refresh(msg: String) {
        val g = selected()
        status.text = msg
        gamesPill.text = "Mis juegos (${gm.list().size})"
        gameLabel.text = if (g == null) "Ningún juego añadido todavía" else "Juego: ${g.name}" + if (g.hasData) "" else "  (sin data.win)"
        ctrlBtn.text = "Controles: ${presets[prefs.getInt("preset", 0).coerceIn(0, 3)]}  ▾"
        resBtn.text = "Resolución: ${resolutions[prefs.getInt("res", 0).coerceIn(0, 1)]}  ▾"
        Ui.setEnabledStyle(optionsBtn, g != null)
        Ui.setEnabledStyle(playBtn, g?.hasData == true)
    }

    private fun addGameDialog() {
        AlertDialog.Builder(this).setTitle("Añadir juego").setItems(arrayOf("Elegir carpeta", "Elegir archivo .zip")) { _, i ->
            if (i == 0) addTree.launch(null) else addZip.launch(zipTypes)
        }.show()
    }

    private fun showGames() {
        val all = gm.list()
        if (all.isEmpty()) { status.text = "Añade un juego primero"; return }
        AlertDialog.Builder(this).setTitle("Mis juegos")
            .setItems(all.map { it.name }.toTypedArray()) { _, i ->
                prefs.edit().putString("game", all[i].name).apply(); refresh("")
            }.show()
    }

    private fun chooseControls() {
        AlertDialog.Builder(this).setTitle("Controles")
            .setSingleChoiceItems(presets, prefs.getInt("preset", 0)) { d, i ->
                prefs.edit().putInt("preset", i).apply(); d.dismiss(); refresh("")
            }.show()
    }

    private fun chooseResolution() {
        AlertDialog.Builder(this).setTitle("Resolución")
            .setSingleChoiceItems(resolutions, prefs.getInt("res", 0)) { d, i ->
                prefs.edit().putInt("res", i).apply(); d.dismiss(); refresh("")
            }.show()
    }

    private fun editControls() {
        val g = selected() ?: run { status.text = "Elige un juego primero"; return }
        startActivity(Intent(this, ControlsEditorActivity::class.java)
            .putExtra("controls", g.controls.absolutePath).putExtra("preset", prefs.getInt("preset", 0)))
    }

    private fun play() {
        val g = selected() ?: return
        if (!g.hasData) { status.text = "Ese juego no tiene data.win"; return }
        startActivity(Intent(this, GameActivity::class.java)
            .putExtra("gameDir", g.dir.absolutePath).putExtra("controls", g.controls.absolutePath)
            .putExtra("preset", prefs.getInt("preset", 0)).putExtra("lowRes", prefs.getInt("res", 0) == 0))
    }

    private fun menu(g: GameManager.Game) {
        AlertDialog.Builder(this).setTitle(g.name).setItems(
            arrayOf("Duplicar", "Aplicar mod encima (carpeta)", "Aplicar mod encima (.zip)", "Borrar")
        ) { _, i ->
            target = g
            when (i) {
                0 -> askName("Nombre de la copia", "${g.name} - mod") { n -> work("Duplicando...") { gm.duplicate(g, n).name } }
                1 -> modTree.launch(null)
                2 -> modZip.launch(zipTypes)
                3 -> AlertDialog.Builder(this).setMessage("¿Borrar ${g.name}?")
                    .setPositiveButton("Borrar") { _, _ -> gm.delete(g); refresh("Borrado") }
                    .setNegativeButton("Cancelar", null).show()
            }
        }.show()
    }

    private fun askName(title: String, def: String, ok: (String) -> Unit) {
        val et = EditText(this).apply { setText(def) }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("OK") { _, _ -> ok(et.text.toString()) }
            .setNegativeButton("Cancelar", null).show()
    }
}
