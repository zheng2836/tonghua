package com.zheng2836.tonghua.webrtc

import android.content.Context
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient

class WebRtcEngine(
    context: Context,
    callStore: CallStore
) {
    private val core = RealRtcCore(context, callStore)

    @Volatile
    var iceState: String = "new"
        private set

    @Volatile
    var lastSignal: String = "none"
        private set

    fun attachSignalingClient(client: SignalingClient) {
        core.attach(client)
        syncState()
    }

    fun answer(callId: String, callback: (Boolean) -> Unit) {
        core.answer(callId, callback)
        syncState()
    }

    fun startOutgoing(callId: String, callback: (Boolean) -> Unit) {
        core.startOutgoing(callId, callback)
        syncState()
    }

    fun onRemoteOffer(callId: String, sdp: String) {
        core.onRemoteOffer(callId, sdp)
        syncState()
    }

    fun onRemoteAnswer(callId: String, sdp: String) {
        core.onRemoteAnswer(callId, sdp)
        syncState()
    }

    fun onRemoteIce(callId: String, candidate: String) {
        core.onRemoteIce(callId, candidate)
        syncState()
    }

    fun close(callId: String) {
        core.close(callId)
        syncState()
    }

    private fun syncState() {
        iceState = core.iceState
        lastSignal = core.lastSignal
    }
}
