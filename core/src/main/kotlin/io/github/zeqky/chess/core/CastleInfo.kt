package io.github.zeqky.chess.core

data class CastleInfo(
    val kingFrom: Square,
    val kingTo: Square,
    val rookFrom: Square,
    val rookTo: Square,
    val right: Char
)

val whiteKingSide = CastleInfo(
    Square(5, 1), Square(7, 1),
    Square(8, 1), Square(6, 1),
    'K'
)

val whiteQueenSide = CastleInfo(
    Square(5, 1), Square(3, 1),
    Square(1, 1), Square(4, 1),
    'Q'
)

val blackKingSide = CastleInfo(
    Square(5, 8), Square(7, 8),
    Square(8, 8), Square(6, 8),
    'k'
)

val blackQueenSide = CastleInfo(
    Square(5, 8), Square(3, 8),
    Square(1, 8), Square(4, 8),
    'q'
)
