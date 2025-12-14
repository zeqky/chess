package io.github.zeqky.chess.core.api

import io.github.zeqky.chess.core.Square

open class CPiece {
    open fun spawn() {}
    open fun moveTo(square: Square) {}
    open fun despawn() {}
}