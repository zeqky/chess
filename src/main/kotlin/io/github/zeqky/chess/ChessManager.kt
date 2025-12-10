package io.github.zeqky.chess

import io.github.zeqky.chess.stockfish.StockfishProcess
import org.bukkit.entity.Player

object ChessManager {
    private val games = mutableMapOf<Player, ChessGame>()
    lateinit var stockfish: StockfishProcess// 싱글톤

    fun startGame(player: Player): ChessGame {
        val game = ChessGame(stockfish)
        games[player] = game
        return game
    }

    fun getGame(player: Player): ChessGame? = games[player]

    fun stopGame(player: Player) {
        games.remove(player)
    }
}