package com.karlitodev.meshchess.engine

/**
 * Pure-Kotlin chess engine — faithful port of chess_rules.py.
 * No external dependencies, works on Android without Chaquopy.
 */

data class Pos(val file: Int, val rank: Int)

data class ChessMove(
    val from: Pos,
    val to: Pos,
    val promotion: Char? = null,
    val isEnPassant: Boolean = false,
    val isCastle: Char? = null,   // 'K' or 'Q'
    val isDoublePush: Boolean = false
)

data class GameStatus(
    val inCheck: Boolean,
    val checkmate: Boolean,
    val stalemate: Boolean,
    val gameOver: Boolean,
    val result: String?,
    val winner: Char?,
    val turn: Char
)

class ChessEngine {

    // board[rank][file]; piece = "wP","bK" etc.  null = empty
    var board = Array(8) { arrayOfNulls<String>(8) }
        private set
    var turn: Char = 'w'; private set
    var castling = mutableMapOf("wK" to true, "wQ" to true, "bK" to true, "bQ" to true)
        private set
    var enPassant: String? = null; private set
    var halfClock = 0; private set
    var fullMove = 1; private set
    val history = mutableListOf<Map<String, Any?>>()

    // ------- Offsets -------
    private val knightOff = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    private val diagDirs = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    private val orthDirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    init { reset() }

    fun reset() {
        board = Array(8) { arrayOfNulls(8) }
        val back = charArrayOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
        for (f in 0..7) {
            board[0][f] = "w${back[f]}"; board[1][f] = "wP"
            board[6][f] = "bP"; board[7][f] = "b${back[f]}"
        }
        turn = 'w'
        castling = mutableMapOf("wK" to true, "wQ" to true, "bK" to true, "bQ" to true)
        enPassant = null; halfClock = 0; fullMove = 1
        history.clear()
    }

    fun loadFen(fen: String) {
        val parts = fen.trim().split(" ")
        board = Array(8) { arrayOfNulls(8) }
        val ranks = parts[0].split("/")
        for (i in 0..7) {
            val rank = 7 - i; var file = 0
            for (ch in ranks[i]) {
                if (ch.isDigit()) { file += ch.digitToInt() }
                else {
                    val color = if (ch.isUpperCase()) 'w' else 'b'
                    board[rank][file] = "$color${ch.uppercaseChar()}"
                    file++
                }
            }
        }
        turn = if (parts.size > 1 && parts[1] == "b") 'b' else 'w'
        castling = mutableMapOf("wK" to false, "wQ" to false, "bK" to false, "bQ" to false)
        if (parts.size > 2 && parts[2] != "-") {
            for (ch in parts[2]) {
                when (ch) {
                    'K' -> castling["wK"] = true; 'Q' -> castling["wQ"] = true
                    'k' -> castling["bK"] = true; 'q' -> castling["bQ"] = true
                }
            }
        }
        enPassant = if (parts.size > 3 && parts[3] != "-") parts[3] else null
        halfClock = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
        fullMove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1
        history.clear()
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = board[r][f]
                if (p == null) { empty++ } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    val letter = p[1]
                    sb.append(if (p[0] == 'w') letter else letter.lowercaseChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        val cs = buildString {
            if (castling["wK"] == true) append('K'); if (castling["wQ"] == true) append('Q')
            if (castling["bK"] == true) append('k'); if (castling["bQ"] == true) append('q')
        }.ifEmpty { "-" }
        val ep = enPassant ?: "-"
        return "$sb $turn $cs $ep $halfClock $fullMove"
    }

    // ------- Helpers -------
    private fun inBounds(f: Int, r: Int) = f in 0..7 && r in 0..7
    private fun color(p: String?) = p?.get(0)
    private fun type(p: String?) = p?.get(1)

    fun sqToCoord(sq: String): Pos {
        val f = sq[0] - 'a'; val r = sq.substring(1).toInt() - 1
        return Pos(f, r)
    }
    fun coordToSq(f: Int, r: Int) = "${'a' + f}${r + 1}"

    // ------- Attack detection -------
    private fun isAttacked(b: Array<Array<String?>>, f: Int, r: Int, by: Char): Boolean {
        // Pawn attacks
        val pr = if (by == 'w') r - 1 else r + 1
        for (df in intArrayOf(-1, 1)) {
            val sf = f + df
            if (inBounds(sf, pr)) {
                val p = b[pr][sf]
                if (p != null && color(p) == by && type(p) == 'P') return true
            }
        }
        // Knight
        for ((df, dr) in knightOff) {
            val sf = f + df; val sr = r + dr
            if (inBounds(sf, sr)) {
                val p = b[sr][sf]
                if (p != null && color(p) == by && type(p) == 'N') return true
            }
        }
        // King
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            val sf = f + df; val sr = r + dr
            if (inBounds(sf, sr)) {
                val p = b[sr][sf]
                if (p != null && color(p) == by && type(p) == 'K') return true
            }
        }
        // Diagonal (B/Q)
        for ((df, dr) in diagDirs) {
            var sf = f + df; var sr = r + dr
            while (inBounds(sf, sr)) {
                val p = b[sr][sf]
                if (p != null) {
                    if (color(p) == by && type(p) in listOf('B', 'Q')) return true
                    break
                }
                sf += df; sr += dr
            }
        }
        // Orthogonal (R/Q)
        for ((df, dr) in orthDirs) {
            var sf = f + df; var sr = r + dr
            while (inBounds(sf, sr)) {
                val p = b[sr][sf]
                if (p != null) {
                    if (color(p) == by && type(p) in listOf('R', 'Q')) return true
                    break
                }
                sf += df; sr += dr
            }
        }
        return false
    }

