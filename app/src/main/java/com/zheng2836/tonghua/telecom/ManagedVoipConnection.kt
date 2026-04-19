package com.zheng2836.tonghua.telecom

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.TelecomManager

class ManagedVoipConnection(
    private val callId: String,
    private val peerId: String,
    private val peerName: String,
    private val isIncoming: Boolean
) : Connection() {

    init {
        setAudioModeIsVoip(true)
        setAddress(PhoneAccountRegistrar.buildPeerUri(peerId), TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName(peerName, TelecomManager.PRESENTATION_ALLOWED)
        connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD or CAPABILITY_HOLD
    }

    override fun onAnswer() {
        setActive()
    }

    override fun onAnswer(videoState: Int) {
        setActive()
    }

    override fun onReject() {
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    override fun onHold() {
        setOnHold()
    }

    override fun onUnhold() {
        setActive()
    }
}
