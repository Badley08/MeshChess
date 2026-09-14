package com.karlitodev.meshchess.engine

/**
 * Built-in chess puzzles for the Puzzles game mode.
 * Each puzzle has a FEN starting position and a solution sequence.
 */

data class Puzzle(
    val id: Int,
    val title: String,
    val fen: String,
    val solution: List<String>,  // List of moves in "e2e4" format
    val difficulty: String,      // "beginner", "intermediate", "advanced"
    val description: Map<String, String>  // Localized hints: "fr", "en", "es"
)

object PuzzleLibrary {

    val puzzles: List<Puzzle> = listOf(
        // ═══════════════════════════════════════════════════
        //  BEGINNER — Mate in 1
        // ═══════════════════════════════════════════════════
        Puzzle(
            id = 1,
            title = "Back Rank Mate",
            fen = "6k1/5ppp/8/8/8/8/8/R3K3 w Q - 0 1",
            solution = listOf("a1a8"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Mat du couloir ! Trouvez le mat en 1 coup.",
                "en" to "Back rank mate! Find the checkmate in 1 move.",
                "es" to "¡Mate de pasillo! Encuentra el mate en 1 movimiento."
            )
        ),
        Puzzle(
            id = 2,
            title = "Queen Checkmate",
            fen = "4k3/8/8/8/8/8/8/4K2Q w - - 0 1",
            solution = listOf("h1h8"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Matez avec la dame en 1 coup.",
                "en" to "Checkmate with the queen in 1 move.",
                "es" to "Da mate con la dama en 1 movimiento."
            )
        ),
        Puzzle(
            id = 3,
            title = "Rook Ladder",
            fen = "k7/8/1K6/8/8/8/8/7R w - - 0 1",
            solution = listOf("h1a1"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Matez avec la tour en 1 coup.",
                "en" to "Checkmate with the rook in 1 move.",
                "es" to "Da mate con la torre en 1 movimiento."
            )
        ),
        Puzzle(
            id = 4,
            title = "Bishop + Queen Mate",
            fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solution = listOf("f3f7"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Le coup du Berger ! Matez en 1.",
                "en" to "Scholar's Mate! Checkmate in 1.",
                "es" to "¡El mate del pastor! Mate en 1."
            )
        ),
        Puzzle(
            id = 5,
            title = "Smothered Checkmate",
            fen = "r5rk/5Npp/8/8/8/8/8/4K3 w - - 0 1",
            solution = listOf("f7g5"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Mat étouffé avec le cavalier.",
                "en" to "Smothered checkmate with the knight.",
                "es" to "Mate ahogado con el caballo."
            )
        ),

        // ═══════════════════════════════════════════════════
        //  BEGINNER — Simple Tactics
        // ═══════════════════════════════════════════════════
        Puzzle(
            id = 6,
            title = "Win the Queen",
            fen = "rnb1kbnr/pppppppp/8/8/4q3/2N5/PPPPPPPP/R1BQKBNR w KQkq - 0 1",
            solution = listOf("c3e4"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Capturez la dame adverse !",
                "en" to "Capture the opponent's queen!",
                "es" to "¡Captura la dama rival!"
            )
        ),
        Puzzle(
            id = 7,
            title = "Knight Fork",
            fen = "r1bqkb1r/pppppppp/2n2n2/8/3NP3/8/PPP2PPP/RNBQKB1R w KQkq - 2 4",
            solution = listOf("d4c6"),
            difficulty = "beginner",
            description = mapOf(
                "fr" to "Fourchette de cavalier !",
                "en" to "Knight fork!",
                "es" to "¡Horquilla de caballo!"
            )
        ),

        // ═══════════════════════════════════════════════════
        //  INTERMEDIATE — Mate in 2
        // ═══════════════════════════════════════════════════
        Puzzle(
            id = 8,
            title = "Anastasia's Mate",
            fen = "4rrk1/5Npp/8/8/8/8/5R2/6K1 w - - 0 1",
            solution = listOf("f7h6", "g8h8", "f2f8"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "Le mat d'Anastasia. Trouvez la séquence gagnante.",
                "en" to "Anastasia's Mate. Find the winning sequence.",
                "es" to "El mate de Anastasia. Encuentra la secuencia ganadora."
            )
        ),
        Puzzle(
            id = 9,
            title = "Queen Sacrifice",
            fen = "r1bq2kr/pppp1Qpp/2n2n2/2b1p3/2B1P3/8/PPPP1PPP/RNB1K1NR w KQ - 0 1",
            solution = listOf("f7g8"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "Sacrifiez la dame pour mater !",
                "en" to "Sacrifice the queen to checkmate!",
                "es" to "¡Sacrifica la dama para dar mate!"
            )
        ),
        Puzzle(
            id = 10,
            title = "Double Check Mate",
            fen = "rnbqkb1r/pppp1ppp/5n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 3",
            solution = listOf("h5f7"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "Trouvez le mat avec échec double.",
                "en" to "Find the checkmate with double check.",
                "es" to "Encuentra el mate con jaque doble."
            )
        ),
        Puzzle(
            id = 11,
            title = "Pin and Win",
            fen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 3",
            solution = listOf("c6d4"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "Exploitez le clouage pour gagner du matériel.",
                "en" to "Exploit the pin to win material.",
                "es" to "Explota la clavada para ganar material."
            )
        ),
        Puzzle(
            id = 12,
            title = "Discovered Attack",
            fen = "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
            solution = listOf("e2e3"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "Attaque à la découverte sur le fou.",
                "en" to "Discovered attack on the bishop.",
                "es" to "Ataque descubierto sobre el alfil."
            )
        ),
        Puzzle(
            id = 13,
            title = "Trapped Piece",
            fen = "rnbqk1nr/pppp1ppp/8/4p3/1b2P3/2NP4/PPP2PPP/R1BQKBNR b KQkq - 0 3",
            solution = listOf("b4c3"),
            difficulty = "intermediate",
            description = mapOf(
                "fr" to "La pièce est piégée ! Capturez-la.",
                "en" to "The piece is trapped! Capture it.",
                "es" to "¡La pieza está atrapada! Captúrala."
            )
        ),

        // ═══════════════════════════════════════════════════
        //  ADVANCED — Complex Tactics / Mate in 3+
        // ═══════════════════════════════════════════════════
        Puzzle(
            id = 14,
            title = "Boden's Mate",
            fen = "2kr3r/ppp2ppp/2n1b3/4p3/2B5/2N2N2/PPP2PPP/R1B1K2R w KQ - 0 1",
            solution = listOf("c4a6"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Le mat de Boden — les deux fous attaquent !",
                "en" to "Boden's Mate — both bishops attack!",
                "es" to "El mate de Boden — ¡los dos alfiles atacan!"
            )
        ),
        Puzzle(
            id = 15,
            title = "Greek Gift Sacrifice",
            fen = "rnbq1rk1/pppn1ppp/4p3/3pP3/3P4/2N2N2/PPP1BPPP/R1BQ1RK1 w - - 0 7",
            solution = listOf("e2h5"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Le sacrifice grec sur h7. Préparez l'attaque.",
                "en" to "The Greek Gift sacrifice on h7. Prepare the attack.",
                "es" to "El sacrificio griego en h7. Prepara el ataque."
            )
        ),
        Puzzle(
            id = 16,
            title = "Clearance Sacrifice",
            fen = "r1b1kb1r/pppp1ppp/2n2n2/1B2p1q1/4P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 5",
            solution = listOf("f3e5"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Sacrifice de libération pour ouvrir des lignes.",
                "en" to "Clearance sacrifice to open lines.",
                "es" to "Sacrificio de liberación para abrir líneas."
            )
        ),
        Puzzle(
            id = 17,
            title = "Deflection",
            fen = "r2qk2r/ppp2ppp/2n1bn2/3pp3/2B1P1b1/2NP1N2/PPP2PPP/R1BQR1K1 w kq - 0 7",
            solution = listOf("c4d5"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Déviation ! Forcez le déplacement d'une pièce clé.",
                "en" to "Deflection! Force a key piece to move.",
                "es" to "¡Desviación! Fuerza el movimiento de una pieza clave."
            )
        ),
        Puzzle(
            id = 18,
            title = "X-Ray Attack",
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
            solution = listOf("f3g5"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Attaque en rayon X à travers le cavalier.",
                "en" to "X-Ray attack through the knight.",
                "es" to "Ataque de rayos X a través del caballo."
            )
        ),
        Puzzle(
            id = 19,
            title = "Zwischenzug",
            fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            solution = listOf("f1b5"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Coup intermédiaire ! Jouez le Zwischenzug.",
                "en" to "Intermediate move! Play the Zwischenzug.",
                "es" to "¡Movimiento intermedio! Juega el Zwischenzug."
            )
        ),
        Puzzle(
            id = 20,
            title = "Windmill",
            fen = "rn1qkb1r/pp2pppp/2p2n2/3p2B1/3P4/2N2N2/PPP1PPPP/R2QKB1R w KQkq - 0 5",
            solution = listOf("g5f6"),
            difficulty = "advanced",
            description = mapOf(
                "fr" to "Le moulin ! Série de coups dévastateurs.",
                "en" to "The windmill! Series of devastating moves.",
                "es" to "¡El molino! Serie de jugadas devastadoras."
            )
        )
    )

    fun getByDifficulty(difficulty: String): List<Puzzle> =
        puzzles.filter { it.difficulty == difficulty }

    fun getById(id: Int): Puzzle? =
        puzzles.find { it.id == id }
}
