package com.ironpanel.app

import android.content.Context
import com.ironpanel.app.vpn.box.BoxCrumbs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catches Java crashes (never native Go panics — those use [BoxCrumbs])
 * and persists them so the next launch can show/copy the report instead
 * of dying silently.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val stage = BoxCrumbs.lastUncleanStage(appContext) ?: "-"
                val text = buildString {
                    appendLine("IronAPP crash @ $stamp")
                    appendLine("core stage: $stage")
                    appendLine("thread: ${thread.name}")
                    appendLine(error.stackTraceToString())
                    var cause = error.cause
                    while (cause != null) {
                        appendLine("Caused by: " + cause.stackTraceToString())
                        cause = cause.cause
                    }
                }
                File(appContext.filesDir, FILE).writeText(text)
            } catch (_: Exception) {
            }
            try {
                previous?.uncaughtException(thread, error)
            } catch (_: Exception) {
            }
        }
    }

    /** Combined report (Java crash and/or unclean native stop), or null. */
    fun pendingReport(context: Context): String? {
        val parts = mutableListOf<String>()
        try {
            val crash = File(context.filesDir, FILE)
            if (crash.exists() && crash.length() > 0) {
                parts.add(crash.readText().take(4000))
            }
        } catch (_: Exception) {
        }
        val stage = BoxCrumbs.lastUncleanStage(context)
        if (stage != null && parts.isEmpty()) {
            parts.add(
                "The app died while the VPN core was starting " +
                    "(last core stage: $stage). No Java stack trace was recorded, " +
                    "which points at the native core — please send this to support."
            )
        }
        // Go runtime stderr (panics print here via Libbox.setup redirect).
        try {
            val goLog = File(File(context.filesDir, "box"), "CrashReport-ironapp.log")
            if (goLog.exists() && goLog.length() > 0) {
                val tail = goLog.readText().takeLast(3000)
                if (tail.isNotBlank()) {
                    parts.add("---- core log tail ----\n$tail")
                }
            }
        } catch (_: Exception) {
        }
        return if (parts.isEmpty()) null else parts.joinToString("\n\n")
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE).delete()
        } catch (_: Exception) {
        }
        BoxCrumbs.reset(context)
    }
}
