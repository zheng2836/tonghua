package com.zheng2836.tonghua.signaling

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.zheng2836.tonghua.config.AppConfigRepository
import com.zheng2836.tonghua.data.CallSession
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.identity.IdentityRepository
import com.zheng2836.tonghua.telecom.ConnectionRegistry
import com.zheng2836.tonghua.webrtc.WebRtcEngine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.ArrayDeque

class SignalingClient(
    private val context: Context,
    private val callStore: CallStore,
    private val connectionRegistry: ConnectionRegistry
) {
    companion object {
        private const val TAG = "Signal"
        private const val RECONNECT_DELAY_MS = 3000L
        private const val PING_INTERVAL_MS = 15000L
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private var webRtcEngine: WebRtcEngine? = null
    private val identityRepository = IdentityRepository(context)
    private val appConfigRepository = AppConfigRepository(context)
    private val pendingMessages = ArrayDeque<String>()
    private val userId: String
        get() = identityRepository.getMyVirtualNumber()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var manuallyClosed = false
    private val reconnectRunnable = Runnable {
        if (!manuallyClosed && webSocket == null) {
            connect()
        }
    }
    private val pingRunnable = object : Runnable {
        override fun run() {
            if (webSocket != null && connectionState == "connected") {
                val payload = JSONObject()
                    .put("type", "ping")
                    .put("callId", "")
                    .put("data", JSONObject())
                    .toString()
                webSocket?.send(payload)
                mainHandler.postDelayed(this, PING_INTERVAL_MS)
            }
        }
    }

    @Volatile
    var connectionState: String = "idle"
        private set

    fun attachWebRtcEngine(engine: WebRtcEngine) {
        webRtcEngine = engine
    }

    fun connect() {
        if (webSocket != null) return
        manuallyClosed = false
        connectionState = "connecting"
        mainHandler.removeCallbacks(reconnectRunnable)
        val request = Request.Builder()
            .url(appConfigRepository.getServerWsBaseUrl() + "?userId=$userId")
            .build()
        webSocket = client.newWebSocket(request, SignalingSocketListener())
    }

    fun disconnect() {
        manuallyClosed = true
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.removeCallbacks(pingRunnable)
        webSocket?.close(1000, "client closing")
        webSocket = null
        connectionState = "closed"
    }

    fun registerFcmToken(token: String) {
        Log.i(TAG, "registerFcmToken=$token")
        val body = JSONObject()
            .put("userId", userId)
            .put("deviceId", userId)
            .put("fcmToken", token)
            .toString()
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(appConfigRepository.getServerHttpBaseUrl() + "/devices/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e(TAG, "register token failed", e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.close()
                Log.i(TAG, "register token ok code=${response.code}")
            }
        })
    }

    fun startOutgoingInvite(session: CallSession) {
        callStore.updateState(session.callId, CallState.INVITING)
        sendMessage(
            type = "call.invite",
            callId = session.callId,
            targetUserId = session.peerId,
            extra = mapOf("callerName" to session.peerDisplayName)
        )
    }

    fun markIncomingRinging(callId: String) {
        callStore.updateState(callId, CallState.RINGING)
        callStore.get(callId)?.let {
            sendMessage("call.ringing", callId, it.peerId)
        }
    }

    fun answer(callId: String) {
        callStore.updateState(callId, CallState.CONNECTING)
        callStore.get(callId)?.let {
            sendMessage("call.answer", callId, it.peerId)
        }
    }

    fun reject(callId: String) {
        callStore.updateState(callId, CallState.REJECTED)
        callStore.get(callId)?.let {
            sendMessage("call.reject", callId, it.peerId)
        }
    }

    fun hangup(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        callStore.get(callId)?.let {
            sendMessage("call.hangup", callId, it.peerId)
        }
    }

    fun cancel(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        callStore.get(callId)?.let {
            sendMessage("call.cancel", callId, it.peerId)
        }
    }

    fun hold(callId: String) {
        Log.i(TAG, "call.hold $callId")
    }

    fun unhold(callId: String) {
        Log.i(TAG, "call.unhold $callId")
    }

    fun sendWebRtcOffer(callId: String, targetUserId: String, sdp: String) {
        sendMessage("webrtc.offer", callId, targetUserId, mapOf("sdp" to sdp))
    }

    fun sendWebRtcAnswer(callId: String, targetUserId: String, sdp: String) {
        sendMessage("webrtc.answer", callId, targetUserId, mapOf("sdp" to sdp))
    }

    fun sendWebRtcIce(callId: String, targetUserId: String, candidate: String) {
        sendMessage("webrtc.ice", callId, targetUserId, mapOf("candidate" to candidate))
    }

    fun onRemoteAnswered(callId: String) {
        callStore.updateState(callId, CallState.ACTIVE)
        connectionRegistry.get(callId)?.onRemoteAnswered()
    }

    fun onRemoteRejected(callId: String) {
        callStore.updateState(callId, CallState.REJECTED)
        connectionRegistry.get(callId)?.onRemoteRejected()
    }

    fun onRemoteHangup(callId: String) {
        callStore.updateState(callId, CallState.ENDED)
        connectionRegistry.get(callId)?.onRemoteHangup()
    }

    fun reportCreateConnectionFailed(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e(TAG, "createConnectionFailed $callId $reason")
    }

    fun reportRuntimeFailure(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e(TAG, "runtimeFailure $callId $reason")
    }

    fun reportIncomingEscalationFailed(callId: String, reason: String) {
        callStore.updateState(callId, CallState.FAILED)
        Log.e(TAG, "incomingEscalationFailed $callId $reason")
    }

    private fun sendMessage(type: String, callId: String, targetUserId: String, extra: Map<String, String> = emptyMap()) {
        val data = JSONObject().put("targetUserId", targetUserId)
        extra.forEach { (k, v) -> data.put(k, v) }
        val payload = JSONObject()
            .put("type", type)
            .put("callId", callId)
            .put("data", data)
            .toString()

        if (connectionState != "connected" || webSocket == null) {
            if (pendingMessages.size >= 64) {
                pendingMessages.removeFirst()
            }
            pendingMessages.addLast(payload)
            connect()
            Log.i(TAG, "queued type=$type callId=$callId because ws not connected")
            return
        }

        val sent = webSocket?.send(payload) ?: false
        if (!sent) {
            if (pendingMessages.size >= 64) {
                pendingMessages.removeFirst()
            }
            pendingMessages.addLast(payload)
            connect()
        }
        Log.i(TAG, "send type=$type callId=$callId ok=$sent")
    }

    private fun flushPendingMessages() {
        while (connectionState == "connected" && webSocket != null && pendingMessages.isNotEmpty()) {
            val payload = pendingMessages.removeFirst()
            val sent = webSocket?.send(payload) ?: false
            if (!sent) {
                pendingMessages.addFirst(payload)
                break
            }
        }
    }

    private fun scheduleReconnect() {
        if (manuallyClosed) return
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS)
    }

    private inner class SignalingSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            connectionState = "connected"
            Log.i(TAG, "ws connected")
            flushPendingMessages()
            mainHandler.removeCallbacks(pingRunnable)
            mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i(TAG, "ws message=$text")
            handleIncoming(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            connectionState = "closing"
            mainHandler.removeCallbacks(pingRunnable)
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            connectionState = "closed"
            mainHandler.removeCallbacks(pingRunnable)
            this@SignalingClient.webSocket = null
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            connectionState = "failed"
            Log.e(TAG, "ws failed", t)
            mainHandler.removeCallbacks(pingRunnable)
            this@SignalingClient.webSocket = null
            response?.close()
            scheduleReconnect()
        }
    }

    private fun handleIncoming(text: String) {
        val json = JSONObject(text)
        val type = json.optString("type")
        val callId = json.optString("callId")
        val data = json.optJSONObject("data")

        when (type) {
            "pong" -> Log.d(TAG, "ws pong")
            "call.ringing" -> callStore.updateState(callId, CallState.RINGING)
            "call.answer" -> onRemoteAnswered(callId)
            "call.reject" -> onRemoteRejected(callId)
            "call.hangup", "call.cancel" -> onRemoteHangup(callId)
            "webrtc.offer" -> webRtcEngine?.onRemoteOffer(callId, data?.optString("sdp").orEmpty())
            "webrtc.answer" -> webRtcEngine?.onRemoteAnswer(callId, data?.optString("sdp").orEmpty())
            "webrtc.ice" -> webRtcEngine?.onRemoteIce(callId, data?.optString("candidate").orEmpty())
        }
    }
}
