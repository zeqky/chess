package io.github.zeqky.chess.core

enum class PieceType(val num: Int, vararg val movement: Movement) {
    PAWN(1, Movement.PAWN),
    ROOK(2, Movement.VERTICAL),
    BISHOP(3, Movement.DIAGONAL),
    KNIGHT(4, Movement.KNIGHT),
    QUEEN(5, Movement.VERTICAL, Movement.DIAGONAL),
    KING(6, Movement.KING),
    EMPTY(0)
}

enum class Movement {
    PAWN,
    VERTICAL,
    DIAGONAL,
    KNIGHT,
    KING
}