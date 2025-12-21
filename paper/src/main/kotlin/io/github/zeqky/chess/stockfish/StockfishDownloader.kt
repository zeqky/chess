package io.github.zeqky.chess.stockfish

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipInputStream

object StockfishDownloader {

    private const val url =
        "https://github.com/official-stockfish/Stockfish/releases/latest/download/stockfish-windows-x86-64-avx2.zip"

    fun prepare(plugin: JavaPlugin): File {
        val folder = File(plugin.dataFolder, "stockfish")
        if (!folder.exists()) folder.mkdirs()

        val zipFile = File(folder, "stockfish.zip")

        // 이미 다운로드되어 있으면 스킵
        if (!zipFile.exists()) {
            plugin.logger.info("⏳ Downloading Stockfish from $url ...")
            downloadFile(url, zipFile)
            plugin.logger.info("✔ Download completed: ${zipFile.path}")
        }

        plugin.logger.info("⏳ Extracting Stockfish ...")
        val file = File(folder, "stockfish.exe")
        if (file.exists()) {
            return file
        } else {
            val extracted = unzipStockfish(zipFile, folder, true)
            plugin.logger.info("✔ Extracted: ${extracted.path}")

            return extracted
        }
    }

    private fun downloadFile(url: String, file: File) {
        URL(url).openStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun unzipStockfish(zip: File, folder: File, isWindows: Boolean): File {
        var binary: File? = null

        ZipInputStream(Files.newInputStream(zip.toPath())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name

                if (!entry.isDirectory) {
                    // stockfish 실행파일만 복사
                    if (isWindows && name.endsWith(".exe") ||
                        !isWindows && name.endsWith("stockfish")) {

                        binary = File(folder, if (isWindows) "stockfish.exe" else "stockfish")
                        FileOutputStream(binary).use { out ->
                            zis.copyTo(out)
                        }
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        require(binary != null) { "❌ Stockfish binary not found in ZIP." }

        binary.setExecutable(true)

        return binary
    }
}