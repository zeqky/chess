package io.github.zeqky.chess.core

import kotlinx.coroutines.*

class Game (private val board: Board) {
    private var turnWhite: Boolean = true

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        scope.launch(dispatcher) {
            while (isActive) {

            }
        }
    }
}