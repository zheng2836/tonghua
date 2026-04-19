package com.zheng2836.tonghua.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar
import com.zheng2836.tonghua.telecom.TelecomFacade

class AppFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.i("FCM", "new token=$token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "incoming_call") return

        val callId = data["callId"] ?: return
        val callerId = data["callerId"] ?: return
        val callerName = data["callerName"] ?: callerId

        if (!PhoneAccountRegistrar.isEnabled(this)) {
            Log.e("FCM", "phone account not enabled")
            return
        }

        TelecomFacade.addIncomingCall(this, callId, callerId, callerName)
    }
}
