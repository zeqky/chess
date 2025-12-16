package io.github.zeqky.chess.core

class Square(val x: Int, val y: Int) {
    fun toPos(): String {
        val ex = ('a'.code + x - 1).toChar()
        return "$ex$y"
    }
}