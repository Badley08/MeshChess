package com.karlitodev.meshchess.engine

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class UIState(
    val board: Array<Array<String?>>,
    val turn: Char,
    val status: GameStatus,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val selectedSquare: Pos? = null,
    val isVsAI: Boolean = false,
    val aiIsThinking: Boolean = false
)

class GameViewModel : ViewModel() {
    private val engine = ChessEngine()
    private var geminiAgent: GeminiAgent? = null
    private var isVsAI = false

    private val _uiState = MutableStateFlow(buildUIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    fun setGeminiAgent(agent: GeminiAgent?) {
        this.geminiAgent = agent
        this.isVsAI = agent != null
        _uiState.value = buildUIState()
    }

    private fun buildUIState(selected: Pos? = null, aiThinking: Boolean = false): UIState {
        val boardCopy = Array(8) { r -> Array(8) { c -> engine.board[r][c] } }
        val moves = if (selected != null) engine.legalMovesFrom(selected.file, selected.rank) else emptyList()
        return UIState(
            board = boardCopy,
            turn = engine.turn,
            status = engine.getStatus(),
            legalMovesForSelected = moves,
            selectedSquare = selected,
            isVsAI = isVsAI,
            aiIsThinking = aiThinking
        )
    }

    fun onSquareClicked(f: Int, r: Int) {
        if (_uiState.value.aiIsThinking) return
        if (isVsAI && engine.turn == 'b') return // Block user input when it's AI's turn

        val currentSelected = _uiState.value.selectedSquare
        if (currentSelected != null) {
            if (currentSelected.file == f && currentSelected.rank == r) {
                _uiState.value = buildUIState(null)
                return
            }

            val move = _uiState.value.legalMovesForSelected.find { it.to.file == f && it.to.rank == r }
            if (move != null) {
                engine.applyMove(move)
                _uiState.value = buildUIState(null)
                
                if (isVsAI && !engine.getStatus().gameOver) {
                    triggerAIMove()
                }
                return
            }
        }

        val piece = engine.pieceAt(f, r)
        if (piece != null && piece[0] == engine.turn) {
            _uiState.value = buildUIState(Pos(f, r))
        } else {
            _uiState.value = buildUIState(null)
        }
    }
    
    private fun triggerAIMove() {
        if (geminiAgent == null) return
        _uiState.value = buildUIState(null, aiThinking = true)
        
        viewModelScope.launch {
            val bestMove = geminiAgent!!.getBestMove(engine.toFen(), engine.legalMoves(), engine)
            if (bestMove != null) {
                engine.applyMove(bestMove)
            }
            _uiState.value = buildUIState(null, aiThinking = false)
        }
    }

    fun restartGame() {
        engine.reset()
        _uiState.value = buildUIState(null)
    }
}
