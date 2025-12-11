package io.github.zeqky.chess.core

class Square(val x: Int, val y: Int) {
    var pieceType: PieceType = PieceType.EMPTY
    var isWhite = true
}