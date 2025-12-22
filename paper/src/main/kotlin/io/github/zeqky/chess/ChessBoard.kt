package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.GameCommand
import io.github.zeqky.chess.core.GameState
import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square
import io.github.zeqky.fount.fake.FakeEntity
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player

class ChessBoard(val name: String, val board: Board, val a1: Location) {
    var selectedPiece: ChessPiece? = null
    var white: ChessPlayer? = null
    var black: ChessPlayer? = null

    val currentPlayer
        get() = if (board.isWhiteTurn) white else black

    fun init() {
        board.attach(this)
        board.pieces.forEach {
            it.attach(ChessPiece(this, it).apply { spawn() })
        }
    }

    fun setWhite(player: Player) {
        white = ChessPlayer(true, isAI = false).apply { bukkitPlayer = player }
    }

    fun setBlack(player: Player) {
        black = ChessPlayer(false, isAI = false).apply { bukkitPlayer = player }
    }

    fun setWhiteAI() {
        white = ChessPlayer(true, isAI = true)
    }

    fun setBlackAI() {
        black = ChessPlayer(false, isAI = true)
    }

    fun print(string: String) {
        Bukkit.broadcast(text(string))
    }

    fun spawn(piece: Piece) {
        board.pieces.add(piece)
        piece.attach(ChessPiece(this, piece).apply { spawn() })
    }

    fun onClick(square: Square) {
        if (board.game?.gameState != GameState.ACTIVE) return

        if (selectedPiece == null) {
            val piece = getPiece(square)
            if (piece != null) {
                if (piece.piece.isWhite != currentPlayer?.isWhite) return
                registerSelected(piece)
            }
        } else input(square)
    }

    fun getPiece(square: Square): ChessPiece? {
        return if (board.pieces.any { it.square.x == square.x && it.square.y == square.y }) {
            board.pieces.first { it.square.x == square.x && it.square.y == square.y }.attachment() as ChessPiece
        } else null
    }

    fun getPieceByFake(entity: FakeEntity<*>): ChessPiece? {
        val loc = entity.location.clone().subtract(a1).add(1.0, 0.0, 1.0)
        val square = Square(loc.x.toInt(), loc.z.toInt())
        return getPiece(square)
    }

    fun removeAll() {
        board.pieces.forEach {
            it.attachment<ChessPiece>().fakeEntity.remove()
        }
    }

    private fun registerSelected(piece: ChessPiece) {
        selectedPiece = piece
    }

    private fun input(square: Square) {
        val squares = board.getAvailableSquares(selectedPiece!!.piece.pieceType, selectedPiece!!.currentSquare, selectedPiece!!.piece.isWhite)
        if (!squares.any { it.x == square.x && it.y == square.y }) {
            selectedPiece = null
            return
        }
        val previousSq = selectedPiece!!.currentSquare.toPos()
        val nextSq = square.toPos()
        board.game?.send(GameCommand.PieceMove("$previousSq$nextSq"))
        selectedPiece = null
    }

    fun findSquare(loc: Location): Square {
        val dx = loc.blockX - a1.blockX + 1
        val dz = loc.blockZ - a1.blockZ + 1
        return board.squares.first { it.x == dz && it.y == dx }
    }

    fun update() {
        if (selectedPiece != null) {
            val squares = board.getAvailableSquares(selectedPiece!!.piece.pieceType, selectedPiece!!.currentSquare, selectedPiece!!.piece.isWhite)
            squares.forEach {
                val pos = a1.clone().add(it.y - 1.0, 1.1, it.x - 1.0)
                pos.world.spawnParticle(Particle.END_ROD, pos, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }
}