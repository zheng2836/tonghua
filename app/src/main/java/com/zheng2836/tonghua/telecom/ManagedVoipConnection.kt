package com.zheng2836.tonghua.telecom

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import com.zheng2836.tonghua.AppGraph
import com.zheng2836.tonghua.data.CallState

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
        setConnectionCapabilities(CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD or CAPABILITY_HOLD)
    }

    override fun onAnswer() {
        AppGraph.signalingClient.answer(callId)
        AppGraph.webRtcEngine.answer(callId) { ok ->
            if (ok) {
                AppGraph.callStore.updateState(callId, CallState.ACTIVE)
                setActive()
            } else {
                failAndDestroy("webrtc_answer_failed")
            }
        }
    }

    override fun onAnswer(videoState: Int) {
        onAnswer()
    }

    override fun onReject() {
        AppGraph.signalingClient.reject(callId)
        AppGraph.callStore.updateState(callId, CallState.REJECTED)
        cleanup(DisconnectCause(DisconnectCause.REJECTED))
    }

    override fun onDisconnect() {
        AppGraph.signalingClient.hangup(callId)
        AppGraph.callStore.updateState(callId, CallState.ENDED)
        cleanup(DisconnectCause(DisconnectCause.LOCAL))
    }

    override fun onAbort() {
        AppGraph.signalingClient.cancel(callId)
        AppGraph.callStore.updateState(callId, CallState.ENDED)
        cleanup(DisconnectCause(DisconnectCause.CANCELED))
    }

    override fun onHold() {
        AppGraph.signalingClient.hold(callId)
        setOnHold()
    }

    override fun onUnhold() {
        AppGraph.signalingClient.unhold(callId)
        setActive()
    }

    fun onRemoteAnswered() {
        AppGraph.webRtcEngine.startOutgoing(callId) { ok ->
            if (ok) {
                AppGraph.callStore.updateState(callId, CallState.CONNECTING)
            } else {
                failAndDestroy("webrtc_offer_failed")
            }
        }
    }

    fun onMediaConnected() {
        AppGraph.callStore.updateState(callId, CallState.ACTIVE)
        setActive()
    }

    fun onRemoteRejected() {
        AppGraph.callStore.updateState(callId, CallState.REJECTED)
        cleanup(DisconnectCause(DisconnectCause.REJECTED))
    }

    fun onRemoteHangup() {
        AppGraph.callStore.updateState(callId, CallState.ENDED)
        cleanup(DisconnectCause(DisconnectCause.REMOTE))
    }

    private fun failAndDestroy(reason: String) {
        AppGraph.signalingClient.reportRuntimeFailure(callId, reason)
        cleanup(DisconnectCause(DisconnectCause.ERROR, reason))
    }

    private fun cleanup(cause: DisconnectCause) {
        AppGraph.webRtcEngine.close(callId)
        AppGraph.connectionRegistry.remove(callId)
        setDisconnected(cause)
        destroy()
    }
}
