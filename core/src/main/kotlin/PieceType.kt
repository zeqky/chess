enum class PieceType(val num: Int, vararg val movement: Movement) {
    PAWN(0, Movement.PAWN),
    ROOK(1, Movement.VERTICAL),
    BISHOP(2, Movement.DIAGONAL),
    NIGHT(3, Movement.KNIGHT),
    QUEEN(4, Movement.VERTICAL, Movement.DIAGONAL),
    KING(5, Movement.KING)
}

enum class Movement {
    PAWN,
    VERTICAL,
    DIAGONAL,
    KNIGHT,
    KING
}