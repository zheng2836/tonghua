package com.zheng2836.tonghua

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
