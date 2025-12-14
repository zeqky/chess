package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.api.CBoard
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Location

class ChessBoard(val name: String, val board: Board, val a1: Location) : CBoard() {
    var selectedPiece: ChessPiece? = null

    fun init() {
        board.pieces.forEach {
            val loc = a1.clone().add(it.square.y - 1.0, 0.0, it.square.x - 1.0)
            it.attach(ChessPiece(loc, it).apply { spawn() })
        }
        board.attach(this)
    }

    override fun print(string: String) {
        Bukkit.broadcast(text(string))
    }

    override fun spawn(piece: Piece) {
        board.pieces.add(piece)
        val loc = a1.clone().add(piece.square.y - 1.0, 0.0, piece.square.x - 1.0)
        piece.attach(ChessPiece(loc, piece).apply { spawn() })
    }

    fun registerSelection() {

    }
}