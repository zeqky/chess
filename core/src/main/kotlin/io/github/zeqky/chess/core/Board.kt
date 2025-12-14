package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.api.CBoard

class Board {
    val squares = arrayListOf<Square>()
    val pieces = arrayListOf<Piece>()
    var startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    var placement = ""
    var side = ""
    var castlingRights = ""
    var enpassantTarget = ""
    var halfmoveClock = ""
    var fullmoveNumber = ""
    var isWhiteTurn = true

    lateinit var cBoard: CBoard

    fun attach(cb: CBoard) {
        cBoard = cb
    }

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
            var file = 0
            for (ch in rankString) {
                when {
                    ch.isDigit() -> file += ch.digitToInt()
                    else -> {
                        val square = squares.first { it.y == 8 - rankIndex && it.x == file + 1 }
                        val pieceType = when (ch.lowercase()) {
                            "p" -> PieceType.PAWN
                            "r" -> PieceType.ROOK
                            "n" -> PieceType.KNIGHT
                            "b" -> PieceType.BISHOP
                            "q" -> PieceType.QUEEN
                            "k" -> PieceType.KING
                            else -> PieceType.EMPTY
                        }
                        val isWhite = ch.isUpperCase()

                        pieces += Piece(pieceType, isWhite, square)
                        file++
                    }
                }
            }
        }
    }

    fun showupBoard() {
        for (y in 8 downTo 1) { // 체스판은 일반적으로 8→1
            val line = StringBuilder()
            for (x in 1..8) {
                line.append(if (getPiece(x, y) != null) "p " else "n ")
            }
            cBoard.print(line.toString())
            // 또는 서버 전체 브로드캐스트
            // Bukkit.broadcast(Component.text(line.toString(), NamedTextColor.WHITE))
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

    fun inputMove(move: String) {
        if (!checkMoveValid(move)) return

        val from = parseSquare(move[0], move[1])
        val to   = parseSquare(move[2], move[3])

        val piece = getPiece(from.x, from.y) ?: return

        if (isWhiteTurn != piece.isWhite) return

        // 1️⃣ 도착 칸에 말이 있으면 (상대 말) → 잡기
        if (getPiece(to.x, to.y) != null) {
            removePiece(to)
        }

        // 2️⃣ 말 이동
        movePiece(to, piece)

        // 3️⃣ 턴 변경
        isWhiteTurn = !isWhiteTurn
    }

    fun removePiece(target: Square) {
        require(pieces.any { it.square.x == target.x && it.square.y == target.y }) {
            cBoard.print("Pieces Cannot Be Null!")
        }
        val piece = pieces.first { it.square.x == target.x && it.square.y == target.y }
        piece.cPiece.despawn()
        pieces.remove(piece)
    }

    fun movePiece(to: Square, piece: Piece) {
        piece.moveTo(to)
    }

    private fun parseSquare(file: Char, rank: Char): Square {
        val x = file - 'a' + 1   // a~h → 1~8
        val y = rank - '0'       // '1'~'8' → 1~8
        return Square(x, y)
    }

    private fun checkMoveValid(move: String): Boolean {
        if(move.length != 4) return false
        val from = parseSquare(move[0], move[1])
        val to = parseSquare(move[2], move[3])
        val piece = getPiece(from.x, from.y) ?: return false
        val sqs = getAvailableSquares(piece.pieceType, squares.first { it.x == from.x && it.y == from.y}, piece.isWhite)
        return sqs.any { it.x == to.x && it.y == to.y }
    }

    fun getPiece(x: Int, y: Int): Piece? {
        if (pieces.none { it.square.x == x && it.square.y == y }) return null
        return pieces.first { it.square.x == x && it.square.y == y }
    }

    private fun getAvailableSquares(pieceType: PieceType, sq: Square, isWhite: Boolean): List<Square> {
        return when (pieceType) {
            PieceType.PAWN -> pawnMoves(sq, isWhite)
            PieceType.KING -> kingMoves(sq, isWhite)
            PieceType.QUEEN -> slidingMoves(sq, queenDirs, isWhite)
            PieceType.ROOK -> slidingMoves(sq, rookDirs, isWhite)
            PieceType.BISHOP -> slidingMoves(sq, bishopDirs, isWhite)
            PieceType.KNIGHT -> knightMoves(sq, isWhite)
            PieceType.EMPTY -> emptyList()
        }
    }

    private fun isInBoard(x: Int, y: Int) = x in 1..8 && y in 1..8

    private fun pawnMoves(sq: Square, isWhite: Boolean): List<Square> {
        val moves = mutableListOf<Square>()
        val dir = if (isWhite) 1 else -1
        val ny = sq.y + dir

        // 전진 1칸
        if (isInBoard(sq.x, ny) && getPiece(sq.x, ny) == null) {
            moves += Square(sq.x, ny)

            // 처음 2칸 이동
            val startRow = if (isWhite) 2 else 7
            val ny2 = sq.y + dir * 2
            if (sq.y == startRow && getPiece(sq.x, ny) == null && getPiece(sq.x, ny2) == null) {
                moves += Square(sq.x, ny2)
            }
        }

        // 대각선 캡처
        for (dx in listOf(-1, 1)) {
            val nx = sq.x + dx
            if (!isInBoard(nx, ny)) continue
            val targetPiece = getPiece(nx, ny)
            if (targetPiece != null && targetPiece.isWhite != isWhite) {
                moves += Square(nx, ny)
            }
        }

        return moves
    }

    private fun kingMoves(sq: Square, isWhite: Boolean): List<Square> {
        val moves = mutableListOf<Square>()
        val dirs = listOf(
            -1 to -1, 0 to -1, 1 to -1,
            -1 to  0,          1 to  0,
            -1 to  1, 0 to  1, 1 to  1
        )

        for ((dx, dy) in dirs) {
            val nx = sq.x + dx
            val ny = sq.y + dy
            val target = Square(nx, ny)

            if (isInBoard(nx, ny) && !checkCollision(target, isWhite)) {
                moves += target
            }
        }
        return moves
    }


    private fun knightMoves(sq: Square, isWhite: Boolean): List<Square> {
        val moves = mutableListOf<Square>()
        val dirs = listOf(
            -2 to -1, -2 to 1,
            -1 to -2, -1 to 2,
            1 to -2,  1 to 2,
            2 to -1,  2 to 1
        )

        for ((dx, dy) in dirs) {
            val nx = sq.x + dx
            val ny = sq.y + dy
            val target = Square(nx, ny)

            if (isInBoard(nx, ny) && !checkCollision(target, isWhite)) {
                moves += target
            }
        }
        return moves
    }


    private val rookDirs = listOf(
        1 to 0, -1 to 0, 0 to 1, 0 to -1
    )
    private val bishopDirs = listOf(
        1 to 1, 1 to -1, -1 to 1, -1 to -1
    )
    private val queenDirs = rookDirs + bishopDirs
    private fun slidingMoves(
        sq: Square,
        dirs: List<Pair<Int, Int>>,
        isWhite: Boolean
    ): List<Square> {

        val moves = mutableListOf<Square>()

        for ((dx, dy) in dirs) {
            var x = sq.x + dx
            var y = sq.y + dy

            while (isInBoard(x, y)) {
                val target = Square(x, y)
                val piece = getPiece(target.x, target.y)

                if (piece == null) {
                    // 빈 칸
                    moves += target
                } else {
                    // 말이 있음
                    if (piece.isWhite != isWhite) {
                        // 상대 말 → 잡기 가능
                        moves += target
                    }
                    break // 아군/상대 상관없이 중단
                }

                x += dx
                y += dy
            }
        }
        return moves
    }

    private fun checkCollision(target: Square, isWhite: Boolean): Boolean {
        val piece = getPiece(target.x, target.y)
        return if (piece == null) {
            false
        } else {
            piece.isWhite == isWhite
        }
    }
}