package com.example.ptloader

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var gm: GameManager
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var target: GameManager.Game? = null

    private val zipTypes = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

    private val addTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { work("Copiando juego...") { gm.addFromTree(it, ::progress) } }
    }
    private val addZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { work("Extrayendo juego...") { gm.addFromZip(it, ::progress) } }
    }
    private val modTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val g = target; if (uri != null && g != null) work("Aplicando mod...") { gm.applyTree(g, uri, ::progress) }
    }
    private val modZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val g = target; if (uri != null && g != null) work("Aplicando mod...") { gm.applyZip(g, uri, ::progress) }
    }

    private fun progress(name: String) = runOnUiThread { status.text = name }

    private fun work(msg: String, block: () -> Unit) {
        status.text = msg
        thread {
            try { block(); runOnUiThread { refresh("Listo") } }
            catch (e: Throwable) { runOnUiThread { refresh("Error: ${e.message}") } }
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        gm = GameManager(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        status = TextView(this)
        root.addView(Button(this).apply { text = "Añadir juego (carpeta)"; setOnClickListener { addTree.launch(null) } })
        root.addView(Button(this).apply { text = "Añadir juego (.zip)"; setOnClickListener { addZip.launch(zipTypes) } })
        root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        refresh("Añade tu copia de Pizza Tower para empezar")
    }

    private fun refresh(msg: String) {
        status.text = msg
        list.removeAllViews()
        gm.list().forEach { g ->
            list.addView(Button(this).apply {
                text = g.name + if (g.hasData) "" else "  (sin data.win)"
                setOnClickListener { play(g) }
                setOnLongClickListener { menu(g); true }
            })
        }
    }

    private fun play(g: GameManager.Game) {
        if (!g.hasData) { status.text = "Ese juego no tiene data.win"; return }
        startActivity(Intent(this, GameActivity::class.java)
            .putExtra("gameDir", g.dir.absolutePath).putExtra("controls", g.controls.absolutePath))
    }

    /** Mantén pulsado un juego para ver estas opciones. */
    private fun menu(g: GameManager.Game) {
        AlertDialog.Builder(this).setTitle(g.name).setItems(
            arrayOf("Duplicar", "Aplicar mod encima (carpeta)", "Aplicar mod encima (.zip)", "Borrar")
        ) { _, i ->
            target = g
            when (i) {
                0 -> askName("Nombre de la copia", "${g.name} - mod") { n -> work("Duplicando...") { gm.duplicate(g, n) } }
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
