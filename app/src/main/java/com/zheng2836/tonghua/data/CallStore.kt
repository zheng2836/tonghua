package com.zheng2836.tonghua.data

import java.util.concurrent.ConcurrentHashMap

class CallStore {
    private val sessions = ConcurrentHashMap<String, CallSession>()

    fun put(session: CallSession) {
        sessions[session.callId] = session
    }

    fun get(callId: String): CallSession? = sessions[callId]

    fun updateState(callId: String, state: CallState) {
        sessions[callId]?.state = state
    }

    fun remove(callId: String) {
        sessions.remove(callId)
    }
}
