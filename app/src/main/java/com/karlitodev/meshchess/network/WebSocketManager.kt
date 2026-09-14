package com.karlitodev.meshchess.network

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class WsEvent {
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class Error(val message: String) : WsEvent()
    data class RoomJoined(val color: Char) : WsEvent()
    object OpponentJoined : WsEvent()
    object OpponentDisconnected : WsEvent()
    data class OpponentMoved(val fromSq: String, val toSq: String, val promotion: String?) : WsEvent()
}

class WebSocketManager {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val SERVER_URL = "wss://meshchess-server-production.up.railway.app/"
    
    private val _events = MutableSharedFlow<WsEvent>(replay = 10)
    val events: SharedFlow<WsEvent> = _events
    
    var currentRoom: String? = null
        private set

    fun connect() {
        val request = Request.Builder().url(SERVER_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                _events.tryEmit(WsEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "room_joined" -> {
                            val colorStr = json.optString("color", "w")
                            _events.tryEmit(WsEvent.RoomJoined(colorStr[0]))
                        }
                        "opponent_joined" -> _events.tryEmit(WsEvent.OpponentJoined)
                        "opponent_disconnected" -> _events.tryEmit(WsEvent.OpponentDisconnected)
                        "move" -> {
                            val from = json.getString("from")
                            val to = json.getString("to")
                            val promo = json.optString("promotion", null).takeIf { it.isNotBlank() }
                            _events.tryEmit(WsEvent.OpponentMoved(from, to, promo))
                        }
                        "error" -> _events.tryEmit(WsEvent.Error(json.optString("message", "Unknown error")))
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse error", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed")
                _events.tryEmit(WsEvent.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Failure", t)
                _events.tryEmit(WsEvent.Error(t.message ?: "Connection failed"))
                _events.tryEmit(WsEvent.Disconnected)
            }
        })
    }

    fun joinRoom(roomCode: String) {
        currentRoom = roomCode
        val msg = JSONObject().apply {
            put("type", "join")
            put("room", roomCode)
        }
        webSocket?.send(msg.toString())
    }

    fun sendMove(fromSq: String, toSq: String, promotion: String?) {
        if (currentRoom == null) return
        val msg = JSONObject().apply {
            put("type", "move")
            put("room", currentRoom)
            put("from", fromSq)
            put("to", toSq)
            if (promotion != null) put("promotion", promotion)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
        currentRoom = null
        _events.tryEmit(WsEvent.Disconnected)
    }
}
