package io.github.zeqky.chess

import io.github.zeqky.chess.core.event.CheckMateEvent
import io.github.zeqky.chess.core.event.FiftyMoveEvent
import io.github.zeqky.chess.core.event.PieceDespawnEvent
import io.github.zeqky.chess.core.event.PieceMoveEvent
import io.github.zeqky.chess.core.event.PieceSpawnEvent
import io.github.zeqky.chess.core.event.StaleMateEvent
import io.github.zeqky.chess.core.event.ThreefoldEvent
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
        }
    }

    private fun onPieceSpawn(event: PieceSpawnEvent) {
        event.piece.attachment<ChessPiece>().spawn()
    }
    private fun onPieceMove(event: PieceMoveEvent) {
        event.piece.attachment<ChessPiece>().moveTo(event.nextSquare)
    }

    private fun onPieceDespawn(event: PieceDespawnEvent) {
        event.piece.attachment<ChessPiece>().despawn()
    }

    private fun onCheckMate(event: CheckMateEvent) {
        val board = process.game.board.attachment<ChessBoard>()
        val whiteName = board.white?.name
        val blackName = board.black?.name
        Bukkit.getLogger().info { "CheckMate. Winner: ${if (event.winner) whiteName else blackName}" }
    }

    private fun onStaleMate(event: StaleMateEvent) {
        Bukkit.getLogger().info { "Draw by StaleMate!" }
    }

    private fun onThreeFold(event: ThreefoldEvent) {
        Bukkit.getLogger().info { "Draw by Threefold!" }
    }

    private fun onFiftyMove(event: FiftyMoveEvent) {
        Bukkit.getLogger().info { "Draw by FiftyMove Rule!" }
    }
}