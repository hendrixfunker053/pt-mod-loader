package com.example.ptloader

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Cada juego es una carpeta normal, tal cual en PC:
 *   games/<nombre>/game/          ← los archivos del juego (data.win, audio, etc.)
 *   games/<nombre>/controls.json  ← (opcional) controles táctiles de ese juego
 *
 * Un "mod" = duplicar tu Pizza Tower y copiar los archivos del mod encima.
 * Un fangame = añadirlo como un juego más.
 */
class GameManager(private val ctx: Context) {
    data class Game(val name: String, val dir: File, val controls: File) {
        val hasData get() = File(dir, "data.win").exists()
    }

    private val root = File(ctx.filesDir, "games").apply { mkdirs() }

    fun list(): List<Game> =
        root.listFiles { f -> f.isDirectory }?.map { toGame(it) }?.sortedBy { it.name } ?: emptyList()

    private fun toGame(d: File) = Game(d.name, File(d, "game"), File(d, "controls.json"))

    private fun uniqueName(raw: String): String {
        val base = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Juego" }
        var n = base; var i = 2
        while (File(root, n).exists()) n = "$base ($i)".also { i++ }
        return n
    }

    // ---------- Añadir un juego (Pizza Tower, un fangame...) ----------

    fun addFromTree(tree: Uri, onProgress: (String) -> Unit = {}): Game {
        val src = DocumentFile.fromTreeUri(ctx, tree) ?: error("Carpeta no válida")
        val g = toGame(File(root, uniqueName(src.name ?: "Juego")))
        g.dir.mkdirs()
        copyTree(src, g.dir, onProgress)
        return g
    }

    fun addFromZip(uri: Uri, onProgress: (String) -> Unit = {}): Game {
        val name = (DocumentFile.fromSingleUri(ctx, uri)?.name ?: "Juego").removeSuffix(".zip")
        val g = toGame(File(root, uniqueName(name)))
        g.dir.mkdirs()
        extractZip(uri, g.dir, onProgress)
        return g
    }

    // ---------- Duplicar (copia tu Pizza Tower para asignarle un mod) ----------

    fun duplicate(src: Game, newName: String): Game {
        val g = toGame(File(root, uniqueName(newName)))
        src.dir.copyRecursively(g.dir, overwrite = true)
        if (src.controls.exists()) src.controls.copyTo(g.controls, overwrite = true)
        return g
    }

    // ---------- Aplicar un mod encima (como pegar y reemplazar en PC) ----------

    fun applyTree(g: Game, tree: Uri, onProgress: (String) -> Unit = {}) {
        copyTree(DocumentFile.fromTreeUri(ctx, tree) ?: error("Carpeta no válida"), g.dir, onProgress)
    }

    fun applyZip(g: Game, uri: Uri, onProgress: (String) -> Unit = {}) = extractZip(uri, g.dir, onProgress)

    fun delete(g: Game) { g.dir.parentFile?.deleteRecursively() }

    // ---------- Utilidades ----------

    private fun copyTree(src: DocumentFile, dst: File, onProgress: (String) -> Unit) {
        for (f in src.listFiles()) {
            val name = f.name ?: continue
            val target = File(dst, name)
            if (f.isDirectory) { target.mkdirs(); copyTree(f, target, onProgress) }
            else {
                onProgress(name)
                ctx.contentResolver.openInputStream(f.uri)?.use { i -> target.outputStream().use { o -> i.copyTo(o) } }
            }
        }
    }

    /** Extrae un zip sobre `into` (sobrescribe). Si todo viene dentro de una única carpeta, la quita. */
    private fun extractZip(uri: Uri, into: File, onProgress: (String) -> Unit) {
        val tmp = File(ctx.cacheDir, "zip_${System.nanoTime()}").apply { mkdirs() }
        try {
            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                ZipInputStream(ins).use { zin ->
                    var e = zin.nextEntry
                    while (e != null) {
                        val out = File(tmp, e.name)
                        if (!out.canonicalPath.startsWith(tmp.canonicalPath + File.separator))
                            throw SecurityException("Ruta inválida dentro del zip")
                        if (e.isDirectory) out.mkdirs()
                        else { out.parentFile?.mkdirs(); onProgress(e.name); out.outputStream().use { zin.copyTo(it) } }
                        e = zin.nextEntry
                    }
                }
            }
            val top = tmp.listFiles().orEmpty()
            val src = if (top.size == 1 && top[0].isDirectory) top[0] else tmp
            src.copyRecursively(into, overwrite = true)
        } finally { tmp.deleteRecursively() }
    }
}
