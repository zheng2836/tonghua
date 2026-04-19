package com.zheng2836.tonghua.telecom

import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle

class ManagedVoipConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        val extras = request.extras ?: Bundle.EMPTY
        val callId = extras.getString(TelecomFacade.EXTRA_CALL_ID)
            ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, "missing callId")
            )

        val peerId = request.address?.schemeSpecificPart
            ?: extras.getString(TelecomFacade.EXTRA_PEER_ID).orEmpty()
        val peerName = extras.getString(TelecomFacade.EXTRA_PEER_NAME) ?: peerId

        return ManagedVoipConnection(callId, peerId, peerName, isIncoming = false).apply {
            setInitializing()
            setDialing()
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        val extras = request.extras ?: Bundle.EMPTY
        val callId = extras.getString(TelecomFacade.EXTRA_CALL_ID)
            ?: return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, "missing callId")
            )

        val peerId = request.address?.schemeSpecificPart
            ?: extras.getString(TelecomFacade.EXTRA_PEER_ID).orEmpty()
        val peerName = extras.getString(TelecomFacade.EXTRA_PEER_NAME) ?: peerId

        return ManagedVoipConnection(callId, peerId, peerName, isIncoming = true).apply {
            setInitializing()
            setRinging()
        }
    }
}
