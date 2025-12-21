package io.github.zeqky.chess.core.dialog

import io.github.zeqky.chess.core.Piece

abstract class GameDialog<R> {
    lateinit var piece: Piece
        private set

    lateinit var default: () -> R
        private set

    internal fun initialize(piece: Piece, default: () -> R) {
        this.piece = piece
        this.default = default
    }
}