package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.event.CheckMateEvent
import io.github.zeqky.chess.core.event.DrawEvent
import io.github.zeqky.chess.core.event.FiftyMoveEvent
import io.github.zeqky.chess.core.event.ResignEvent
import io.github.zeqky.chess.core.event.StaleMateEvent
import io.github.zeqky.chess.core.event.ThreefoldEvent
import io.github.zeqky.chess.core.event.TimeoutEvent
import io.github.zeqky.chess.core.exception.CheckMateException
import io.github.zeqky.chess.core.exception.DrawException
import io.github.zeqky.chess.core.exception.FiftyMoveException
import io.github.zeqky.chess.core.exception.ResignException
import io.github.zeqky.chess.core.exception.StaleMateException
import io.github.zeqky.chess.core.exception.ThreefoldException
import io.github.zeqky.chess.core.exception.TimeoutException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.UUID

class Game (val board: Board) {

    private val inputChannel = Channel<GameCommand>(Channel.UNLIMITED)

    lateinit var key: suspend () -> String

    var gameState = GameState.NEW

    var fakeUndoManager: FakeUndoManager
        private set

    init {
        board.game = this
        fakeUndoManager = FakeUndoManager(board)
    }

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        gameState = GameState.ACTIVE
        scope.launch(dispatcher) {
            while (isActive) {

                // 움직임 요청
                try {
                    while(isActive) {
                        checkCmd()
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
                } catch (e: TimeoutException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(TimeoutEvent(e.winnerisWhite))
                    break
                } catch (e: ResignException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(ResignEvent(e.loser))
                    break
                } catch (e: DrawException) {
                    gameState = GameState.FINISHED
                    board.eventAdapter.call(DrawEvent())
                    break
                }
            }
        }
    }

    fun send(cmd: GameCommand) {
        inputChannel.trySend(cmd)
    }

    private suspend fun checkCmd() {
        val cmd = inputChannel.receive()
        when (cmd) {
            is GameCommand.PieceMove -> {
                board.inputMove(cmd.move)
                fakeUndoManager.fakeResetAll()
            }

            is GameCommand.TimeOut -> {
                throw TimeoutException(cmd.winner)
            }

            is GameCommand.Resign -> {
                throw ResignException(cmd.loser)
            }

            GameCommand.Undo -> board.undo()

            GameCommand.Redo -> board.redo()

            GameCommand.Reset -> board.reset()

            is GameCommand.FakeUndo -> {
                fakeUndoManager.fakeUndo(cmd.p)
            }

            is GameCommand.FakeRedo -> {
                fakeUndoManager.fakeRedo(cmd.p)
            }

            is GameCommand.FakeReset -> {
                fakeUndoManager.fakeReset(cmd.p)
            }

            GameCommand.Draw -> throw DrawException()
        }
    }
}

sealed interface GameCommand {
    data class PieceMove(val move: String) : GameCommand
    data class TimeOut(val winner: Boolean) : GameCommand
    data class Resign(val loser: Boolean): GameCommand
    object Undo: GameCommand
    object Redo: GameCommand
    object Reset: GameCommand
    data class FakeUndo(val p: UUID): GameCommand
    data class FakeRedo(val p: UUID): GameCommand
    data class FakeReset(val p: UUID): GameCommand
    object Draw: GameCommand
}

enum class GameState {
    NEW,
    ACTIVE,
    FINISHED
}