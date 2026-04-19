package com.zheng2836.tonghua

import android.content.Context
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient
import com.zheng2836.tonghua.telecom.ConnectionRegistry
import com.zheng2836.tonghua.webrtc.WebRtcEngine

object AppGraph {
    lateinit var appContext: Context
        private set

    lateinit var callStore: CallStore
        private set

    lateinit var connectionRegistry: ConnectionRegistry
        private set

    lateinit var signalingClient: SignalingClient
        private set

    lateinit var webRtcEngine: WebRtcEngine
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        callStore = CallStore()
        connectionRegistry = ConnectionRegistry()
        signalingClient = SignalingClient(appContext, callStore, connectionRegistry)
        webRtcEngine = WebRtcEngine(callStore)
        signalingClient.attachWebRtcEngine(webRtcEngine)
        webRtcEngine.attachSignalingClient(signalingClient)
    }
}
