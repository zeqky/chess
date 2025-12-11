package io.github.zeqky.chess

import io.github.zeqky.chess.stockfish.StockfishProcess

class ChessGame(
    private val stockfish: StockfishProcess
) {
    /** private val moves = mutableListOf<String>()

    init {
        stockfish.newGame()
    }

    /** Stockfish에게 넘길 moves 리스트 반환 */
    private fun getMoves(): List<String> = moves

    fun playerMove(move: String): String {
        moves += move

        // 핵심: startpos moves e2e4 e7e5 ...  형식으로만 전달
        stockfish.setMoves(*moves.toTypedArray())

        return aiMove()
    }

    fun aiMove(): String {
        val bestMoveLine = stockfish.bestMove()
        val parts = bestMoveLine.split(" ")

        if (parts.size < 2) return "AI가 수를 찾지 못했습니다."

        val aiMove = parts[1]
        moves += aiMove

        return aiMove
    } **/
}