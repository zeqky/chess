package io.github.zeqky.chess.plugin

import io.github.zeqky.chess.ChessBoard
import io.github.zeqky.chess.ChessGame
import io.github.zeqky.chess.ChessManager
import io.github.zeqky.chess.core.Board
import io.github.zeqky.fount.kommand.KommandArgument.Companion.dynamic
import io.github.zeqky.fount.kommand.getValue
import io.github.zeqky.fount.kommand.kommand
import net.kyori.adventure.text.Component.text
import org.bukkit.entity.Player

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
                    then("create") {
                        then("name" to string()) {
                            requires {
                                sender is Player
                            }
                            executes {
                                val name: String by it
                                ChessManager.createBoard(name, (sender as Player).location)
                            }
                        }
                    }
                    then("setup") {
                        then("board" to boards) {
                            executes {
                                val board: ChessBoard by it
                                ChessManager.setupBoard(board)
                            }
                        }
                    }

                    then("move") {
                        then("board" to boards) {
                            then("moves" to string()) {
                                executes {
                                    val board: ChessBoard by it
                                    val moves: String by it
                                    board.board.inputMove(moves)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}