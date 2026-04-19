package com.zheng2836.tonghua.telecom

import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import com.zheng2836.tonghua.AppGraph
import com.zheng2836.tonghua.data.CallDirection
import com.zheng2836.tonghua.data.CallSession
import com.zheng2836.tonghua.data.CallState

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

        val session = CallSession(
            callId = callId,
            peerId = peerId,
            peerDisplayName = peerName,
            direction = CallDirection.OUTGOING,
            state = CallState.INVITING
        )
        AppGraph.callStore.put(session)

        val connection = ManagedVoipConnection(callId, peerId, peerName, isIncoming = false).apply {
            setInitializing()
            setDialing()
        }
        AppGraph.connectionRegistry.put(callId, connection)
        AppGraph.signalingClient.startOutgoingInvite(session)
        return connection
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

        val session = CallSession(
            callId = callId,
            peerId = peerId,
            peerDisplayName = peerName,
            direction = CallDirection.INCOMING,
            state = CallState.RINGING
        )
        AppGraph.callStore.put(session)

        val connection = ManagedVoipConnection(callId, peerId, peerName, isIncoming = true).apply {
            setInitializing()
            setRinging()
        }
        AppGraph.connectionRegistry.put(callId, connection)
        AppGraph.signalingClient.markIncomingRinging(callId)
        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        request.extras?.getString(TelecomFacade.EXTRA_CALL_ID)?.let {
            AppGraph.signalingClient.reportCreateConnectionFailed(it, "outgoing_failed")
        }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        request.extras?.getString(TelecomFacade.EXTRA_CALL_ID)?.let {
            AppGraph.signalingClient.reportCreateConnectionFailed(it, "incoming_failed")
        }
    }
}
