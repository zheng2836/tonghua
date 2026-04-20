package com.zheng2836.tonghua

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        PhoneAccountRegistrar.registerIfNeeded(this)
        AppGraph.signalingClient.connect()
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        AppGraph.signalingClient.registerFcmToken(token)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("TongHua", "fetch fcm token failed", error)
                }
        }.onFailure {
            Log.e("TongHua", "init fcm token request failed", it)
        }
    }
}
