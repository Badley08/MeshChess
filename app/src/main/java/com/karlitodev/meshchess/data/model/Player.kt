package com.karlitodev.meshchess.data.model

data class Player(
    val id: String,
    val name: String,
    val color: PlayerColor
)

enum class PlayerColor {
    WHITE, BLACK
}
