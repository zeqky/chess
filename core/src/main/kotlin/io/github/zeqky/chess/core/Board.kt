package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.event.GameEventAdapter
import io.github.zeqky.chess.core.event.PieceDespawnEvent
import io.github.zeqky.chess.core.event.PieceSpawnEvent
import io.github.zeqky.chess.core.exception.CheckMateException
import io.github.zeqky.chess.core.exception.FiftyMoveException
import io.github.zeqky.chess.core.exception.StaleMateException
import io.github.zeqky.chess.core.exception.ThreefoldException

class Board : Attachable() {
    val squares = arrayListOf<Square>()
    val pieces = arrayListOf<Piece>()
    var startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    var placement = ""
    var castlingRights = ""
    var halfmoveClock = ""
    var fullmoveNumber = ""
    var isWhiteTurn = true

    var enPassantSquare: Square? = null
    var enPassantPawn: Piece? = null
    val undoStack = ArrayDeque<MoveState>()
    val redoStack = ArrayDeque<MoveState>()
    val positionCount = HashMap<String, Int>()

    lateinit var eventAdapter: GameEventAdapter

    var game: Game? = null

    fun setup() {
        for (x in 1..8) {
            for (y in 1..8) {
                squares += Square(x, y)
            }
        }
        eventAdapter = GameEventAdapter()
        loadFen(startFen)
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
                        pieces += Piece(this, pieceType, isWhite, square)
                        file++
                    }
                }
            }
        }
    }

    fun loadFen(fen: String) {
        val f = fen.split(" ")
        placement = f[0]
        isWhiteTurn = f[1] == "w"
        castlingRights = f[2]
        enPassantSquare = if (f[3] == "-") null else parseSquare(f[3][0], f[3][1])
        halfmoveClock = f[4]
        fullmoveNumber = f[5]

        setupPieces()
    }

    suspend fun reset() {
        startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        placement = ""
        castlingRights = ""
        halfmoveClock = ""
        fullmoveNumber = ""
        isWhiteTurn = true
        enPassantSquare = null
        enPassantPawn = null
        pieces.forEach {
            eventAdapter.call(PieceDespawnEvent(it))
            pieces.remove(it)
        }
    }

    suspend fun inputMove(move: String) {
        if (!checkMoveValid(move)) return

        val from = parseSquare(move[0], move[1])
        val to   = parseSquare(move[2], move[3])

        val piece = getPiece(from.x, from.y) ?: return
        if (isWhiteTurn != piece.isWhite) return

        /* =========================
           캡처 / 엔패상 판별
           ========================= */

        var captureOrPawnMove = false
        var wasEnPassant = false

        val capturedPiece: Piece?
        val capturedSquare: Square?

        if (
            piece.pieceType == PieceType.PAWN &&
            enPassantSquare != null &&
            to.x == enPassantSquare!!.x &&
            to.y == enPassantSquare!!.y
        ) {
            capturedSquare = Square(to.x, from.y)
            capturedPiece = getPiece(capturedSquare.x, capturedSquare.y)
            wasEnPassant = true
            captureOrPawnMove = true
        } else {
            capturedSquare = to
            capturedPiece = getPiece(to.x, to.y)
            if (capturedPiece != null) captureOrPawnMove = true
        }

        /* =========================
           캐슬링 판별
           ========================= */

        val isCastle =
            piece.pieceType == PieceType.KING &&
                    kotlin.math.abs(to.x - from.x) == 2

        var rook: Piece? = null
        var rookFrom: Square? = null
        var rookTo: Square? = null

        if (isCastle) {
            val info = when {
                piece.isWhite && to.x == 7 -> whiteKingSide
                piece.isWhite && to.x == 3 -> whiteQueenSide
                !piece.isWhite && to.x == 7 -> blackKingSide
                else -> blackQueenSide
            }
            rook = getPiece(info.rookFrom.x, info.rookFrom.y)
            rookFrom = info.rookFrom
            rookTo = info.rookTo
        }

        /* =========================
           🔥 backup 생성 (모든 상태)
           ========================= */

        val backup = MoveState(
            move = move,
            from = from,
            to = to,
            movedPiece = piece,

            capturedPiece = capturedPiece,
            capturedSquare = capturedSquare,

            wasEnPassant = wasEnPassant,

            wasCastle = isCastle,
            rook = rook,
            rookFrom = rookFrom,
            rookTo = rookTo,

            previousCastlingRights = castlingRights,
            previousEnPassantSquare = enPassantSquare,
            previousEnPassantPawn = enPassantPawn,
            previousHalfmoveClock = halfmoveClock,
            previousFullmoveNumber = fullmoveNumber,
            previousTurn = isWhiteTurn,

            promotedFrom = null
        )

        /* =========================
           실제 이동 처리
           ========================= */

        if (isCastle && rook != null) {
            movePiece(rookTo!!, rook)
        }

        capturedPiece?.let {
            removePiece(capturedSquare)
        }

        movePiece(to, piece)

        /* =========================
           프로모션
           ========================= */

        if (piece.pieceType == PieceType.PAWN) {
            val promotionRank = if (piece.isWhite) 8 else 1
            if (to.y == promotionRank) {
                backup.copy(promotedFrom = PieceType.PAWN)
                handlePromotion(piece)
            }
            captureOrPawnMove = true
        }

        /* =========================
           halfmove / fullmove
           ========================= */

        halfmoveClock =
            if (captureOrPawnMove) "0"
            else (halfmoveClock.toInt() + 1).toString()

        if (!isWhiteTurn) {
            fullmoveNumber = (fullmoveNumber.toInt() + 1).toString()
        }

        /* =========================
           엔패상 상태 갱신
           ========================= */

        enPassantSquare = null
        enPassantPawn = null
        if (piece.pieceType == PieceType.PAWN &&
            kotlin.math.abs(to.y - from.y) == 2
        ) {
            val passedY = (to.y + from.y) / 2
            enPassantSquare = Square(from.x, passedY)
            enPassantPawn = piece
        }

        /* =========================
           턴 전환
           ========================= */

        isWhiteTurn = !isWhiteTurn

        /* =========================
           3회 반복 카운트
           ========================= */
        updatePlacement()

        val key = positionKey()
        positionCount[key] = (positionCount[key] ?: 0) + 1

        if (positionCount[key] == 3) {
            throw ThreefoldException()
        }

        /* =========================
           체크 / 메이트 / 스테일
           ========================= */

        val opponentIsWhite = isWhiteTurn
        val opponentKingSquare = findKing(opponentIsWhite)
        val opponentInCheck =
            isSquareAttacked(opponentKingSquare, !opponentIsWhite)

        val opponentHasMoves = pieces
            .filter { it.isWhite == opponentIsWhite }
            .any { p ->
                getAvailableSquares(p.pieceType, p.square, p.isWhite)
                    .any { target ->
                        checkMoveValid("${p.square.toPos()}${target.toPos()}")
                    }
            }

        when {
            halfmoveClock.toInt() >= 100 ->
                throw FiftyMoveException()
            opponentInCheck && !opponentHasMoves ->
                throw CheckMateException(!opponentIsWhite)
            !opponentInCheck && !opponentHasMoves ->
                throw StaleMateException()
            opponentInCheck ->
                print("Check!")
        }

        /* =========================
           undo / redo 스택
           ========================= */

        undoStack.addLast(backup)
        redoStack.clear()
    }



    suspend fun removePiece(target: Square) {
        val piece = pieces.firstOrNull {
            it.square.x == target.x && it.square.y == target.y
        } ?: return

        if (piece.pieceType == PieceType.ROOK) {
            when (target.x to target.y) {
                1 to 1 -> castlingRights = castlingRights.replace("Q", "")
                8 to 1 -> castlingRights = castlingRights.replace("K", "")
                1 to 8 -> castlingRights = castlingRights.replace("q", "")
                8 to 8 -> castlingRights = castlingRights.replace("k", "")
            }
        }

        eventAdapter.call(PieceDespawnEvent(piece))
        pieces.remove(piece)
    }

    suspend fun movePiece(to: Square, piece: Piece) {
        piece.moveTo(to)
    }

    private fun parseSquare(file: Char, rank: Char): Square {
        val x = file - 'a' + 1
        val y = rank - '0'
        return Square(x, y)
    }

    private fun checkMoveValid(move: String): Boolean {
        if (move.length != 4) return false

        val from = parseSquare(move[0], move[1])
        val to = parseSquare(move[2], move[3])
        val piece = getPiece(from.x, from.y) ?: return false

        val moves = getAvailableSquares(piece.pieceType, piece.square, piece.isWhite)
        if (moves.none { it.x == to.x && it.y == to.y }) return false

        val isCastle =
            piece.pieceType == PieceType.KING &&
                    kotlin.math.abs(to.x - from.x) == 2

        val captured = getPiece(to.x, to.y)
        val epCapturedSquare =
            if (
                piece.pieceType == PieceType.PAWN &&
                enPassantSquare != null &&
                to.x == enPassantSquare!!.x &&
                to.y == enPassantSquare!!.y
            ) Square(to.x, from.y) else null

        val epCaptured = epCapturedSquare?.let { getPiece(it.x, it.y) }

        val originalPieceSquare = piece.square
        var rook: Piece? = null
        var originalRookSquare: Square? = null

        captured?.let { pieces.remove(it) }
        epCaptured?.let { pieces.remove(it) }

        if (isCastle) {
            val info = when {
                piece.isWhite && to.x == 7 -> whiteKingSide
                piece.isWhite && to.x == 3 -> whiteQueenSide
                !piece.isWhite && to.x == 7 -> blackKingSide
                else -> blackQueenSide
            }

            rook = getPiece(info.rookFrom.x, info.rookFrom.y)
            originalRookSquare = rook?.square

            piece.square = info.kingTo
            rook?.square = info.rookTo
        } else {
            piece.square = to
        }

        val kingSquare = findKing(piece.isWhite)
        val inCheck = isSquareAttacked(kingSquare, !piece.isWhite)

        piece.square = originalPieceSquare
        if (originalRookSquare != null) {
            rook?.square = originalRookSquare
        }
        captured?.let { pieces.add(it) }
        epCaptured?.let { pieces.add(it) }

        return !inCheck
    }

    fun getPiece(x: Int, y: Int): Piece? {
        if (pieces.none { it.square.x == x && it.square.y == y }) return null
        return pieces.first { it.square.x == x && it.square.y == y }
    }

    fun getAvailableSquares(pieceType: PieceType, sq: Square, isWhite: Boolean): List<Square> {
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

        if (isInBoard(sq.x, ny) && getPiece(sq.x, ny) == null) {
            moves += Square(sq.x, ny)
            val startRow = if (isWhite) 2 else 7
            val ny2 = sq.y + dir * 2
            if (sq.y == startRow && getPiece(sq.x, ny) == null && getPiece(sq.x, ny2) == null) {
                moves += Square(sq.x, ny2)
            }
        }

        for (dx in listOf(-1, 1)) {
            val nx = sq.x + dx
            if (!isInBoard(nx, ny)) continue
            val targetPiece = getPiece(nx, ny)
            if (targetPiece != null && targetPiece.isWhite != isWhite) {
                moves += Square(nx, ny)
            }
        }

        for (dx in listOf(-1, 1)) {
            val nx = sq.x + dx
            if (!isInBoard(nx, sq.y)) continue
            val sidePawn = getPiece(nx, sq.y)
            if (sidePawn != null && sidePawn == enPassantPawn && sidePawn.isWhite != isWhite) {
                moves += Square(nx, sq.y + dir)
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

        if (isWhite && sq.x == 5 && sq.y == 1) {
            tryAddCastle(whiteKingSide, moves)
            tryAddCastle(whiteQueenSide, moves)
        }
        if (!isWhite && sq.x == 5 && sq.y == 8) {
            tryAddCastle(blackKingSide, moves)
            tryAddCastle(blackQueenSide, moves)
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
                    moves += target
                } else {
                    if (piece.isWhite != isWhite) {
                        moves += target
                    }
                    break
                }

                x += dx
                y += dy
            }
        }
        return moves
    }

    private fun checkCollision(target: Square, isWhite: Boolean): Boolean {
        val piece = getPiece(target.x, target.y)
        return piece?.isWhite == isWhite
    }

    private fun isSquareAttacked(target: Square, byWhite: Boolean): Boolean {
        for (piece in pieces) {
            if (piece.isWhite != byWhite) continue

            val from = piece.square
            val moves = when (piece.pieceType) {
                PieceType.PAWN -> pawnAttackSquares(from, piece.isWhite)
                PieceType.KING -> kingAttackSquares(from)
                else -> getAvailableSquares(piece.pieceType, from, piece.isWhite)
            }

            if (moves.any { it.x == target.x && it.y == target.y }) {
                return true
            }
        }
        return false
    }

    private fun pawnAttackSquares(sq: Square, isWhite: Boolean): List<Square> {
        val dir = if (isWhite) 1 else -1
        return listOfNotNull(
            Square(sq.x - 1, sq.y + dir).takeIf { isInBoard(it.x, it.y) },
            Square(sq.x + 1, sq.y + dir).takeIf { isInBoard(it.x, it.y) }
        )
    }

    private fun kingAttackSquares(sq: Square): List<Square> {
        val moves = mutableListOf<Square>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = sq.x + dx
                val ny = sq.y + dy
                if (isInBoard(nx, ny)) {
                    moves += Square(nx, ny)
                }
            }
        }
        return moves
    }

    private fun findKing(isWhite: Boolean): Square {
        return pieces.first {
            it.pieceType == PieceType.KING && it.isWhite == isWhite
        }.square
    }

    private fun tryAddCastle(info: CastleInfo, moves: MutableList<Square>) {
        if (!castlingRights.contains(info.right)) return

        val king = getPiece(info.kingFrom.x, info.kingFrom.y)
        val rook = getPiece(info.rookFrom.x, info.rookFrom.y)
        if (
            king?.pieceType != PieceType.KING ||
            rook?.pieceType != PieceType.ROOK ||
            king.isWhite != rook.isWhite
        ) return

        val enemyIsWhite = !king.isWhite

        if (isSquareAttacked(info.kingFrom, enemyIsWhite)) return

        val step = if (info.kingTo.x > info.kingFrom.x) 1 else -1
        var x = info.kingFrom.x + step
        while (x != info.kingTo.x + step) {
            val sq = Square(x, info.kingFrom.y)

            if (x != info.kingTo.x && getPiece(x, sq.y) != null) return

            if (isSquareAttacked(sq, enemyIsWhite)) return

            x += step
        }

        moves += info.kingTo
    }

    suspend fun handlePromotion(pawn: Piece, promotedType: PieceType = PieceType.QUEEN) {
        require(promotedType in listOf(PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.QUEEN)) {
            print("PromotedType cannot be $promotedType")
        }

        pieces.remove(pawn)
        eventAdapter.call(PieceDespawnEvent(pawn))

        val newPiece = Piece(this, promotedType, pawn.isWhite, pawn.square)
        eventAdapter.call(PieceSpawnEvent(newPiece))
    }

    suspend fun undo() {
        if (undoStack.isEmpty()) return

        /* =========================
           1️⃣ 현재 포지션 카운트 감소
           ========================= */

        updatePlacement()
        val currentKey = positionKey()
        positionCount[currentKey]?.let {
            if (it <= 1) positionCount.remove(currentKey)
            else positionCount[currentKey] = it - 1
        }

        /* =========================
           2️⃣ move 상태 복구
           ========================= */

        val m = undoStack.removeLast()
        redoStack.addLast(m)

        isWhiteTurn = m.previousTurn
        castlingRights = m.previousCastlingRights
        enPassantSquare = m.previousEnPassantSquare
        enPassantPawn = m.previousEnPassantPawn
        halfmoveClock = m.previousHalfmoveClock
        fullmoveNumber = m.previousFullmoveNumber

        // 프로모션 복구
        if (m.promotedFrom != null) {
            removePiece(m.to)
            val pawn = Piece(this, PieceType.PAWN, m.movedPiece.isWhite, m.to)
            pieces.add(pawn)
            eventAdapter.call(PieceSpawnEvent(pawn))
        }

        // 말 원위치
        m.movedPiece.moveTo(m.from)

        // 캐슬링 복구
        if (m.wasCastle && m.rook != null && m.rookFrom != null) {
            m.rook.moveTo(m.rookFrom)
        }

        // 캡처 복구
        m.capturedPiece?.let {
            pieces.add(it)
            eventAdapter.call(PieceSpawnEvent(it))
        }

        /* =========================
           3️⃣ placement 갱신 (선택)
           ========================= */

        updatePlacement()
    }


    suspend fun redo() {
        if (redoStack.isEmpty()) return

        val m = redoStack.removeLast()
        undoStack.addLast(m)

        inputMove(m.move)
    }

    private fun positionKey(): String {
        val ep = enPassantSquare?.toPos() ?: "-"
        return "$placement ${if (isWhiteTurn) "w" else "b"} $castlingRights $ep"
    }

    private fun updatePlacement() {
        val ranks = (8 downTo 1).joinToString("/") { y ->
            var empty = 0
            val sb = StringBuilder()

            for (x in 1..8) {
                val p = getPiece(x, y)
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    val ch = when (p.pieceType) {
                        PieceType.PAWN -> 'p'
                        PieceType.ROOK -> 'r'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                        else -> '?'
                    }
                    sb.append(if (p.isWhite) ch.uppercaseChar() else ch)
                }
            }
            if (empty > 0) sb.append(empty)
            sb.toString()
        }
        placement = ranks
    }

}