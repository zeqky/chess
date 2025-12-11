package io.github.zeqky.chess.core

class Piece(val pieceType: PieceType, val isWhite: Boolean, square: Square) {
    var square: Square = square
        internal set

    fun moveTo(nextSquare: Square) {

    }
}