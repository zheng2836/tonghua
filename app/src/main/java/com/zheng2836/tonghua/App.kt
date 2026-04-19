package com.zheng2836.tonghua

import android.app.Application
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        AppGraph.signalingClient.connect()
        PhoneAccountRegistrar.registerIfNeeded(this)
    }
}
