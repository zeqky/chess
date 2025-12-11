package io.github.zeqky.chess.core.dialog

abstract class GameDialog<R> {
    lateinit var default: () -> R
        private set
}

class ChessPieceMove(val isWhiteTurn: Boolean) : GameDialog<String>()