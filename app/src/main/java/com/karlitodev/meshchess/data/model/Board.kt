package com.karlitodev.meshchess.data.model

data class Board(
    val fen: String,
    val turn: PlayerColor,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isCheck: Boolean = false,
    val winner: PlayerColor? = null
)
