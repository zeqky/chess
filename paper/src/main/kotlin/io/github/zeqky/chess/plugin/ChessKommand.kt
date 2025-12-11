package io.github.zeqky.chess.plugin

import io.github.zeqky.chess.ChessGame
import io.github.zeqky.chess.core.Board
import io.github.zeqky.fount.kommand.getValue
import io.github.zeqky.fount.kommand.kommand
import net.kyori.adventure.text.Component.text

object ChessKommand {
    lateinit var board: Board
    fun register(plugin: ChessPlugin) {
        plugin.kommand {
            register("chess") {
                then("board") {
                    executes {
                        board = Board()
                        board.setup()
                        feedback(text("Board Created"))
                    }
                }
                then("square") {
                    then("x" to int(1, 8)) {
                        then("y" to int(1, 8)) {
                            executes {
                                val x: Int by it
                                val y: Int by it
                                val square = board.squares.first { it.x == x && it.y == y }
                                val squareColor = if(square.isWhite) "white" else "black"
                                feedback(text("PieceType: ${square.pieceType}, PieceColor: $squareColor"))
                            }
                        }
                    }
                }
            }
        }
    }
}