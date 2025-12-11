package io.github.zeqky.chess.stockfish

import io.github.zeqky.chess.ChessManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object StockfishManager {

    lateinit var engine: StockfishProcess

    fun init(plugin: JavaPlugin) {
        plugin.logger.info("⏳ Preparing Stockfish...")

        // 다운로드 + unzip + 실행파일 찾기
        val binary: File = StockfishDownloader.prepare(plugin)

        plugin.logger.info("⏳ Starting Stockfish engine...")
        engine = StockfishProcess(binary).apply {
            ChessManager.stockfish = this
        }

        plugin.logger.info("✔ Stockfish engine ready.")
    }
}