package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.event.PieceMoveEvent

class Piece(val board: Board, val pieceType: PieceType, val isWhite: Boolean, square: Square): Attachable() {
    var square: Square = square
        internal set

    suspend fun moveTo(nextSquare: Square) {
        square = nextSquare
        board.eventAdapter.call(PieceMoveEvent(this, nextSquare))
    }
}