package io.github.zeqky.chess.stockfish

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

/**
 * StockfishProcess: Stockfish UCI 엔진 프로세스 래퍼
 *
 * 사용법:
 *   val exe = File(plugin.dataFolder, "stockfish" or "stockfish.exe")
 *   val sf = StockfishProcess(exe)
 *   sf.uci()
 *   sf.isReady()
 *   sf.newGame()
 *   sf.setStartPos()
 *   val best = sf.bestMove(depth = 10)
 *   sf.quit()
 */
class StockfishProcess(private val exe: File) {

    private val process: Process
    private val writer: BufferedWriter
    private val reader: BufferedReader

    init {
        process = startProcess(exe)
        writer = BufferedWriter(OutputStreamWriter(process.outputStream))
        reader = BufferedReader(InputStreamReader(process.inputStream))
    }

    // ------------------- low-level -------------------

    private fun startProcess(exe: File): Process {
        if (!exe.exists()) throw IllegalArgumentException("Stockfish executable not found: ${exe.absolutePath}")
        // start
        val pb = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(true)
        val p = pb.start()

        // 잠깐 대기해서 바로 종료되는지 확인
        Thread.sleep(150)
        if (!p.isAlive) {
            val out = p.inputStream.readAllBytes().toString(Charsets.UTF_8)
            throw IllegalStateException("❌ Stockfish failed to start. output:\n$out")
        }

        return p
    }

    @Synchronized
    private fun send(cmd: String) {
        if (!process.isAlive) throw IllegalStateException("Stockfish process is not alive")
        writer.write(cmd)
        writer.newLine()
        writer.flush()
    }

    @Synchronized
    private fun readLineInternal(timeoutMs: Long = 0): String? {
        // timeoutMs == 0 => block until line available
        return (if (timeoutMs <= 0) {
            reader.readLine()
        } else {
            val start = System.nanoTime()
            while (true) {
                if (reader.ready()) {
                    return reader.readLine()
                }
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMs) return null
                Thread.sleep(5)
            }
        }) as String?
    }

    // read until a line that startsWith(prefix) appears, blocking
    private fun readUntil(prefix: String): String {
        while (true) {
            val line = readLineInternal() ?: continue
            if (line.startsWith(prefix)) return line
        }
    }

    private fun readUntilBestMove(): String = readUntil("bestmove")

    // ------------------- high-level UCI wrappers -------------------

    /** Enter UCI mode and return all lines until "uciok" */
    @Synchronized
    fun uci(): List<String> {
        send("uci")
        val out = mutableListOf<String>()
        while (true) {
            val l = readLineInternal() ?: continue
            out.add(l)
            if (l.trim() == "uciok") break
        }
        return out
    }

    /** Send isready and wait for readyok */
    @Synchronized
    fun isReady(timeoutMs: Long = 0): Boolean {
        send("isready")
        // if timeoutMs == 0 block indefinitely
        return (if (timeoutMs <= 0L) {
            readUntil("readyok") == "readyok"
        } else {
            val start = System.nanoTime()
            while (true) {
                val l = readLineInternal(50) ?: continue
                if (l.startsWith("readyok")) return true
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMs) return false
            }
        }) as Boolean
    }

    /** Set an engine option */
    @Synchronized
    fun setOption(name: String, value: String) {
        send("setoption name $name value $value")
    }

    /** Notify engine a new game started */
    @Synchronized
    fun newGame() {
        send("ucinewgame")
    }

    /** Set position to startpos */
    @Synchronized
    fun setStartPos() {
        send("position startpos")
    }

    /** Set position by FEN */
    @Synchronized
    fun setFen(fen: String) {
        send("position fen $fen")
    }

    /** Set startpos plus moves */
    @Synchronized
    fun setMoves(vararg moves: String) {
        if (moves.isEmpty()) {
            send("position startpos")
        } else {
            send("position startpos moves ${moves.joinToString(" ")}")
        }
    }

    /** Set FEN and optionally moves after that */
    @Synchronized
    fun setFenAndMoves(fen: String, moves: List<String> = emptyList()) {
        if (moves.isEmpty()) send("position fen $fen")
        else send("position fen $fen moves ${moves.joinToString(" ")}")
    }

    /**
     * Ask engine for best move by fixed depth.
     * Returns the 'bestmove ...' line (e.g. "bestmove e2e4")
     */
    @Synchronized
    fun bestMove(depth: Int = 12): String {
        send("go depth $depth")
        return parseBestMoveLine(readUntilBestMove())
    }

    /**
     * Ask engine for best move by time (ms).
     * Returns 'bestmove ...' line.
     */
    @Synchronized
    fun bestMoveByTime(ms: Int): String {
        send("go movetime $ms")
        return parseBestMoveLine(readUntilBestMove())
    }

    /** Start infinite analysis (engine keeps analyzing). Call stop() to get bestmove. */
    @Synchronized
    fun goInfinite() {
        send("go infinite")
    }

    /** Stop the running search and return bestmove line */
    @Synchronized
    fun stop(): String {
        send("stop")
        return parseBestMoveLine(readUntilBestMove())
    }

    /**
     * MultiPV: set MultiPV option and run go depth.
     * Returns list of info lines + a final bestmove line.
     */
    @Synchronized
    fun multiPV(depth: Int, multiPV: Int): List<String> {
        setOption("MultiPV", multiPV.toString())
        send("go depth $depth")
        val lines = mutableListOf<String>()
        while (true) {
            val l = readLineInternal() ?: continue
            if (l.startsWith("info")) lines.add(l)
            if (l.startsWith("bestmove")) {
                lines.add(l)
                break
            }
        }
        return lines
    }

    /** Utility: read all currently buffered engine lines (non-blocking) */
    @Synchronized
    fun readAllAvailable(): List<String> {
        val out = mutableListOf<String>()
        while (reader.ready()) {
            val l = reader.readLine() ?: break
            out.add(l)
        }
        return out
    }

    /** Tell engine to quit and destroy process */
    @Synchronized
    fun quit() {
        try {
            send("quit")
        } catch (_: Exception) { /* ignore */ }
        try {
            process.destroy()
        } catch (_: Exception) { /* ignore */ }
    }

    // ------------------- helpers -------------------

    private fun parseBestMoveLine(line: String): String {
        // line like: "bestmove e2e4 ponder e7e5" or "bestmove (none)"
        return line.trim()
    }

    fun isAlive(): Boolean = process.isAlive
}