package com.mehra.hide

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File

object FileHider {

    /** Folder me .nomedia file banate hi gallery use dikhana band kar deti hai */
    fun set(ctx: Context, path: String, hide: Boolean) {
        val dir = File(path)
        if (!dir.isDirectory) return
        val marker = File(dir, ".nomedia")
        try {
            if (hide) marker.createNewFile() else marker.delete()
        } catch (_: Exception) { }
        MediaScannerConnection.scanFile(ctx, arrayOf(dir.absolutePath), null, null)
    }

    fun setAll(ctx: Context, paths: Set<String>, hide: Boolean) =
        paths.forEach { set(ctx, it, hide) }

    fun candidates(): List<String> {
        val root = Environment.getExternalStorageDirectory()
        val out = mutableListOf<String>()
        listOf("DCIM", "Pictures", "Movies", "Download", "Documents").forEach { n ->
            val d = File(root, n)
            if (d.isDirectory) {
                out.add(d.absolutePath)
                d.listFiles()?.filter { it.isDirectory }?.forEach { out.add(it.absolutePath) }
            }
        }
        return out
    }
}
