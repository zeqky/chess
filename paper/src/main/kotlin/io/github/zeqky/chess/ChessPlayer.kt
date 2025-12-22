package io.github.zeqky.chess

import org.bukkit.entity.Player

class ChessPlayer(val isWhite: Boolean, val isAI: Boolean) {
    var bukkitPlayer: Player? = null

    val name: String
        get() {
            return if (bukkitPlayer != null) {
                bukkitPlayer!!.name
            } else {
                "AI"
            }
        }
}