    private fun findKing(b: Array<Array<String?>>, col: Char): Pos? {
        for (r in 0..7) for (f in 0..7) {
            val p = b[r][f]
            if (p != null && color(p) == col && type(p) == 'K') return Pos(f, r)
        }
        return null
    }

    fun isInCheck(col: Char): Boolean {
        val k = findKing(board, col) ?: return false
        val enemy = if (col == 'w') 'b' else 'w'
        return isAttacked(board, k.file, k.rank, enemy)
    }

    // ------- Move generation -------
    private fun genPieceMoves(f: Int, r: Int): List<ChessMove> {
        val piece = board[r][f] ?: return emptyList()
        val col = color(piece)!!; val pt = type(piece)!!
        val moves = mutableListOf<ChessMove>()

        fun add(tf: Int, tr: Int, promo: Char? = null, ep: Boolean = false,
                castle: Char? = null, dp: Boolean = false) {
            moves.add(ChessMove(Pos(f, r), Pos(tf, tr), promo, ep, castle, dp))
        }

        when (pt) {
            'P' -> {
                val dir = if (col == 'w') 1 else -1
                val start = if (col == 'w') 1 else 6
                val promoR = if (col == 'w') 7 else 0
                // Forward
                if (inBounds(f, r + dir) && board[r + dir][f] == null) {
                    if (r + dir == promoR) {
                        for (pr in charArrayOf('Q', 'R', 'B', 'N')) add(f, r + dir, promo = pr)
                    } else {
                        add(f, r + dir)
                        if (r == start && board[r + 2 * dir][f] == null)
                            add(f, r + 2 * dir, dp = true)
                    }
                }
                // Captures
                for (df in intArrayOf(-1, 1)) {
                    val tf = f + df; val tr = r + dir
                    if (!inBounds(tf, tr)) continue
                    val target = board[tr][tf]
                    if (target != null && color(target) != col) {
                        if (tr == promoR) {
                            for (pr in charArrayOf('Q', 'R', 'B', 'N')) add(tf, tr, promo = pr)
                        } else add(tf, tr)
                    } else if (target == null && enPassant != null) {
                        val ep = sqToCoord(enPassant!!)
                        if (ep.file == tf && ep.rank == tr) add(tf, tr, ep = true)
                    }
                }
            }
            'N' -> {
                for ((df, dr) in knightOff) {
                    val tf = f + df; val tr = r + dr
                    if (!inBounds(tf, tr)) continue
                    val t = board[tr][tf]
                    if (t == null || color(t) != col) add(tf, tr)
                }
            }
            'B', 'R', 'Q' -> {
                val dirs = mutableListOf<Pair<Int, Int>>()
                if (pt == 'B' || pt == 'Q') dirs.addAll(diagDirs)
                if (pt == 'R' || pt == 'Q') dirs.addAll(orthDirs)
                for ((df, dr) in dirs) {
                    var tf = f + df; var tr = r + dr
                    while (inBounds(tf, tr)) {
                        val t = board[tr][tf]
                        if (t == null) { add(tf, tr) }
                        else { if (color(t) != col) add(tf, tr); break }
                        tf += df; tr += dr
                    }
                }
            }
            'K' -> {
                for (df in -1..1) for (dr in -1..1) {
                    if (df == 0 && dr == 0) continue
                    val tf = f + df; val tr = r + dr
                    if (!inBounds(tf, tr)) continue
                    val t = board[tr][tf]
                    if (t == null || color(t) != col) add(tf, tr)
                }
                // Castling
                val home = if (col == 'w') 0 else 7
                val enemy = if (col == 'w') 'b' else 'w'
                if (r == home && f == 4 && !isAttacked(board, 4, home, enemy)) {
                    val kSide = if (col == 'w') "wK" else "bK"
                    val qSide = if (col == 'w') "wQ" else "bQ"
                    if (castling[kSide] == true &&
                        board[home][5] == null && board[home][6] == null &&
                        board[home][7] == "${col}R" &&
                        !isAttacked(board, 5, home, enemy) &&
                        !isAttacked(board, 6, home, enemy)) {
                        add(6, home, castle = 'K')
                    }
                    if (castling[qSide] == true &&
                        board[home][1] == null && board[home][2] == null && board[home][3] == null &&
                        board[home][0] == "${col}R" &&
                        !isAttacked(board, 3, home, enemy) &&
                        !isAttacked(board, 2, home, enemy)) {
                        add(2, home, castle = 'Q')
                    }
                }
            }
        }
        return moves
    }

