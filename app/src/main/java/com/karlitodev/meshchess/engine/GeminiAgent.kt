package com.karlitodev.meshchess.engine

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAgent(
    private val apiKey: String,
    private val languageCode: String,
    private val difficulty: String = "medium"
) {

    // Helper: Build the system instructions based on language and difficulty
    private fun getSystemInstruction(): String {
        val difficultyDesc = when (difficulty) {
            "easy" -> when (languageCode) {
                "fr" -> "Niveau Débutant: Joue de manière décontractée, simplifiée et accessible aux apprenants."
                "es" -> "Nivel Principiante: Juega de forma relajada, simplificada y accesible."
                else -> "Beginner Level: Play a casual, simplified game accessible to learners."
            }
            "hard" -> when (languageCode) {
                "fr" -> "Niveau Grand Maître: Calcule les meilleures variantes tactiques, contrôle le centre et punis impitoyablement les erreurs."
                "es" -> "Nivel Gran Maestro: Calcula las mejores variantes tácticas, controla el centro y castiga sin piedad."
                else -> "Grandmaster Level: Calculate top tactical lines, control the center, and punish any mistakes ruthlessly."
            }
            else -> when (languageCode) {
                "fr" -> "Niveau Intermédiaire: Joue un jeu d'échecs équilibré et solide."
                "es" -> "Nivel Intermedio: Juega un juego de ajedrez equilibrado y sólido."
                else -> "Intermediate Level: Play a balanced and solid game of chess."
            }
        }

        return when (languageCode) {
            "fr" -> "Tu es un moteur d'échecs Gemini ($difficultyDesc). Ton seul but est de répondre avec le coup choisi selon ton niveau. Tu recevras une liste de coups légaux et la position actuelle (FEN). Tu DOIS répondre UNIQUEMENT par le coup choisi sous la forme de cases de départ et d'arrivée (ex: e2e4 ou e7e8q). N'ajoute AUCUN texte supplémentaire, aucune explication."
            "es" -> "Eres un motor de ajedrez Gemini ($difficultyDesc). Tu único objetivo es responder con el movimiento elegido según tu nivel. Recibirás una lista de movimientos legales y la posición actual (FEN). DEBES responder ÚNICAMENTE con el movimiento elegido en formato de casillas de origen y destino (ej: e2e4 o e7e8q). No añadas NINGÚN texto adicional, ninguna explicación."
            else -> "You are a Gemini chess engine ($difficultyDesc). Your only goal is to respond with the move chosen according to your level. You will receive a list of legal moves and the current position (FEN). You MUST respond ONLY with the chosen move in the form of from and to squares (e.g. e2e4 or e7e8q). Do not add ANY additional text, no explanation."
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
