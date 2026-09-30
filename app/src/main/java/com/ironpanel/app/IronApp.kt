package com.ironpanel.app

import android.app.Application
import com.ironpanel.app.vpn.box.BoxSetup

class IronApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crash handler first, so even core setup failures are recorded.
        try {
            CrashReporter.install(this)
        } catch (_: Exception) {
        }
        // One-time sing-box core setup (paths + crash log); cheap file ops.
        try {
            BoxSetup.ensure(this, BuildConfig.VERSION_NAME)
        } catch (_: Exception) {
        }
    }
}
