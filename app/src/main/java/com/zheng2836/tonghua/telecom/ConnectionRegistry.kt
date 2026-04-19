package com.zheng2836.tonghua.telecom

import java.util.concurrent.ConcurrentHashMap

class ConnectionRegistry {
    private val connections = ConcurrentHashMap<String, ManagedVoipConnection>()

    fun put(callId: String, connection: ManagedVoipConnection) {
        connections[callId] = connection
    }

    fun get(callId: String): ManagedVoipConnection? = connections[callId]

    fun remove(callId: String) {
        connections.remove(callId)
    }
}
