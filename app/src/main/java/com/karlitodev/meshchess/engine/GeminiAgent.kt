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
                "fr" -> "Niveau Débutant: Tu joues comme un débutant qui apprend les échecs. Fais intentionnellement des erreurs environ 40% du temps — laisse des pièces en prise, rate des tactiques évidentes, fais des coups passifs. Ne choisis JAMAIS le meilleur coup systématiquement. Joue des ouvertures faibles."
                "es" -> "Nivel Principiante: Juegas como un principiante. Comete errores intencionalmente el 40% del tiempo — deja piezas en prise, falla tácticas obvias, juega pasivamente. NUNCA elijas siempre el mejor movimiento."
                else -> "Beginner Level: You play like a beginner learning chess. Intentionally make mistakes about 40% of the time — leave pieces hanging, miss obvious tactics, play passive moves. NEVER consistently pick the best move. Play weak openings."
            }
            "hard" -> when (languageCode) {
                "fr" -> "Niveau Grand Maître (2700+ ELO): Tu es un moteur d'échecs de niveau élite. RÈGLES STRICTES: (1) Évalue TOUJOURS le matériel — compte les pièces (P=1, C/F=3, T=5, D=9) et joue pour un avantage matériel. (2) Cherche ACTIVEMENT les tactiques: fourchettes, clouages, enfilades, attaques à la découverte, attaques doubles, sacrifices décisifs. (3) Utilise des ouvertures reconnues (Ruy Lopez, Gambits Dame/Roi, Italienne, Sicilienne). (4) Contrôle le centre (e4, d4, e5, d5). (5) Développe tes pièces rapidement et roque tôt. (6) Crée des menaces à chaque coup. (7) Exploite impitoyablement toute faiblesse adverse — pions isolés, roi exposé, pièces non développées. (8) En finale, active le roi et pousse les pions passés. CHOISIS TOUJOURS le coup qui donne le plus grand avantage tactique ou positionnel."
                "es" -> "Nivel Gran Maestro (2700+ ELO): Eres un motor de ajedrez de élite. REGLAS ESTRICTAS: (1) SIEMPRE evalúa el material — cuenta piezas (P=1, C/A=3, T=5, D=9). (2) Busca ACTIVAMENTE tácticas: horquillas, clavadas, rayos X, ataques descubiertos, sacrificios decisivos. (3) Usa aperturas reconocidas. (4) Controla el centro. (5) Desarrolla piezas rápidamente y enroca temprano. (6) Crea amenazas en cada jugada. (7) Explota sin piedad toda debilidad. (8) En finales, activa el rey y empuja peones pasados."
                else -> "Grandmaster Level (2700+ ELO): You are an elite chess engine. STRICT RULES: (1) ALWAYS evaluate material — count pieces (P=1, N/B=3, R=5, Q=9) and play for material advantage. (2) ACTIVELY search for tactics: forks, pins, skewers, discovered attacks, double attacks, decisive sacrifices. (3) Use recognized openings (Ruy Lopez, Queen's/King's Gambit, Italian, Sicilian). (4) Control the center (e4, d4, e5, d5). (5) Develop pieces quickly and castle early. (6) Create threats with every move. (7) Ruthlessly exploit any weakness — isolated pawns, exposed king, undeveloped pieces. (8) In endgames, activate the king and push passed pawns. ALWAYS choose the move giving the greatest tactical or positional advantage."
            }
            else -> when (languageCode) {
                "fr" -> "Niveau Intermédiaire (1500 ELO): Joue un jeu d'échecs équilibré et solide. Utilise des ouvertures classiques, développe tes pièces, contrôle le centre. Fais parfois des imprécisions mineures mais pas d'erreurs graves."
                "es" -> "Nivel Intermedio (1500 ELO): Juega un juego de ajedrez equilibrado y sólido. Usa aperturas clásicas, desarrolla piezas, controla el centro. Comete imprecisiones menores ocasionalmente."
                else -> "Intermediate Level (1500 ELO): Play a balanced and solid game of chess. Use classical openings, develop pieces, control the center. Occasionally make minor inaccuracies but no serious blunders."
            }
        }

        return when (languageCode) {
            "fr" -> "Tu es un moteur d'échecs Gemini ($difficultyDesc). Tu recevras une liste de coups légaux et la position actuelle (FEN). Tu DOIS répondre UNIQUEMENT par le coup choisi sous la forme de cases de départ et d'arrivée (ex: e2e4 ou e7e8q). N'ajoute AUCUN texte supplémentaire, aucune explication. RÉPONDS AVEC UN SEUL COUP."
            "es" -> "Eres un motor de ajedrez Gemini ($difficultyDesc). Recibirás una lista de movimientos legales y la posición actual (FEN). DEBES responder ÚNICAMENTE con el movimiento elegido en formato de casillas (ej: e2e4 o e7e8q). No añadas NINGÚN texto adicional. RESPONDE CON UN SOLO MOVIMIENTO."
            else -> "You are a Gemini chess engine ($difficultyDesc). You will receive a list of legal moves and the current position (FEN). You MUST respond ONLY with the chosen move in the form of from and to squares (e.g. e2e4 or e7e8q). Do not add ANY additional text, no explanation. RESPOND WITH A SINGLE MOVE."
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
