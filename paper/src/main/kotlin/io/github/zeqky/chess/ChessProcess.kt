package io.github.zeqky.chess

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.Game

class ChessProcess {
    lateinit var game: Game
        private set

    lateinit var eventListener: GameEventListener

    private val scope = HeartbeatScope()


    fun start(board: Board) {
        game = Game(board)
        eventListener = GameEventListener(this)
        game.launch(scope)
    }
}