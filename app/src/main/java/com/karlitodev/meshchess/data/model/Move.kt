package com.karlitodev.meshchess.data.model

data class Move(
    val from: String, // e.g., "e2"
    val to: String,   // e.g., "e4"
    val promotion: String? = null // "Q", "R", "B", "N"
) {
    override fun toString(): String {
        return if (promotion != null) "$from$to$promotion" else "$from$to"
    }
}
