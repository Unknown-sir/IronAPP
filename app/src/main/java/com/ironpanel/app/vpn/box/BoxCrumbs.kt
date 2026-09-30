package com.ironpanel.app.vpn.box

import android.content.Context
import java.io.File

/**
 * Breadcrumb trail for the native core lifecycle. If the process dies
 * (including Go panics, which bypass Java handlers) mid-start, the next
 * launch can tell the user — and us — exactly which stage died.
 */
object BoxCrumbs {
    private const val DIR = "boxboot"
    private const val FILE = "stages.txt"
    private const val CLEAN = "90-clean-stop"

    private fun file(context: Context): File {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        return File(dir, FILE)
    }

    @Synchronized
    fun mark(context: Context, stage: String) {
        try {
            file(context).appendText(stage + "\n")
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun markCleanStop(context: Context) {
        try {
            file(context).appendText("$CLEAN\n")
        } catch (_: Exception) {
        }
    }

    /** Last stage of the previous run, or null when it stopped cleanly. */
    @Synchronized
    fun lastUncleanStage(context: Context): String? {
        return try {
            val f = file(context)
            if (!f.exists()) return null
            val lines = f.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty() || lines.last() == CLEAN) return null
            lines.last()
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun reset(context: Context) {
        try {
            file(context).delete()
        } catch (_: Exception) {
        }
    }
}
