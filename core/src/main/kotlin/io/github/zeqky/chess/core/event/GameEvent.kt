package io.github.zeqky.chess.core.event

import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square

interface GameEvent

class PieceSpawnEvent(val piece: Piece) : GameEvent
class PieceMoveEvent(val piece: Piece, val nextSquare: Square) : GameEvent
class PieceDespawnEvent(val piece: Piece) : GameEvent
class CheckMateEvent(val winner: Boolean) : GameEvent
class StaleMateEvent() : GameEvent
class FiftyMoveEvent() : GameEvent
class ThreefoldEvent() : GameEvent