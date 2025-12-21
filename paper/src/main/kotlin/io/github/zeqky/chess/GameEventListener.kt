package io.github.zeqky.chess

import io.github.zeqky.chess.core.event.PieceDespawnEvent
import io.github.zeqky.chess.core.event.PieceMoveEvent
import io.github.zeqky.chess.core.event.PieceSpawnEvent

class GameEventListener(private val process: ChessProcess) {
    init {
        process.game.board.eventAdapter.apply {
            register(PieceMoveEvent::class.java, ::onPieceMove)
            register(PieceDespawnEvent::class.java, ::onPieceDespawn)
            register(PieceSpawnEvent::class.java, ::onPieceSpawn)
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
}