package com.ironpanel.app.vpn.box

import android.content.Context
import com.ironpanel.libbox.CommandServer
import com.ironpanel.libbox.CommandServerHandler
import com.ironpanel.libbox.Libbox
import com.ironpanel.libbox.OverrideOptions
import com.ironpanel.libbox.SetupOptions
import com.ironpanel.libbox.SystemProxyStatus

/** One-time core setup (mirrors the official client Application init). */
object BoxSetup {
    @Volatile
    private var ready = false

    @Synchronized
    fun ensure(context: Context, appVersion: String) {
        if (ready) return
        val base = context.filesDir
        val working = java.io.File(base, "box").apply { mkdirs() }
        val temp = context.cacheDir
        Libbox.setup(
            SetupOptions().apply {
                basePath = base.absolutePath
                workingPath = working.absolutePath
                tempPath = temp.absolutePath
                fixAndroidStack = true
                crashReportSource = "ironapp"
                this.appVersion = appVersion
                appMarketingVersion = appVersion
            }
        )
        ready = true
    }
}

/**
 * Owns one CommandServer session: start(config JSON) / stop().
 * Created inside the VpnService so TUN + protect + lifecycle stay together.
 *
 * Every stage is wrapped AND breadcrumb-marked: a Java failure becomes a
 * readable error (never a silent process death), and a native Go panic
 * leaves the exact stage behind for the next launch's crash dialog.
 */
class BoxHost(
    private val appContext: Context,
    private val platform: IronPlatformInterface,
) : CommandServerHandler {

    @Volatile
    private var server: CommandServer? = null

    @Synchronized
    fun start(configJson: String) {
        stop()
        BoxCrumbs.mark(appContext, "10-start-enter")
        val checked: String = try {
            // Fail fast on malformed configs before touching the TUN.
            Libbox.checkConfig(configJson)
            BoxCrumbs.mark(appContext, "11-check-ok")
            configJson
        } catch (e: Exception) {
            BoxCrumbs.mark(appContext, "11-check-fail")
            throw IllegalArgumentException("core rejected config: ${e.message}")
        }
        val commandServer: CommandServer = try {
            BoxCrumbs.mark(appContext, "20-server-create")
            CommandServer(this, platform)
        } catch (e: Exception) {
            BoxCrumbs.mark(appContext, "20-server-create-fail")
            throw IllegalStateException("core init failed: ${e.message}")
        }
        try {
            BoxCrumbs.mark(appContext, "21-server-start")
            commandServer.start()
            BoxCrumbs.mark(appContext, "22-reload-start")
            commandServer.startOrReloadService(checked, OverrideOptions())
            BoxCrumbs.mark(appContext, "23-reload-ok")
        } catch (e: Exception) {
            BoxCrumbs.mark(appContext, "22-reload-fail")
            try {
                commandServer.close()
            } catch (_: Exception) {
            }
            throw IllegalStateException("core start failed: ${e.message}")
        }
        server = commandServer
        BoxCrumbs.mark(appContext, "30-running")
    }

    @Synchronized
    fun stop() {
        val commandServer = server
        server = null
        if (commandServer != null) {
            try {
                commandServer.closeService()
            } catch (_: Exception) {
            }
            try {
                commandServer.close()
            } catch (_: Exception) {
            }
        }
        BoxCrumbs.markCleanStop(appContext)
    }

    // ---- CommandServerHandler (system proxy unused) ----

    override fun serviceStop() {
        stop()
        BoxTun.close()
    }

    override fun serviceReload() {
    }

    override fun getSystemProxyStatus(): SystemProxyStatus? = null

    override fun setSystemProxyEnabled(isEnabled: Boolean) {
    }

    override fun triggerNativeCrash() {
    }

    override fun writeDebugMessage(message: String?) {
    }

    override fun connectSSHAgent(): Int = -1
}
