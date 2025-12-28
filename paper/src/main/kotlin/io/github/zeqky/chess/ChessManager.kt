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
    lateinit var plugin: ChessPlugin

    fun init(plugin: ChessPlugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(PaperListener(), plugin)
        Bukkit.getScheduler().runTaskTimer(plugin, ::update, 0L, 1L)
    }

    fun createBoard(name: String, loc: Location): ChessBoard {
        require(!boards.any { it.name == name }) {
            "Board $name already exist!"
        }

        return ChessBoard(name,
            Board().apply {
                setup()
            },
            loc.toBlockLocation().add(0.5, 0.0, 0.5)).apply {
                boards.add(this)
            }.apply {
                init()
        }
    }

    fun resetBoard(name: String) {
        val board = boards.first { it.name == name }.apply {
            boards.remove(this)
            removeAll()
        }
        createBoard(name, board.a1)
    }

    fun start(board: ChessBoard, tc: String) {
        ChessProcess(plugin).start(board.board, tc)
    }

    private fun update() {
        boards.forEach {
            it.update()
        }
    }
}