package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.dialog.GameDialogAdapter
import kotlinx.coroutines.*

class Game (private val board: Board) {
    private var turnWhite: Boolean = true
    val dialogAdapter = GameDialogAdapter()

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        scope.launch(dispatcher) {
            while (isActive) {

            }
        }
    }
}