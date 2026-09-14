package com.karlitodev.meshchess.engine

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UIState(
    val board: Array<Array<String?>>,
    val turn: Char,
    val status: GameStatus,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val selectedSquare: Pos? = null
)

class GameViewModel : ViewModel() {
    private val engine = ChessEngine()

    private val _uiState = MutableStateFlow(buildUIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    private fun buildUIState(selected: Pos? = null): UIState {
        // Deep copy board for Compose recomposition
        val boardCopy = Array(8) { r -> Array(8) { c -> engine.board[r][c] } }
        val moves = if (selected != null) engine.legalMovesFrom(selected.file, selected.rank) else emptyList()
        return UIState(
            board = boardCopy,
            turn = engine.turn,
            status = engine.getStatus(),
            legalMovesForSelected = moves,
            selectedSquare = selected
        )
    }

    fun onSquareClicked(f: Int, r: Int) {
        val currentSelected = _uiState.value.selectedSquare
        if (currentSelected != null) {
            if (currentSelected.file == f && currentSelected.rank == r) {
                // Deselect
                _uiState.value = buildUIState(null)
                return
            }

            // Check if it's a valid move
            val move = _uiState.value.legalMovesForSelected.find { it.to.file == f && it.to.rank == r }
            if (move != null) {
                // Apply move. If promotion is needed, we'd ideally show a dialog, but default to Q for now
                // In a real app we'd pause and ask the user.
                engine.applyMove(move)
                _uiState.value = buildUIState(null)
                return
            }
        }

        // Select a piece if it belongs to the current turn
        val piece = engine.pieceAt(f, r)
        if (piece != null && piece[0] == engine.turn) {
            _uiState.value = buildUIState(Pos(f, r))
        } else {
            _uiState.value = buildUIState(null) // Deselect on invalid click
        }
    }

    fun restartGame() {
        engine.reset()
        _uiState.value = buildUIState(null)
    }
}
