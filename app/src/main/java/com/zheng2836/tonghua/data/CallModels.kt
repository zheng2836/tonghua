package com.zheng2836.tonghua.data

enum class CallDirection {
    INCOMING,
    OUTGOING
}

enum class CallState {
    IDLE,
    INVITING,
    RINGING,
    CONNECTING,
    ACTIVE,
    ENDED,
    REJECTED,
    FAILED
}

data class CallSession(
    val callId: String,
    val peerId: String,
    val peerDisplayName: String,
    val direction: CallDirection,
    var state: CallState = CallState.IDLE
)