    private fun cloneBoard(b: Array<Array<String?>>): Array<Array<String?>> =
        Array(8) { b[it].clone() }

    private fun simulate(b: Array<Array<String?>>, m: ChessMove): Array<Array<String?>> {
        val nb = cloneBoard(b)
        val piece = nb[m.from.rank][m.from.file]!!
        val col = color(piece)!!
        if (m.isEnPassant) nb[m.from.rank][m.to.file] = null
        nb[m.to.rank][m.to.file] = if (m.promotion != null) "$col${m.promotion}" else piece
        nb[m.from.rank][m.from.file] = null
        if (m.isCastle == 'K') {
            nb[m.from.rank][5] = nb[m.from.rank][7]; nb[m.from.rank][7] = null
        } else if (m.isCastle == 'Q') {
            nb[m.from.rank][3] = nb[m.from.rank][0]; nb[m.from.rank][0] = null
        }
        return nb
    }

    private fun isSafe(m: ChessMove): Boolean {
        val piece = board[m.from.rank][m.from.file] ?: return false
        val col = color(piece)!!
        val sim = simulate(board, m)
        val k = findKing(sim, col) ?: return false
        val enemy = if (col == 'w') 'b' else 'w'
        return !isAttacked(sim, k.file, k.rank, enemy)
    }

    fun legalMoves(col: Char = turn): List<ChessMove> {
        val all = mutableListOf<ChessMove>()
        for (r in 0..7) for (f in 0..7) {
            val p = board[r][f] ?: continue
            if (color(p) != col) continue
            for (m in genPieceMoves(f, r)) {
                if (isSafe(m)) all.add(m)
            }
        }
        return all
    }

