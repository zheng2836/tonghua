package com.zheng2836.tonghua.data

data class DebugState(
    val signalingState: String,
    val iceState: String,
    val lastCallId: String?,
    val lastCallState: String?
)
