package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.plugin.ChessPlugin
import io.github.zeqky.chess.stockfish.StockfishProcess
import io.github.zeqky.fount.fake.FakeEntityServer
import org.bukkit.Bukkit
import org.bukkit.Location

object ChessManager {
    lateinit var stockfish: StockfishProcess// 싱글톤
    val boards = arrayListOf<ChessBoard>()
    lateinit var fakeEntityServer: FakeEntityServer
    lateinit var plugin: ChessPlugin

    fun init(plugin: ChessPlugin) {
        this.plugin = plugin
        fakeEntityServer = FakeEntityServer.create(plugin).apply {
            Bukkit.getOnlinePlayers().forEach { this.addPlayer(it) }
            plugin.server.pluginManager.registerEvents(PaperListener(), plugin)
            plugin.server.scheduler.runTaskTimer(plugin, this@ChessManager::update, 0L, 1L)
        }
    }

    fun createBoard(name: String, loc: Location) {
        boards += ChessBoard(name,
            Board().apply {
                setup()
            },
            loc.toBlockLocation().add(0.5, 0.0, 0.5))
    }

    fun setupBoard(board: ChessBoard) {
        board.init()
    }

    private fun update() {
        fakeEntityServer.update()
        boards.forEach {
            it.update()
        }
    }
}