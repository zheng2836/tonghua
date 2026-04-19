package com.zheng2836.tonghua.signaling

enum class SignalType {
    CALL_INVITE,
    CALL_RINGING,
    CALL_ANSWER,
    CALL_REJECT,
    CALL_CANCEL,
    CALL_HANGUP,
    WEBRTC_OFFER,
    WEBRTC_ANSWER,
    WEBRTC_ICE
}

data class SignalingMessage(
    val type: SignalType,
    val callId: String,
    val payload: Map<String, String> = emptyMap()
)
