package com.karlitodev.meshchess.engine

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAgent(private val apiKey: String, private val languageCode: String) {

    // Helper: Build the system instructions based on the selected language.
    private fun getSystemInstruction(): String {
        return when (languageCode) {
            "fr" -> "Tu es un moteur d'échecs expert. Ton seul but est de répondre avec le meilleur coup d'échecs légal. Tu recevras une liste de coups légaux et la position actuelle (FEN). Tu DOIS répondre UNIQUEMENT par le coup choisi sous la forme de cases de départ et d'arrivée (ex: e2e4). N'ajoute AUCUN texte supplémentaire, aucune explication."
            "es" -> "Eres un experto motor de ajedrez. Tu único objetivo es responder con el mejor movimiento de ajedrez legal. Recibirás una lista de movimientos legales y la posición actual (FEN). DEBES responder ÚNICAMENTE con el movimiento elegido en formato de casillas de origen y destino (ej: e2e4). No añadas NINGÚN texto adicional, ninguna explicación."
            else -> "You are an expert chess engine. Your only goal is to respond with the best legal chess move. You will receive a list of legal moves and the current position (FEN). You MUST respond ONLY with the chosen move in the form of from and to squares (e.g. e2e4). Do not add ANY additional text, no explanation."
        }
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun getBestMove(fen: String, legalMoves: List<ChessMove>, engine: ChessEngine): ChessMove? {
        if (legalMoves.isEmpty()) return null

        val moveListStr = legalMoves.joinToString(", ") { move ->
            "${engine.coordToSq(move.from.file, move.from.rank)}${engine.coordToSq(move.to.file, move.to.rank)}${move.promotion ?: ""}"
        }

        val promptText = when (languageCode) {
            "fr" -> "Position actuelle (FEN) : $fen\nCoups légaux : $moveListStr\nQuel est le meilleur coup ?"
            "es" -> "Posición actual (FEN) : $fen\nMovimientos legales : $moveListStr\n¿Cuál es el mejor movimiento?"
            else -> "Current Position (FEN): $fen\nLegal Moves: $moveListStr\nWhat is the best move?"
        }

        val fullPrompt = getSystemInstruction() + "\n\n" + promptText

        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(fullPrompt)
                val responseText = response.text?.trim()?.lowercase() ?: return@withContext null
                
                // Try to find the returned move in the legal moves list
                val bestMove = legalMoves.find { move ->
                    val moveStr = "${engine.coordToSq(move.from.file, move.from.rank)}${engine.coordToSq(move.to.file, move.to.rank)}${move.promotion?.lowercaseChar() ?: ""}"
                    responseText.contains(moveStr)
                }
                
                // Fallback to random if AI hallucinated or gave invalid output
                bestMove ?: legalMoves.random()
            } catch (e: Exception) {
                // If network fails or quota exceeded, pick a random move so the game doesn't stall
                legalMoves.random()
            }
        }
    }
}
