package com.zheng2836.tonghua.webrtc

import android.util.Log
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore

class WebRtcEngine(
    private val callStore: CallStore
) {
    @Volatile
    var iceState: String = "new"
        private set

    fun answer(callId: String, callback: (Boolean) -> Unit) {
        Log.i("WebRTC", "answer $callId")
        iceState = "checking"
        callStore.updateState(callId, CallState.CONNECTING)
        iceState = "connected"
        callback(true)
    }

    fun startOutgoing(callId: String, callback: (Boolean) -> Unit) {
        Log.i("WebRTC", "startOutgoing $callId")
        iceState = "checking"
        callStore.updateState(callId, CallState.CONNECTING)
        iceState = "connected"
        callback(true)
    }

    fun close(callId: String) {
        Log.i("WebRTC", "close $callId")
        iceState = "closed"
    }
}
