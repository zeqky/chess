package io.github.zeqky.chess

import io.github.zeqky.chess.core.Board
import io.github.zeqky.chess.plugin.ChessPlugin
import io.github.zeqky.chess.stockfish.StockfishProcess
import io.github.zeqky.fount.fake.FakeEntityServer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

object ChessManager {
    private val games = mutableMapOf<Player, ChessGame>()
    lateinit var stockfish: StockfishProcess// 싱글톤
    val boards = arrayListOf<ChessBoard>()
    lateinit var fakeEntityServer: FakeEntityServer
    lateinit var plugin: ChessPlugin

    fun startGame(player: Player): ChessGame {
        val game = ChessGame(stockfish)
        games[player] = game
        return game
    }

    fun getGame(player: Player): ChessGame? = games[player]

    fun stopGame(player: Player) {
        games.remove(player)
    }

    fun init(plugin: ChessPlugin) {
        this.plugin = plugin
        fakeEntityServer = FakeEntityServer.create(plugin).apply {
            Bukkit.getOnlinePlayers().forEach { this.addPlayer(it) }
            plugin.server.pluginManager.registerEvents(PaperListener(), plugin)
            plugin.server.scheduler.runTaskTimer(plugin, ::update, 0L, 1L)
        }
    }

    fun createBoard(name: String, loc: Location) {
        boards += ChessBoard(name, Board().apply { setup() }, loc)
    }

    fun setupBoard(board: ChessBoard) {
        board.init(plugin)
    }
}