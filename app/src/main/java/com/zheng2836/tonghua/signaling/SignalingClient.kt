package com.zheng2836.tonghua.signaling

import android.content.Context
import android.util.Log
import com.zheng2836.tonghua.data.CallSession
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.telecom.ConnectionRegistry

class SignalingClient(
    private val context: Context,
    private val callStore: CallStore,
    private val connectionRegistry: ConnectionRegistry
) {
    @Volatile
    var connectionState: String = "idle"
        private set

    fun connect() {
        connectionState = "connecting"
        Log.i("Signal", "connect")
        connectionState = "connected"
    }

    fun registerFcmToken(token: String) {
        Log.i("Signal", "registerFcmToken=$token")
    }

    fun startOutgoingInvite(session: CallSession) {
        callStore.updateState(session.callId, CallState.INVITING)
        Log.i("Signal", "call.invite ${session.callId}")
    }

    fun markIncomingRinging(callId: String) {
        callStore.updateState(callId, CallState.RINGING)
        Log.i("Signal", "call.ringing $callId")
    }

    fun answer(callId: String) {
        callStore.updateState(callId, CallState.CONNECTING)
        Log.i("Signal", "call.answer $callId")
    }

    fun reject(callId: String) {
        callStore.updateState(callId, CallState.REJECTED)
        Log.i("Signal", "call.reject $callId")
    }

    fun hangup(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        Log.i("Signal", "call.hangup $callId")
    }

    fun cancel(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        Log.i("Signal", "call.cancel $callId")
    }

    fun hold(callId: String) {
        Log.i("Signal", "call.hold $callId")
    }

    fun unhold(callId: String) {
        Log.i("Signal", "call.unhold $callId")
    }

    fun onRemoteAnswered(callId: String) {
        callStore.updateState(callId, CallState.ACTIVE)
        connectionRegistry.get(callId)?.onRemoteAnswered()
    }

    fun onRemoteRejected(callId: String) {
        callStore.updateState(callId, CallState.REJECTED)
        connectionRegistry.get(callId)?.onRemoteRejected()
    }

    fun onRemoteHangup(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        connectionRegistry.get(callId)?.onRemoteHangup()
    }

    fun reportCreateConnectionFailed(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e("Signal", "createConnectionFailed $callId $reason")
    }

    fun reportRuntimeFailure(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e("Signal", "runtimeFailure $callId $reason")
    }

    fun reportIncomingEscalationFailed(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e("Signal", "incomingEscalationFailed $callId $reason")
    }
}