    fun legalMovesFrom(f: Int, r: Int): List<ChessMove> {
        val p = board[r][f] ?: return emptyList()
        if (color(p) != turn) return emptyList()
        return genPieceMoves(f, r).filter { isSafe(it) }
    }

    fun findLegalMove(fromSq: String, toSq: String, promo: Char? = null): ChessMove? {
        val from = sqToCoord(fromSq); val to = sqToCoord(toSq)
        return legalMoves().firstOrNull { m ->
            m.from == from && m.to == to &&
                    (m.promotion == null || m.promotion == (promo ?: 'Q'))
        }
    }

    // ------- Apply move -------
    fun applyMove(m: ChessMove) {
        val piece = board[m.from.rank][m.from.file]!!
        val col = color(piece)!!; val pt = type(piece)!!
        val captured = board[m.to.rank][m.to.file]
        val isPawn = pt == 'P'
        val isCapture = captured != null || m.isEnPassant

        if (m.isEnPassant) board[m.from.rank][m.to.file] = null
        board[m.to.rank][m.to.file] = if (m.promotion != null) "$col${m.promotion}" else piece
        board[m.from.rank][m.from.file] = null

        if (m.isCastle == 'K') {
            board[m.from.rank][5] = board[m.from.rank][7]; board[m.from.rank][7] = null
        } else if (m.isCastle == 'Q') {
            board[m.from.rank][3] = board[m.from.rank][0]; board[m.from.rank][0] = null
        }

        // Update castling rights
        if (pt == 'K') { castling["${col}K"] = false; castling["${col}Q"] = false }
        if (pt == 'R') {
            if (col == 'w' && m.from.rank == 0 && m.from.file == 0) castling["wQ"] = false
            if (col == 'w' && m.from.rank == 0 && m.from.file == 7) castling["wK"] = false
            if (col == 'b' && m.from.rank == 7 && m.from.file == 0) castling["bQ"] = false
            if (col == 'b' && m.from.rank == 7 && m.from.file == 7) castling["bK"] = false
        }
        if (captured != null && type(captured) == 'R') {
            val tr = m.to.rank; val tf = m.to.file
            if (tr == 0 && tf == 0) castling["wQ"] = false
            if (tr == 0 && tf == 7) castling["wK"] = false
            if (tr == 7 && tf == 0) castling["bQ"] = false
            if (tr == 7 && tf == 7) castling["bK"] = false
        }

        enPassant = if (m.isDoublePush) {
            coordToSq(m.from.file, (m.from.rank + m.to.rank) / 2)
        } else null

        halfClock = if (isPawn || isCapture) 0 else halfClock + 1
        if (col == 'b') fullMove++
        turn = if (col == 'w') 'b' else 'w'

        history.add(mapOf(
            "from" to coordToSq(m.from.file, m.from.rank),
            "to" to coordToSq(m.to.file, m.to.rank),
            "piece" to pt, "color" to col,
            "captured" to (if (captured != null) type(captured) else if (m.isEnPassant) 'P' else null),
            "promotion" to m.promotion, "castle" to m.isCastle
        ))
    }

    // ------- Game status -------
    fun getStatus(): GameStatus {
        val inCheck = isInCheck(turn)
        val legal = legalMoves()
        val noMoves = legal.isEmpty()
        val checkmate = inCheck && noMoves
        val stalemate = !inCheck && noMoves

        val result = when {
            checkmate -> "checkmate"
            stalemate -> "stalemate"
            halfClock >= 100 -> "fifty_move"
            else -> null
        }
        val winner = if (checkmate) (if (turn == 'w') 'b' else 'w') else null

        return GameStatus(inCheck, checkmate, stalemate, result != null, result, winner, turn)
    }

    // Piece at position (for UI)
    fun pieceAt(file: Int, rank: Int): String? = board[rank][file]
}
