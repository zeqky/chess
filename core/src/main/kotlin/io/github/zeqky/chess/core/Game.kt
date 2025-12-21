package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.exception.CheckMateException
import io.github.zeqky.chess.core.exception.FiftyMoveException
import io.github.zeqky.chess.core.exception.StaleMateException
import io.github.zeqky.chess.core.exception.ThreefoldException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class Game (val board: Board) {
    private var turnWhite: Boolean = true

    private val commands = Channel<GameCommand>(Channel.UNLIMITED)

    val mchannel = Channel<String>(Channel.UNLIMITED)

    init {
        board.game = this
    }

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        scope.launch(dispatcher) {
            while (isActive) {
                checkCmd()

                // 움직임 요청
                try {
                    while(isActive) {
                        val move = requestMove()
                        send(GameCommand.PieceMove(move))
                        turnWhite = !turnWhite
                        break
                    }
                } catch (e: CheckMateException) {
                    break
                } catch (e: FiftyMoveException) {
                    break
                } catch (e: StaleMateException) {
                    break
                } catch (e: ThreefoldException) {
                    break
                }
            }
        }
    }

    fun send(cmd: GameCommand) {
        commands.trySend(cmd)
    }

    private suspend fun checkCmd() {
        for (cmd in commands) {
            when (cmd) {
                is GameCommand.PieceMove -> board.inputMove(cmd.move)

                GameCommand.Undo -> board.undo()

                GameCommand.Redo -> board.redo()

                GameCommand.Reset -> board.reset()
            }
        }
    }

    private suspend fun requestMove(): String {
        return mchannel.receive()
    }
}

sealed interface GameCommand {
    data class PieceMove(val move: String) : GameCommand
    object Undo: GameCommand
    object Redo: GameCommand
    object Reset: GameCommand
}