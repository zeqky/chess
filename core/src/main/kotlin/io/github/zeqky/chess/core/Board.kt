package io.github.zeqky.chess.core

class Board {
    val squares = arrayListOf<Square>()
    var startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    var placement = ""
    var side = ""
    var castlingRights = ""
    var enpassantTarget = ""
    var halfmoveClock = ""
    var fullmoveNumber = ""
    fun setup() {
        //square Setup
        for (x in 1..8) {
            for (y in 1..8) {
                squares += Square(x, y)
            }
        }
        loadFen(startFen)
        setupPieces()
    }

    private fun setupPieces() {
        val ranks = placement.split("/")
        for ((rankIndex, rankString) in ranks.withIndex()) {
            println(rankIndex)
            var file = 0
            for (ch in rankString) {
                when {
                    ch.isDigit() -> file += ch.digitToInt()
                    else -> {
                        val s = squares.first { it.y == 8 - rankIndex && it.x == file + 1 }
                        s.pieceType = when (ch.lowercase()) {
                            "p" -> PieceType.PAWN
                            "r" -> PieceType.ROOK
                            "n" -> PieceType.KNIGHT
                            "b" -> PieceType.BISHOP
                            "q" -> PieceType.QUEEN
                            "k" -> PieceType.KING
                            else -> PieceType.EMPTY
                        }
                        s.isWhite = ch.isUpperCase()
                        file++
                    }
                }
            }
        }
    }

    fun loadFen(fen: String) {
        val f = fen.split(" ")
        placement = f[0]
        side = f[1]
        castlingRights = f[2]
        enpassantTarget = f[3]
        halfmoveClock = f[4]
        fullmoveNumber = f[5]
    }
}