package com.zheng2836.tonghua.webrtc

import android.content.Context
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient

class WebRtcEngine(
    context: Context,
    private val callStore: CallStore
) {
    @Volatile
    var iceState: String = "new"
        private set

    @Volatile
    var lastSignal: String = "none"
        private set

    private var signalingClient: SignalingClient? = null

    init {
        val unused = context.applicationContext
        if (unused == null) {
            iceState = "new"
        }
    }

    fun attachSignalingClient(client: SignalingClient) {
        signalingClient = client
    }

    fun answer(callId: String, callback: (Boolean) -> Unit) {
        val session = callStore.get(callId) ?: run {
            callback(false)
            return
        }
        lastSignal = "local_answer"
        iceState = "connected"
        callStore.updateState(callId, CallState.CONNECTING)
        signalingClient?.sendWebRtcAnswer(callId, session.peerId, "placeholder-answer-sdp")
        signalingClient?.sendWebRtcIce(callId, session.peerId, "placeholder-answer-ice")
        callback(true)
    }

    fun startOutgoing(callId: String, callback: (Boolean) -> Unit) {
        val session = callStore.get(callId) ?: run {
            callback(false)
            return
        }
        lastSignal = "local_offer"
        iceState = "checking"
        callStore.updateState(callId, CallState.CONNECTING)
        signalingClient?.sendWebRtcOffer(callId, session.peerId, "placeholder-offer-sdp")
        signalingClient?.sendWebRtcIce(callId, session.peerId, "placeholder-offer-ice")
        callback(true)
    }

    fun onRemoteOffer(callId: String, sdp: String) {
        if (sdp.isBlank()) return
        lastSignal = "remote_offer"
        iceState = "have_remote_offer"
        callStore.updateState(callId, CallState.CONNECTING)
    }

    fun onRemoteAnswer(callId: String, sdp: String) {
        if (sdp.isBlank()) return
        lastSignal = "remote_answer"
        iceState = "connected"
        callStore.updateState(callId, CallState.ACTIVE)
    }

    fun onRemoteIce(callId: String, candidate: String) {
        if (candidate.isBlank()) return
        lastSignal = "remote_ice"
        if (iceState == "new") {
            iceState = "checking"
        }
    }

    fun close(callId: String) {
        if (callId.isNotBlank()) {
            iceState = "closed"
            lastSignal = "closed"
        }
    }
}
