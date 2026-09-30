package com.ironpanel.app

import android.app.Application
import com.ironpanel.app.vpn.box.BoxSetup

class IronApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // One-time sing-box core setup (paths + crash log); cheap file ops.
        try {
            BoxSetup.ensure(this, BuildConfig.VERSION_NAME)
        } catch (_: Exception) {
        }
    }
}
