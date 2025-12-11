package io.github.zeqky.chess.plugin

import io.github.zeqky.chess.ChessGame
import io.github.zeqky.fount.kommand.kommand

object ChessKommand {
    lateinit var game: ChessGame
    fun register(plugin: ChessPlugin) {
        plugin.kommand {
            register("chess") {

            }
        }
    }
}