package com.zheng2836.tonghua

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
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
