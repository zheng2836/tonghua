package com.zheng2836.tonghua.signaling

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.zheng2836.tonghua.data.CallSession
import com.zheng2836.tonghua.data.CallState
import com.zheng2836.tonghua.data.CallStore
import com.zheng2836.tonghua.telecom.ConnectionRegistry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class SignalingClient(
    private val context: Context,
    private val callStore: CallStore,
    private val connectionRegistry: ConnectionRegistry
) {
    companion object {
        private const val TAG = "Signal"
        private const val DEV_HTTP_BASE_URL = "http://10.0.2.2:8080"
        private const val DEV_WS_BASE_URL = "ws://10.0.2.2:8080/ws"
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val userId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "android-debug"
    }

    @Volatile
    var connectionState: String = "idle"
        private set

    fun connect() {
        if (webSocket != null) return
        connectionState = "connecting"
        val request = Request.Builder()
            .url("$DEV_WS_BASE_URL?userId=$userId")
            .build()
        webSocket = client.newWebSocket(request, SignalingSocketListener())
    }

    fun disconnect() {
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
            .url("$DEV_HTTP_BASE_URL/devices/register")
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

        val sent = webSocket?.send(payload) ?: false
        Log.i(TAG, "send type=$type callId=$callId ok=$sent")
    }

    private inner class SignalingSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            connectionState = "connected"
            Log.i(TAG, "ws connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i(TAG, "ws message=$text")
            handleIncoming(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            connectionState = "closing"
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            connectionState = "closed"
            this@SignalingClient.webSocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            connectionState = "failed"
            Log.e(TAG, "ws failed", t)
            this@SignalingClient.webSocket = null
            response?.close()
        }
    }

    private fun handleIncoming(text: String) {
        val json = JSONObject(text)
        val type = json.optString("type")
        val callId = json.optString("callId")

        when (type) {
            "call.ringing" -> callStore.updateState(callId, CallState.RINGING)
            "call.answer" -> onRemoteAnswered(callId)
            "call.reject" -> onRemoteRejected(callId)
            "call.hangup", "call.cancel" -> onRemoteHangup(callId)
        }
    }
}
