package io.github.zeqky.chess

import io.github.zeqky.chess.core.GameCommand
import io.github.zeqky.chess.core.GameState
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import kotlin.math.max

private const val TICK_MS = 50L

class TimeControl(val board: ChessBoard) {

    var whiteTimeMs: Long = 10 * 60 * 1000L
    var blackTimeMs: Long = 10 * 60 * 1000L

    var whiteIncrementMs: Long = 0L
    var blackIncrementMs: Long = 0L

    private val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    val objective: Objective =
        scoreboard.getObjective("tc")
            ?: scoreboard.registerNewObjective(
                "tc",
                Criteria.DUMMY,
                text("§6♟ Time Control")
            ).apply {
                displaySlot = DisplaySlot.SIDEBAR
            }

    init {
        // 초기 줄 등록 (문자열 중복 방지)
        objective.getScore("§fWhite").score = 2
        objective.getScore("§fBlack").score = 1
    }

    /**
     * tick마다 호출 (50ms)
     * Bukkit main thread 전용
     */
    fun update() {
        if (board.board.game?.gameState != GameState.ACTIVE) return
        val currentIsWhite = board.currentPlayer?.isWhite ?: return

        if (currentIsWhite) {
            whiteTimeMs = max(0L, whiteTimeMs - TICK_MS)
        } else {
            blackTimeMs = max(0L, blackTimeMs - TICK_MS)
        }

        // 표시 (초 단위)
        objective.getScore("§fWhite").score = (whiteTimeMs / 1000).toInt()
        objective.getScore("§fBlack").score = (blackTimeMs / 1000).toInt()

        // timeout 판정
        if (whiteTimeMs <= 0) {
            board.board.game?.send(GameCommand.TimeOut(false))
        } else if (blackTimeMs <= 0) {
            board.board.game?.send(GameCommand.TimeOut(true))
        }
    }

    /**
     * 수가 정상적으로 두어졌을 때 호출
     * (turn이 바뀌기 직전 or 직후 중 한쪽으로 통일)
     */
    fun onMoveMade(isWhite: Boolean) {
        if (isWhite) {
            whiteTimeMs += whiteIncrementMs
        } else {
            blackTimeMs += blackIncrementMs
        }
    }

    /**
     * TimeControl 문자열 파싱
     *
     * 지원:
     *  - 10+2
     *  - 5
     *  - 10m+2s
     *  - 600+2
     */
    fun parseTC(tc: String) {
        val clean = tc.replace(" ", "")
        val parts = clean.split("+")

        val base = parseBase(parts[0])
        val inc = if (parts.size > 1) parseInc(parts[1]) else 0L

        whiteTimeMs = base
        blackTimeMs = base
        whiteIncrementMs = inc
        blackIncrementMs = inc
    }

    /**
     * 기본 시간 파싱
     * 단위 없으면:
     *  - 60 이상 → 초
     *  - 미만 → 분
     */
    private fun parseBase(s: String): Long {
        return when {
            s.endsWith("m") -> s.dropLast(1).toLong() * 60_000
            s.endsWith("s") -> s.dropLast(1).toLong() * 1_000
            else -> {
                val v = s.toLong()
                if (v >= 60) v * 1_000 else v * 60_000
            }
        }
    }

    /**
     * 증초 파싱
     * 단위 없으면 초
     */
    private fun parseInc(s: String): Long {
        return when {
            s.endsWith("s") -> s.dropLast(1).toLong() * 1_000
            else -> s.toLong() * 1_000
        }
    }
}