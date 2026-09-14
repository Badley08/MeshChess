package com.karlitodev.meshchess.data.network

import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocket Client for MeshChess Online Multiplayer.
 * Connects to Railway server (wss://meshchess-server-production.up.railway.app).
 */
class WebSocketClient(
    private val serverUrl: String = ServerConfig.WS_URL,
    private val listener: GameEventListener
) {
    interface GameEventListener {
        fun onConnected()
        fun onRoomJoined(room: String, color: String, username: String, playersCount: Int)
        fun onGameStart(opponentUsername: String)
        fun onMoveReceived(from: String, to: String, username: String)
        fun onPlayerDisconnected(message: String)
        fun onError(error: String)
        fun onDisconnected()
    }

    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected: Boolean = false
        private set

    fun connect() {
        if (isConnected) return

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "ROOM_JOINED" -> {
                            val room = json.optString("room")
                            val color = json.optString("color")
                            val username = json.optString("username")
                            val playersCount = json.optInt("playersCount", 1)
                            listener.onRoomJoined(room, color, username, playersCount)
                        }
                        "GAME_START" -> {
                            val opponentUsername = json.optString("opponentUsername")
                            listener.onGameStart(opponentUsername)
                        }
                        "MOVE" -> {
                            val from = json.optString("from")
                            val to = json.optString("to")
                            val username = json.optString("username")
                            listener.onMoveReceived(from, to, username)
                        }
                        "PLAYER_DISCONNECTED" -> {
                            val msg = json.optString("message", "L'adversaire s'est déconnecté")
                            listener.onPlayerDisconnected(msg)
                        }
                        "ERROR" -> {
                            val msg = json.optString("message", "Erreur serveur")
                            listener.onError(msg)
                        }
                    }
                } catch (e: Exception) {
                    listener.onError("Erreur parsing: ${e.localizedMessage}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener.onError("Erreur connexion: ${t.localizedMessage}")
                listener.onDisconnected()
            }
        })
    }

    fun joinRoom(roomCode: String, username: String) {
        val payload = JSONObject().apply {
            put("type", "JOIN_ROOM")
            put("room", roomCode)
            put("username", username)
        }
        webSocket?.send(payload.toString())
    }

    fun sendMove(from: String, to: String) {
        val payload = JSONObject().apply {
            put("type", "MOVE")
            put("from", from)
            put("to", to)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }
}
