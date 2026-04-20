package com.zheng2836.tonghua.webrtc

import android.content.Context
import android.util.Log
import com.zheng2836.tonghua.config.AppConfigRepository
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.signaling.SignalingClient
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class RealRtcCore(context: Context, private val store: CallStore) {
    @Volatile var iceState = "new"
    @Volatile var lastSignal = "none"
    private val app = context.applicationContext
    private val cfg = AppConfigRepository(app)
    private val signal = java.util.concurrent.atomic.AtomicReference<SignalingClient?>()
    private val rts = ConcurrentHashMap<String, Rt>()
    private val factory: PeerConnectionFactory by lazy {
        if (INIT.compareAndSet(false, true)) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(app).createInitializationOptions()
            )
        }
        val adm = JavaAudioDeviceModule.builder(app).createAudioDeviceModule()
        PeerConnectionFactory.builder().setAudioDeviceModule(adm).createPeerConnectionFactory()
    }

    fun attach(s: SignalingClient) {
        signal.set(s)
    }

    fun startOutgoing(callId: String, cb: (Boolean) -> Unit) {
        val peerId = store.get(callId)?.peerId ?: return cb(false)
        val rt = ensure(callId, peerId) ?: return cb(false)
        store.updateState(callId, CallState.CONNECTING)
        rt.pc.createOffer(object : Obs() {
            override fun onCreateSuccess(d: SessionDescription) {
                rt.pc.setLocalDescription(object : Obs() {
                    override fun onSetSuccess() {
                        lastSignal = "local_offer"
                        signal.get()?.sendWebRtcOffer(callId, peerId, d.description)
                        cb(true)
                    }
                }, d)
            }

            override fun onCreateFailure(e: String) {
                cb(false)
            }
        }, mc())
    }

    fun answer(callId: String, cb: (Boolean) -> Unit) {
        val peerId = store.get(callId)?.peerId ?: return cb(false)
        val rt = ensure(callId, peerId) ?: return cb(false)
        rt.wantAnswer.set(true)
        store.updateState(callId, CallState.CONNECTING)
        val offer = rt.offer ?: return cb(true)
        makeAnswer(callId, peerId, rt, offer, cb)
    }

    fun onRemoteOffer(callId: String, sdp: String) {
        if (sdp.isBlank()) return
        val peerId = store.get(callId)?.peerId ?: return
        val rt = ensure(callId, peerId) ?: return
        rt.offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        lastSignal = "remote_offer"
        if (rt.wantAnswer.get()) {
            makeAnswer(callId, peerId, rt, rt.offer!!, null)
        }
    }

    fun onRemoteAnswer(callId: String, sdp: String) {
        if (sdp.isBlank()) return
        val rt = rts[callId] ?: return
        rt.pc.setRemoteDescription(object : Obs() {
            override fun onSetSuccess() {
                iceState = "connected"
                store.updateState(callId, CallState.ACTIVE)
                flush(rt)
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun onRemoteIce(callId: String, raw: String) {
        val rt = rts[callId] ?: return
        val c = dec(raw) ?: return
        if (!rt.pc.addIceCandidate(c)) {
            rt.pending += c
        }
        if (iceState == "new") {
            iceState = "checking"
        }
        lastSignal = "remote_ice"
    }

    fun close(callId: String) {
        val rt = rts.remove(callId) ?: return
        rt.pc.close()
        rt.track.dispose()
        rt.source.dispose()
        iceState = "closed"
        lastSignal = "closed"
    }

    private fun makeAnswer(callId: String, peerId: String, rt: Rt, offer: SessionDescription, cb: ((Boolean) -> Unit)?) {
        rt.pc.setRemoteDescription(object : Obs() {
            override fun onSetSuccess() {
                flush(rt)
                rt.pc.createAnswer(object : Obs() {
                    override fun onCreateSuccess(d: SessionDescription) {
                        rt.pc.setLocalDescription(object : Obs() {
                            override fun onSetSuccess() {
                                lastSignal = "local_answer"
                                signal.get()?.sendWebRtcAnswer(callId, peerId, d.description)
                                cb?.invoke(true)
                            }
                        }, d)
                    }

                    override fun onCreateFailure(e: String) {
                        cb?.invoke(false)
                    }
                }, mc())
            }

            override fun onSetFailure(e: String) {
                cb?.invoke(false)
            }
        }, offer)
    }

    private fun ensure(callId: String, peerId: String): Rt? {
        rts[callId]?.let { return it }
        val src = factory.createAudioSource(MediaConstraints())
        val tr = factory.createAudioTrack("a-$callId", src)
        val pc = factory.createPeerConnection(iceServers(), object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                signal.get()?.sendWebRtcIce(callId, peerId, enc(c))
            }

            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {
                iceState = s.name.lowercase()
                if (s == PeerConnection.IceConnectionState.CONNECTED || s == PeerConnection.IceConnectionState.COMPLETED) {
                    store.updateState(callId, CallState.ACTIVE)
                }
            }

            override fun onSignalingChange(s: PeerConnection.SignalingState) {
                lastSignal = s.name.lowercase()
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>) {}
            override fun onAddStream(s: MediaStream) {}
            override fun onRemoveStream(s: MediaStream) {}
            override fun onDataChannel(d: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver, m: Array<out MediaStream>) {}
        }) ?: return null
        pc.addTrack(tr, listOf("stream-$callId"))
        return Rt(pc, src, tr).also {
            rts[callId] = it
            lastSignal = "runtime_$peerId"
        }
    }

    private fun flush(rt: Rt) {
        while (rt.pending.isNotEmpty()) {
            rt.pc.addIceCandidate(rt.pending.removeAt(0))
        }
    }

    private fun mc() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
    }

    private fun iceServers(): List<PeerConnection.IceServer> {
        val out = mutableListOf<PeerConnection.IceServer>()
        cfg.getStunServerUrl().takeIf { it.isNotBlank() }?.let {
            out += PeerConnection.IceServer.builder(it).createIceServer()
        }
        cfg.getTurnServerUrl().takeIf { it.isNotBlank() }?.let {
            val b = PeerConnection.IceServer.builder(it)
            cfg.getTurnUsername().takeIf { u -> u.isNotBlank() }?.let(b::setUsername)
            cfg.getTurnPassword().takeIf { p -> p.isNotBlank() }?.let(b::setPassword)
            out += b.createIceServer()
        }
        return out
    }

    private fun enc(c: IceCandidate): String {
        return listOf(c.sdpMid.orEmpty(), c.sdpMLineIndex.toString(), c.sdp).joinToString("\n")
    }

    private fun dec(raw: String): IceCandidate? {
        val p = raw.split("\n", limit = 3)
        val i = p.getOrNull(1)?.toIntOrNull() ?: return null
        val mid = p.getOrNull(0) ?: return null
        val sdp = p.getOrNull(2) ?: return null
        return IceCandidate(mid, i, sdp)
    }

    private class Rt(
        val pc: PeerConnection,
        val source: AudioSource,
        val track: AudioTrack,
        val pending: MutableList<IceCandidate> = mutableListOf(),
        val wantAnswer: AtomicBoolean = AtomicBoolean(false),
        var offer: SessionDescription? = null
    )

    private open class Obs : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String) {
            Log.e(TAG, e)
        }
        override fun onSetFailure(e: String) {
            Log.e(TAG, e)
        }
    }

    private companion object {
        const val TAG = "RealRtcCore"
        val INIT = AtomicBoolean(false)
    }
}
