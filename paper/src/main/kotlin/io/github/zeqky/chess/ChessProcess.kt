package io.github.zeqky.chess

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.core.Game
import io.github.zeqky.chess.plugin.ChessPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand

class ChessProcess(val plugin: ChessPlugin) {
    lateinit var game: Game
        private set

    lateinit var eventListener: GameEventListener

    private val scope = HeartbeatScope()

    lateinit var timeControl: TimeControl


    fun start(board: Board, tc: String) {
        board.attachment<ChessBoard>().process = this
        board.attachment<ChessBoard>().setup()
        game = Game(board)
        eventListener = GameEventListener(this)
        timeControl = TimeControl(board.attachment()).apply {
            Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 1L)
            parseTC(tc)
        }
        game.launch(scope)
    }
}