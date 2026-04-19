package com.zheng2836.tonghua.webrtc

import android.util.Log
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient

class WebRtcEngine(
    private val callStore: CallStore
) {
    @Volatile
    var iceState: String = "new"
        private set

    @Volatile
    var lastSignal: String = "none"
        private set

    private var signalingClient: SignalingClient? = null

    fun attachSignalingClient(client: SignalingClient) {
        signalingClient = client
    }

    fun answer(callId: String, callback: (Boolean) -> Unit) {
        Log.i("WebRTC", "answer $callId")
        val session = callStore.get(callId)
        iceState = "checking"
        lastSignal = "local_answer"
        callStore.updateState(callId, CallState.CONNECTING)
        session?.let {
            signalingClient?.sendWebRtcAnswer(
                callId = callId,
                targetUserId = it.peerId,
                sdp = "fake-answer-sdp-$callId"
            )
            signalingClient?.sendWebRtcIce(
                callId = callId,
                targetUserId = it.peerId,
                candidate = "fake-answer-ice-$callId"
            )
        }
        iceState = "connected"
        callback(true)
    }

    fun startOutgoing(callId: String, callback: (Boolean) -> Unit) {
        Log.i("WebRTC", "startOutgoing $callId")
        val session = callStore.get(callId)
        iceState = "checking"
        lastSignal = "local_offer"
        callStore.updateState(callId, CallState.CONNECTING)
        session?.let {
            signalingClient?.sendWebRtcOffer(
                callId = callId,
                targetUserId = it.peerId,
                sdp = "fake-offer-sdp-$callId"
            )
            signalingClient?.sendWebRtcIce(
                callId = callId,
                targetUserId = it.peerId,
                candidate = "fake-offer-ice-$callId"
            )
        }
        callback(true)
    }

    fun onRemoteOffer(callId: String, sdp: String) {
        Log.i("WebRTC", "onRemoteOffer $callId")
        lastSignal = "remote_offer"
        iceState = "have_remote_offer"
        callStore.updateState(callId, CallState.CONNECTING)
    }

    fun onRemoteAnswer(callId: String, sdp: String) {
        Log.i("WebRTC", "onRemoteAnswer $callId")
        lastSignal = "remote_answer"
        iceState = "connected"
        callStore.updateState(callId, CallState.ACTIVE)
    }

    fun onRemoteIce(callId: String, candidate: String) {
        Log.i("WebRTC", "onRemoteIce $callId")
        lastSignal = "remote_ice"
        if (iceState == "new") {
            iceState = "checking"
        }
    }

    fun close(callId: String) {
        Log.i("WebRTC", "close $callId")
        iceState = "closed"
        lastSignal = "closed"
    }
}
