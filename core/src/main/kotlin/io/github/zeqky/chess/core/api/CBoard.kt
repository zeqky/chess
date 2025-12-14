package io.github.zeqky.chess.core.api

import io.github.zeqky.chess.core.Piece

open class CBoard {
    open fun print(string: String) {}
    open fun spawn(piece: Piece) {}
}