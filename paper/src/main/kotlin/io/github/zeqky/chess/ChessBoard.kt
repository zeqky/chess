package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.GameCommand
import io.github.zeqky.chess.core.GameState
import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square
import io.github.zeqky.fount.event.EntityEventManager
import io.github.zeqky.fount.fake.FakeEntity
import io.github.zeqky.fount.fake.FakeEntityServer
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChessBoard(val name: String, val board: Board, val a1: Location) {

    var process: ChessProcess? = null
    var selectedPiece: ChessPiece? = null
    var white: ChessPlayer? = null
    var black: ChessPlayer? = null

    var spectators = arrayListOf<ChessPlayer>()

    val players: List<ChessPlayer>
        get() = listOfNotNull(white, black) + spectators

    val currentPlayer
        get() = if (board.isWhiteTurn) white else black

    fun init() {
        board.attach(this)
    }

    fun setWhite(player: Player) {
        white = ChessPlayer(true, isAI = false).apply {
            bukkitPlayer = player
            board = this@ChessBoard
        }
    }

    fun setBlack(player: Player) {
        black = ChessPlayer(false, isAI = false).apply {
            bukkitPlayer = player
            board = this@ChessBoard
        }
    }

    fun setup() {
        for (player in players) {
            player.fakeEntityServer = FakeEntityServer.create(process!!.plugin).apply {
                addPlayer(player.bukkitPlayer)
                setupFake(player.bukkitPlayer, this)
            }
        }
        board.pieces.forEach {
            it.attach(ChessPiece(this, it).apply { spawn() })
        }
    }

    private fun setupFake(player: Player, server: FakeEntityServer) {
        server.apply {
            val plugin = process!!.plugin
            Bukkit.getScheduler().runTaskTimer(plugin, server::update, 0L, 1L)
            Bukkit.getPluginManager().registerEvents(FakeListener(server, player), plugin)
        }
    }

    class FakeListener(val server: FakeEntityServer, val player: Player): Listener {
        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            if (event.player == player) server.addPlayer(event.player)
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            if (event.player == player) server.removePlayer(event.player)
        }
    }

    fun setWhiteAI() {
        white = ChessPlayer(true, isAI = true).apply {
            board = this@ChessBoard
        }
    }

    fun setBlackAI() {
        black = ChessPlayer(false, isAI = true).apply {
            board = this@ChessBoard
        }
    }

    fun print(string: String) {
        Bukkit.broadcast(text(string))
    }

    fun spawn(piece: Piece) {
        board.pieces.add(piece)
        piece.attach(ChessPiece(this, piece).apply { spawn() })
    }

    fun getPiece(square: Square): ChessPiece? {
        return if (board.pieces.any { it.square.x == square.x && it.square.y == square.y }) {
            board.pieces.first { it.square.x == square.x && it.square.y == square.y }.attachment() as ChessPiece
        } else null
    }

    fun getPieceByFake(entity: FakeEntity<*>): ChessPiece? {
        val loc = entity.location.clone().subtract(a1).add(1.0, 0.0, 1.0)
        val square = Square(loc.z.toInt(), loc.x.toInt())
        return getPiece(square)
    }

    fun removeAll() {
        board.pieces.forEach {
            it.attachment<ChessPiece>().despawn()
        }
    }

    fun onClick(square: Square) {
        if (board.game?.gameState != GameState.ACTIVE) return
        if (!board.game!!.fakeUndoManager.isAtLatest(currentPlayer?.bukkitPlayer!!.uniqueId)) return

        if (selectedPiece == null) {
            val piece = getPiece(square)
            if (piece != null) {
                if (piece.piece.isWhite != currentPlayer?.isWhite) return
                registerSelected(piece)
            }
        } else input(square)
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
        if (board.game?.gameState != GameState.ACTIVE) return
        white?.update()
        black?.update()
        if (selectedPiece != null) {
            val squares = board.getAvailableSquares(selectedPiece!!.piece.pieceType, selectedPiece!!.currentSquare, selectedPiece!!.piece.isWhite)
            currentPlayer?.showAvailableSquares(squares)
        }
    }
}