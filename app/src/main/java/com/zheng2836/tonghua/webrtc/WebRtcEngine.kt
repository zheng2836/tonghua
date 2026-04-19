package com.zheng2836.tonghua.webrtc

import android.content.Context
import android.util.Log
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SdpObserver
import org.webrtc.IceCandidate
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap

class WebRtcEngine(
    context: Context,
    private val callStore: CallStore
) {
    @Volatile
    var iceState: String = "new"
        private set

    @Volatile
    var lastSignal: String = "none"
        private set

    private var signalingClient: SignalingClient? = null
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val peerFactory: PeerConnectionFactory
    private val localAudioSource: AudioSource

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        val audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        peerFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
        localAudioSource = peerFactory.createAudioSource(MediaConstraints())
    }

    fun attachSignalingClient(client: SignalingClient) {
        signalingClient = client
    }

    fun answer(callId: String, callback: (Boolean) -> Unit) {
        val session = callStore.get(callId) ?: run {
            callback(false)
            return
        }
        val pc = ensurePeerConnection(callId, session.peerId) ?: run {
            callback(false)
            return
        }
        lastSignal = "local_answer"
        callStore.updateState(callId, CallState.CONNECTING)
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) {
                    callback(false)
                    return
                }
                pc.setLocalDescription(noopSdpObserver(), desc)
                signalingClient?.sendWebRtcAnswer(callId, session.peerId, desc.description)
                callback(true)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { callback(false) }
            override fun onSetFailure(error: String?) {}
        }, audioOnlyConstraints())
    }

    fun startOutgoing(callId: String, callback: (Boolean) -> Unit) {
        val session = callStore.get(callId) ?: run {
            callback(false)
            return
        }
        val pc = ensurePeerConnection(callId, session.peerId) ?: run {
            callback(false)
            return
        }
        lastSignal = "local_offer"
        callStore.updateState(callId, CallState.CONNECTING)
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) {
                    callback(false)
                    return
                }
                pc.setLocalDescription(noopSdpObserver(), desc)
                signalingClient?.sendWebRtcOffer(callId, session.peerId, desc.description)
                callback(true)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { callback(false) }
            override fun onSetFailure(error: String?) {}
        }, audioOnlyConstraints())
    }

    fun onRemoteOffer(callId: String, sdp: String) {
        val session = callStore.get(callId) ?: return
        val pc = ensurePeerConnection(callId, session.peerId) ?: return
        lastSignal = "remote_offer"
        iceState = "have_remote_offer"
        callStore.updateState(callId, CallState.CONNECTING)
        pc.setRemoteDescription(noopSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    fun onRemoteAnswer(callId: String, sdp: String) {
        val session = callStore.get(callId) ?: return
        val pc = ensurePeerConnection(callId, session.peerId) ?: return
        lastSignal = "remote_answer"
        pc.setRemoteDescription(noopSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, sdp))
        iceState = "connected"
        callStore.updateState(callId, CallState.ACTIVE)
    }

    fun onRemoteIce(callId: String, candidate: String) {
        val session = callStore.get(callId) ?: return
        val pc = ensurePeerConnection(callId, session.peerId) ?: return
        lastSignal = "remote_ice"
        val ice = parseIceCandidate(candidate)
        pc.addIceCandidate(ice)
        if (iceState == "new") {
            iceState = "checking"
        }
    }

    fun close(callId: String) {
        peerConnections.remove(callId)?.close()
        iceState = "closed"
        lastSignal = "closed"
    }

    private fun ensurePeerConnection(callId: String, peerId: String): PeerConnection? {
        peerConnections[callId]?.let { return it }
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val pc = peerFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                iceState = state?.name ?: iceState
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) return
                signalingClient?.sendWebRtcIce(callId, peerId, serializeIceCandidate(candidate))
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) {}
        }) ?: return null
        val audioTrack: AudioTrack = peerFactory.createAudioTrack("audio_$callId", localAudioSource)
        pc.addTrack(audioTrack, listOf("stream_$callId"))
        peerConnections[callId] = pc
        return pc
    }

    private fun audioOnlyConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
    }

    private fun noopSdpObserver(): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }

    private fun serializeIceCandidate(candidate: IceCandidate): String {
        return JSONObject()
            .put("sdpMid", candidate.sdpMid)
            .put("sdpMLineIndex", candidate.sdpMLineIndex)
            .put("candidate", candidate.sdp)
            .toString()
    }

    private fun parseIceCandidate(raw: String): IceCandidate {
        return try {
            val json = JSONObject(raw)
            IceCandidate(
                json.optString("sdpMid"),
                json.optInt("sdpMLineIndex"),
                json.optString("candidate")
            )
        } catch (_: Exception) {
            IceCandidate("0", 0, raw)
        }
    }
}
