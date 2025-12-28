package io.github.zeqky.chess.core.event

import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square
import java.util.UUID

interface GameEvent

class PieceSpawnEvent(val piece: Piece) : GameEvent
class FakePieceSpawnEvent(val uuid: UUID, val piece: Piece) : GameEvent
class PieceMoveEvent(val piece: Piece, val nextSquare: Square) : GameEvent
class FakePieceMoveEvent(val uuid: UUID, val piece: Piece,val from: Square, val to: Square) : GameEvent
class PieceDespawnEvent(val piece: Piece) : GameEvent
class FakePieceDespawnEvent(val uuid: UUID, val piece: Piece) : GameEvent
class FakeClearBoardEvent(
    val viewer: UUID
) : GameEvent
class CheckMateEvent(val winner: Boolean) : GameEvent
class StaleMateEvent() : GameEvent
class FiftyMoveEvent() : GameEvent
class ThreefoldEvent() : GameEvent
class TimeoutEvent(val winner: Boolean) : GameEvent
class ResignEvent(val loser: Boolean) : GameEvent
class DrawEvent: GameEvent