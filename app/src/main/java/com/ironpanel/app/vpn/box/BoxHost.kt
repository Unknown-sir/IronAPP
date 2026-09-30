package com.ironpanel.app.vpn.box

import android.content.Context
import com.ironpanel.libbox.CommandServer
import com.ironpanel.libbox.CommandServerHandler
import com.ironpanel.libbox.Libbox
import com.ironpanel.libbox.OverrideOptions
import com.ironpanel.libbox.SetupOptions
import com.ironpanel.libbox.SystemProxyStatus
import java.io.File

/** One-time core setup (mirrors the official client Application init). */
object BoxSetup {
    @Volatile
    private var ready = false

    @Synchronized
    fun ensure(context: Context, appVersion: String) {
        if (ready) return
        val base = context.filesDir
        val working = File(base, "box").apply { mkdirs() }
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
 */
class BoxHost(
    private val platform: IronPlatformInterface,
) : CommandServerHandler {

    @Volatile
    private var server: CommandServer? = null

    @Synchronized
    fun start(configJson: String) {
        stop()
        // Fail fast on malformed configs before touching the TUN.
        try {
            Libbox.checkConfig(configJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("core rejected config: ${e.message}")
        }
        val commandServer = CommandServer(this, platform)
        commandServer.start()
        try {
            commandServer.startOrReloadService(configJson, OverrideOptions())
        } catch (e: Exception) {
            try {
                commandServer.close()
            } catch (_: Exception) {
            }
            throw e
        }
        server = commandServer
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
