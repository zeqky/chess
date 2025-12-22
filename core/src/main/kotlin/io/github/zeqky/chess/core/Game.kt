package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.event.CheckMateEvent
import io.github.zeqky.chess.core.event.FiftyMoveEvent
import io.github.zeqky.chess.core.event.StaleMateEvent
import io.github.zeqky.chess.core.event.ThreefoldEvent
import io.github.zeqky.chess.core.exception.CheckMateException
import io.github.zeqky.chess.core.exception.FiftyMoveException
import io.github.zeqky.chess.core.exception.StaleMateException
import io.github.zeqky.chess.core.exception.ThreefoldException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class Game (val board: Board) {
    var turnWhite: Boolean = true

    private val inputChannel = Channel<GameCommand>(Channel.UNLIMITED)

    lateinit var key: suspend () -> String

    var gameState = GameState.NEW

    init {
        board.game = this
    }

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        gameState = GameState.ACTIVE
        scope.launch(dispatcher) {
            while (isActive) {

                // 움직임 요청
                try {
                    while(isActive) {
                        checkCmd()
                        turnWhite = !turnWhite
                        break
                    }
                } catch (e: CheckMateException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(CheckMateEvent(e.winnerisWhite))
                    break
                } catch (e: FiftyMoveException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(FiftyMoveEvent())
                    break
                } catch (e: StaleMateException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(StaleMateEvent())
                    break
                } catch (e: ThreefoldException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(ThreefoldEvent())
                    break
                }
            }
        }
    }

    fun send(cmd: GameCommand) {
        inputChannel.trySend(cmd)
    }

    private suspend fun checkCmd() {
        for (cmd in inputChannel) {
            when (cmd) {
                is GameCommand.PieceMove -> board.inputMove(cmd.move)

                GameCommand.Undo -> board.undo()

                GameCommand.Redo -> board.redo()

                GameCommand.Reset -> board.reset()
            }
        }
    }
}

sealed interface GameCommand {
    data class PieceMove(val move: String) : GameCommand
    object Undo: GameCommand
    object Redo: GameCommand
    object Reset: GameCommand
}

enum class GameState {
    NEW,
    ACTIVE,
    FINISHED
}