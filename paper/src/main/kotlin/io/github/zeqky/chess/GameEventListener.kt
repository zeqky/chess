package io.github.zeqky.chess

import io.github.zeqky.chess.core.event.CheckMateEvent
import io.github.zeqky.chess.core.event.DrawEvent
import io.github.zeqky.chess.core.event.FakeClearBoardEvent
import io.github.zeqky.chess.core.event.FakePieceDespawnEvent
import io.github.zeqky.chess.core.event.FakePieceMoveEvent
import io.github.zeqky.chess.core.event.FakePieceSpawnEvent
import io.github.zeqky.chess.core.event.FiftyMoveEvent
import io.github.zeqky.chess.core.event.PieceDespawnEvent
import io.github.zeqky.chess.core.event.PieceMoveEvent
import io.github.zeqky.chess.core.event.PieceSpawnEvent
import io.github.zeqky.chess.core.event.ResignEvent
import io.github.zeqky.chess.core.event.StaleMateEvent
import io.github.zeqky.chess.core.event.ThreefoldEvent
import io.github.zeqky.chess.core.event.TimeoutEvent
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit

class GameEventListener(private val process: ChessProcess) {
    init {
        process.game.board.eventAdapter.apply {
            register(PieceMoveEvent::class.java, ::onPieceMove)
            register(PieceDespawnEvent::class.java, ::onPieceDespawn)
            register(PieceSpawnEvent::class.java, ::onPieceSpawn)
            register(CheckMateEvent::class.java, ::onCheckMate)
            register(StaleMateEvent::class.java, ::onStaleMate)
            register(ThreefoldEvent::class.java, ::onThreeFold)
            register(FiftyMoveEvent::class.java, ::onFiftyMove)
            register(ResignEvent::class.java, ::onResign)
            register(TimeoutEvent::class.java, ::onTimeout)
            register(DrawEvent::class.java, ::onDraw)
            register(FakePieceMoveEvent::class.java, ::onFakeMove)
            register(FakePieceDespawnEvent::class.java, ::onFakeDespawn)
            register(FakePieceSpawnEvent::class.java, ::onFakeSpawn)
            register(FakeClearBoardEvent::class.java, ::onFakeClear)
        }
    }

    private fun onFakeMove(event: FakePieceMoveEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val player = board.players.first { event.uuid == it.bukkitPlayer.uniqueId }
        event.piece.attachment<ChessPiece>().fakeMoveTo(player, event.from, event.to)
    }

    private fun onFakeDespawn(event: FakePieceDespawnEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val player = board.players.first { event.uuid == it.bukkitPlayer.uniqueId }
        event.piece.attachment<ChessPiece>().fakeDespawn(player)
    }

    private fun onFakeSpawn(event: FakePieceSpawnEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val player = board.players.first { event.uuid == it.bukkitPlayer.uniqueId }
        event.piece.attachment<ChessPiece>().spawnToPlayer(player)
    }

    private fun onFakeClear(event: FakeClearBoardEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val player = board.players.first { event.viewer == it.bukkitPlayer.uniqueId }
        player.fakeEntityServer.entities.forEach {
            it.remove()
        }
    }

    private fun onPieceSpawn(event: PieceSpawnEvent) {
        event.piece.attachment<ChessPiece>().spawn()
    }
    private fun onPieceMove(event: PieceMoveEvent) {
        event.piece.attachment<ChessPiece>().moveTo(event.nextSquare)
        val board = process.game.board.attachment<ChessBoard>()
        if (board.process != null) {
            board.process!!.timeControl.onMoveMade(event.piece.isWhite)
        }
    }

    private fun onPieceDespawn(event: PieceDespawnEvent) {
        event.piece.attachment<ChessPiece>().despawn()
    }

    private fun onCheckMate(event: CheckMateEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val whiteName = board.white?.name
        val blackName = board.black?.name
        Bukkit.broadcast(text("CheckMate. Winner: ${if (event.winner) whiteName else blackName}"))
    }

    private fun onStaleMate(event: StaleMateEvent) {
        Bukkit.broadcast(text("Draw by StaleMate!"))
    }

    private fun onThreeFold(event: ThreefoldEvent) {
        Bukkit.broadcast(text("Draw by Threefold!"))
    }

    private fun onFiftyMove(event: FiftyMoveEvent) {
        Bukkit.broadcast(text("Draw by FiftyMove Rule!"))
    }

    private fun onResign(event: ResignEvent) {
        Bukkit.broadcast(text("${if (event.loser) "Black" else "White"} has losed the game!"))
    }

    private fun onTimeout(event: TimeoutEvent) {
        Bukkit.broadcast(text("${if (event.winner) "White" else "Black"} has won the game by timeout!"))
    }

    private fun onDraw(event: DrawEvent) {
        Bukkit.broadcast(text("Draw"))
    }
}