package io.github.zeqky.chess.core

data class MoveState(
    val move: String,
    val from: Square,
    val to: Square,

    val movedPiece: Piece,

    val capturedPiece: Piece?,
    val capturedSquare: Square?,

    val wasEnPassant: Boolean,

    val wasCastle: Boolean,
    val rook: Piece?,
    val rookFrom: Square?,
    val rookTo: Square?,

    val previousCastlingRights: String,
    val previousEnPassantSquare: Square?,
    val previousEnPassantPawn: Piece?,
    val previousHalfmoveClock: String,
    val previousFullmoveNumber: String,
    val previousTurn: Boolean,

    val promotedFrom: PieceType?
)
