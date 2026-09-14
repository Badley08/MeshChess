package com.karlitodev.meshchess.data.network

/**
 * Server Configuration for MeshChess Online Multiplayer
 */
object ServerConfig {
    /**
     * HTTP base URL for web interface and REST endpoints
     */
    const val SERVER_URL = "https://meshchess-server-production.up.railway.app"

    /**
     * WebSocket URL for real-time online multiplayer matchmaking and game synchronization
     */
    const val WS_URL = "wss://meshchess-server-production.up.railway.app"
}
