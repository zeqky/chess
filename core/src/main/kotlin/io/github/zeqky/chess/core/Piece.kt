package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.api.CPiece

class Piece(val pieceType: PieceType, val isWhite: Boolean, square: Square) {
    var square: Square = square
        internal set
    lateinit var cPiece: CPiece
        private set

    fun moveTo(nextSquare: Square) {
        square = nextSquare
        cPiece.moveTo(nextSquare)
    }

    fun attach(cPiece: CPiece) {
        this.cPiece = cPiece
    }
}