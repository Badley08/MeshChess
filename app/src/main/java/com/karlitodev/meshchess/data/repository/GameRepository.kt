package com.karlitodev.meshchess.data.repository

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.karlitodev.meshchess.data.model.Board
import com.karlitodev.meshchess.data.model.Move
import com.karlitodev.meshchess.data.model.PlayerColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository {
    private val python: Python = Python.getInstance()
    // Use the correctly-named module path (underscores, not hyphens)
    private val chessRulesModule: PyObject = python.getModule("engine.chess_engine.chess_rules")

    private var gameState: PyObject? = null

    private val _boardState = MutableStateFlow(Board("", PlayerColor.WHITE))
    val boardState: StateFlow<Board> = _boardState.asStateFlow()

    init {
        startNewGame()
    }

    fun startNewGame() {
        gameState = chessRulesModule.callAttr("create_game_state")
        updateBoardState()
    }

    fun makeMove(move: Move): Boolean {
        val state = gameState ?: return false

        // Find if move is legal
        val legalMove = chessRulesModule.callAttr("find_legal_move", state, move.from, move.to, move.promotion)
            ?: return false

        // Apply move
        chessRulesModule.callAttr("apply_move", state, legalMove)
        updateBoardState()
        return true
    }

    private fun updateBoardState() {
        val state = gameState ?: return
        val fen = chessRulesModule.callAttr("to_fen", state).toString()
        val statusDict = chessRulesModule.callAttr("get_game_status", state)

        val isCheckmate = statusDict["checkmate"]?.toBoolean() ?: false
        val isStalemate = statusDict["stalemate"]?.toBoolean() ?: false
        val isCheck = statusDict["inCheck"]?.toBoolean() ?: false
        val turnStr = statusDict["turn"]?.toString() ?: "w"

        var winner: PlayerColor? = null
        if (isCheckmate) {
            val winnerStr = statusDict["winner"]?.toString()
            winner = if (winnerStr == "w") PlayerColor.WHITE else PlayerColor.BLACK
        }

        _boardState.value = Board(
            fen = fen,
            turn = if (turnStr == "w") PlayerColor.WHITE else PlayerColor.BLACK,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isCheck = isCheck,
            winner = winner
        )
    }
}
