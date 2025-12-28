package io.github.zeqky.chess.core

import java.util.UUID

class FakeUndoState(
    val viewer: UUID,
    val board: Board
) {
    var cursor: Int = board.undoStack.size
}