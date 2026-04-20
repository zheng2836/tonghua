package com.zheng2836.tonghua.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zheng2836.tonghua.AppGraph
import com.zheng2836.tonghua.data.CallDirection
import com.zheng2836.tonghua.data.CallSession
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar
import com.zheng2836.tonghua.telecom.TelecomFacade

class AppFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.i("FCM", "new token=$token")
        AppGraph.signalingClient.registerFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "incoming_call") return

        val callId = data["callId"] ?: return
        val callerId = data["callerId"] ?: return
        val callerName = data["callerName"] ?: callerId

        AppGraph.signalingClient.connect()
        PhoneAccountRegistrar.registerIfNeeded(this)

        AppGraph.callStore.put(
            CallSession(
                callId = callId,
                peerId = callerId,
                peerDisplayName = callerName,
                direction = CallDirection.INCOMING,
                state = CallState.RINGING
            )
        )

        if (!PhoneAccountRegistrar.isEnabled(this)) {
            Log.e("FCM", "phone account not enabled")
            AppGraph.signalingClient.reportIncomingEscalationFailed(callId, "phone_account_not_enabled")
            return
        }

        val ok = TelecomFacade.addIncomingCall(this, callId, callerId, callerName)
        if (!ok) {
            Log.e("FCM", "add incoming call failed")
            AppGraph.signalingClient.reportIncomingEscalationFailed(callId, "add_incoming_call_failed")
        }
    }
}
