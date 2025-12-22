package io.github.zeqky.chess.plugin

import io.github.zeqky.chess.ChessBoard
import io.github.zeqky.chess.ChessGame
import io.github.zeqky.chess.ChessManager
import io.github.zeqky.chess.ChessProcess
import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.GameCommand
import io.github.zeqky.fount.fake.PlayerInteractFakeEntityEvent
import io.github.zeqky.fount.kommand.KommandArgument.Companion.dynamic
import io.github.zeqky.fount.kommand.getValue
import io.github.zeqky.fount.kommand.kommand
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ChessKommand {
    val boards = dynamic { _, value ->
        ChessManager.boards.first { it.name == value }
    }.apply {
        suggests {
            ChessManager.boards.forEach { suggest(it.name) }
        }
    }

    fun register(plugin: ChessPlugin) {
        plugin.kommand {
            register("chess") {
                then("board") {
                    then("create", "name" to string()) {
                        requires {
                            sender is Player
                        }
                        executes {
                            val name: String by it
                            ChessManager.createBoard(name, (sender as Player).location)
                        }
                    }

                    then("move", "board" to boards, "moves" to string()) {
                        executes {
                            val board: ChessBoard by it
                            val moves: String by it
                            board.board.game?.send(GameCommand.PieceMove(moves))
                        }
                    }

                    then("undo", "board" to boards) {
                        executes {
                            val board: ChessBoard by it
                            board.board.game?.send(GameCommand.Undo)
                        }
                    }

                    then("redo", "board" to boards) {
                        executes {
                            val board: ChessBoard by it
                            board.board.game?.send(GameCommand.Redo)
                        }
                    }

                    then("reset", "board" to boards) {
                        executes {
                            val board: ChessBoard by it
                            ChessManager.resetBoard(board.name)
                        }
                    }

                    then("start",
                        "board" to boards,
                        "white" to player(),
                        "black" to player()) {
                        executes {
                            val board: ChessBoard by it
                            val white: Player by it
                            val black: Player by it
                            ChessManager.setupBoard(board)
                            start(board.board, white, black)
                        }

                        then("tc" to string()) {
                            executes {

                            }
                        }
                    }
                }
            }
        }
    }

    private fun start(board: Board, white: Player, black: Player) {
        board.attachment<ChessBoard>().apply {
            setWhite(white)
            setBlack(black)
        }
        ChessProcess().start(board)
    }
}