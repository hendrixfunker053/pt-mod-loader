package com.example.ptloader

import android.content.Context
import android.net.Uri
import android.system.Os
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File

class ModManager(private val ctx: Context) {
    val baseDir = File(ctx.filesDir, "base")
    val modsDir = File(ctx.filesDir, "mods").apply { mkdirs() }
    private val cacheRoot = File(ctx.filesDir, "cache")

    fun hasBase() = File(baseDir, "data.win").exists()

    /** Copia el juego una sola vez al almacenamiento privado (SAF no admite symlinks). */
    fun importBase(tree: Uri, onProgress: (String) -> Unit = {}) {
        baseDir.deleteRecursively(); baseDir.mkdirs()
        copyTree(DocumentFile.fromTreeUri(ctx, tree) ?: return, baseDir, onProgress)
    }

    private fun copyTree(src: DocumentFile, dst: File, onProgress: (String) -> Unit) {
        for (f in src.listFiles()) {
            val name = f.name ?: continue
            val target = File(dst, name)
            if (f.isDirectory) { target.mkdirs(); copyTree(f, target, onProgress) }
            else {
                onProgress(name)
                ctx.contentResolver.openInputStream(f.uri)?.use { i ->
                    target.outputStream().use { o -> i.copyTo(o) }
                }
            }
        }
    }

    fun listMods(): List<ModInfo> =
        modsDir.listFiles { f -> f.isDirectory }?.mapNotNull { dir ->
            val j = File(dir, "info.json").takeIf { it.exists() }?.let { JSONObject(it.readText()) }
            ModInfo(
                id = dir.name,
                name = j?.optString("name", dir.name) ?: dir.name,
                author = j?.optString("author", "") ?: "",
                patchFile = j?.optString("patch")?.takeIf { it.isNotEmpty() },
                dir = dir,
            )
        }?.sortedBy { it.name } ?: emptyList()

    /** Crea cache/<id> con symlinks al juego base y encima aplica el mod. */
    fun prepareWorkDir(mod: ModInfo): File {
        val work = File(cacheRoot, mod.id)
        work.deleteRecursively(); work.mkdirs()

        baseDir.listFiles()?.forEach {
            Os.symlink(it.absolutePath, File(work, it.name).absolutePath)
        }

        mod.patchFile?.let { p ->
            val out = File(work, "data.win")
            out.delete() // quitar el symlink
            XdeltaPatcher.apply(File(baseDir, "data.win"), File(mod.dir, p), out)
        }

        File(mod.dir, "files").takeIf { it.isDirectory }?.let { overlay(it, work) }
        return work
    }

    private fun overlay(src: File, dst: File) {
        src.listFiles()?.forEach { f ->
            val t = File(dst, f.name)
            if (f.isDirectory) {
                if (!t.isDirectory) { t.delete(); t.mkdirs() }
                overlay(f, t)
            } else { t.delete(); f.copyTo(t) }
        }
    }
}
