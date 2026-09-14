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
    data class RoomJoined(val room: String, val color: String, val playersCount: Int) : WsEvent()
    data class GameStart(val opponentUsername: String, val fen: String) : WsEvent()
    data class OpponentMoved(val fromSq: String, val toSq: String, val promotion: String?, val fen: String) : WsEvent()
    data class PlayerDisconnected(val message: String) : WsEvent()
    data class GameOver(val reason: String, val winner: String?) : WsEvent()
    object QuickMatchWaiting : WsEvent()
    object QuickMatchCancelled : WsEvent()
    data class DrawOffered(val fromUsername: String) : WsEvent()
    data class DrawDeclined(val byUsername: String) : WsEvent()
    data class ChatMessage(val text: String, val username: String, val color: String) : WsEvent()
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
    var playerColor: String? = null
        private set
    var isConnected: Boolean = false
        private set

    fun connect() {
        if (isConnected) return
        val request = Request.Builder().url(SERVER_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
                isConnected = true
                _events.tryEmit(WsEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "ROOM_JOINED" -> {
                            val room = json.optString("room", "")
                            val color = json.optString("color", "white")
                            val count = json.optInt("playersCount", 1)
                            currentRoom = room
                            playerColor = color
                            _events.tryEmit(WsEvent.RoomJoined(room, color, count))
                        }
                        "GAME_START" -> {
                            val opponent = json.optString("opponentUsername", "Opponent")
                            val fen = json.optString("fen", "")
                            _events.tryEmit(WsEvent.GameStart(opponent, fen))
                        }
                        "MOVE" -> {
                            val from = json.getString("from")
                            val to = json.getString("to")
                            val promo = json.optString("promotion", "").takeIf { it.isNotBlank() }
                            val fen = json.optString("fen", "")
                            _events.tryEmit(WsEvent.OpponentMoved(from, to, promo, fen))
                        }
                        "PLAYER_DISCONNECTED" -> {
                            val msg = json.optString("message", "Opponent disconnected")
                            _events.tryEmit(WsEvent.PlayerDisconnected(msg))
                        }
                        "GAME_OVER" -> {
                            val reason = json.optString("reason", "unknown")
                            val winner = json.optString("winner", "").takeIf { it.isNotBlank() }
                            _events.tryEmit(WsEvent.GameOver(reason, winner))
                        }
                        "QUICK_MATCH_WAITING" -> {
                            _events.tryEmit(WsEvent.QuickMatchWaiting)
                        }
                        "QUICK_MATCH_CANCELLED" -> {
                            _events.tryEmit(WsEvent.QuickMatchCancelled)
                        }
                        "DRAW_OFFERED" -> {
                            val fromUser = json.optString("fromUsername", "Opponent")
                            _events.tryEmit(WsEvent.DrawOffered(fromUser))
                        }
                        "DRAW_DECLINED" -> {
                            val byUser = json.optString("byUsername", "Opponent")
                            _events.tryEmit(WsEvent.DrawDeclined(byUser))
                        }
                        "CHAT" -> {
                            val chatText = json.optString("text", "")
                            val username = json.optString("username", "")
                            val color = json.optString("color", "")
                            _events.tryEmit(WsEvent.ChatMessage(chatText, username, color))
                        }
                        "ERROR" -> {
                            _events.tryEmit(WsEvent.Error(json.optString("message", "Unknown error")))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse error", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed")
                isConnected = false
                _events.tryEmit(WsEvent.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Failure", t)
                isConnected = false
                _events.tryEmit(WsEvent.Error(t.message ?: "Connection failed"))
                _events.tryEmit(WsEvent.Disconnected)
            }
        })
    }

    fun joinRoom(roomCode: String, username: String = "Player") {
        currentRoom = roomCode
        val msg = JSONObject().apply {
            put("type", "JOIN_ROOM")
            put("room", roomCode)
            put("username", username)
        }
        webSocket?.send(msg.toString())
    }

    fun quickMatch(username: String = "Player") {
        val msg = JSONObject().apply {
            put("type", "QUICK_MATCH")
            put("username", username)
        }
        webSocket?.send(msg.toString())
    }

    fun cancelQuickMatch() {
        val msg = JSONObject().apply {
            put("type", "CANCEL_QUICK_MATCH")
        }
        webSocket?.send(msg.toString())
    }

    fun sendMove(fromSq: String, toSq: String, promotion: String?) {
        if (currentRoom == null) return
        val msg = JSONObject().apply {
            put("type", "MOVE")
            put("room", currentRoom)
            put("from", fromSq)
            put("to", toSq)
            if (promotion != null) put("promotion", promotion)
        }
        webSocket?.send(msg.toString())
    }

    fun resign() {
        val msg = JSONObject().apply {
            put("type", "RESIGN")
        }
        webSocket?.send(msg.toString())
    }

    fun offerDraw() {
        val msg = JSONObject().apply {
            put("type", "OFFER_DRAW")
        }
        webSocket?.send(msg.toString())
    }

    fun acceptDraw() {
        val msg = JSONObject().apply {
            put("type", "ACCEPT_DRAW")
        }
        webSocket?.send(msg.toString())
    }

    fun declineDraw() {
        val msg = JSONObject().apply {
            put("type", "DECLINE_DRAW")
        }
        webSocket?.send(msg.toString())
    }

    fun sendChat(text: String) {
        val msg = JSONObject().apply {
            put("type", "CHAT")
            put("text", text)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
        currentRoom = null
        playerColor = null
        isConnected = false
        _events.tryEmit(WsEvent.Disconnected)
    }
}
