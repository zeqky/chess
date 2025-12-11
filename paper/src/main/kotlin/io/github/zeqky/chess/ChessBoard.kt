package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ChessBoard(val name: String, private val board: Board, val a1: Location) {

    fun init() {
        board.pieces.forEach {
            val loc = a1.clone().add(it.square.y - 1.0, 0.0, it.square.x - 1.0)
            ChessManager.fakeEntityServer.spawnEntity(loc, ArmorStand::class.java).apply {
                updateMetadata {
                    customName(text(it.pieceType.toString()).color(
                        if(it.isWhite) NamedTextColor.WHITE else NamedTextColor.BLACK
                    ))
                    isCustomNameVisible = true
                }
            }
        }
    }
}