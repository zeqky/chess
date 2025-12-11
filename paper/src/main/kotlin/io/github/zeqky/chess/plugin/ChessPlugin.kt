package io.github.zeqky.chess.plugin

import io.github.zeqky.chess.ChessManager
import io.github.zeqky.chess.stockfish.StockfishDownloader
import io.github.zeqky.chess.stockfish.StockfishManager
import org.bukkit.plugin.java.JavaPlugin

class ChessPlugin : JavaPlugin() {

    override fun onEnable() {
        StockfishManager.init(this)
        ChessManager.init(this)
        ChessKommand.register(this)
    }

    override fun onDisable() {

    }
}
