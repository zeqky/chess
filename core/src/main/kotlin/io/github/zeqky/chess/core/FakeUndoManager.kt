package io.github.zeqky.chess.core

import io.github.zeqky.chess.core.event.FakeClearBoardEvent
import io.github.zeqky.chess.core.event.FakePieceDespawnEvent
import io.github.zeqky.chess.core.event.FakePieceMoveEvent
import io.github.zeqky.chess.core.event.FakePieceSpawnEvent
import java.util.UUID

class FakeUndoManager(private val board: Board) {
    private val states = mutableMapOf<UUID, FakeUndoState>()

    private fun state(uuid: UUID): FakeUndoState =
        states.getOrPut(uuid) { FakeUndoState(uuid, board) }

    suspend fun fakeUndo(viewer: UUID) {
        val s = state(viewer)
        if (s.cursor <= 0) return

        s.cursor--
        val move = board.undoStack.elementAt(s.cursor)

        applyUndo(viewer, move)
    }

    suspend fun fakeRedo(viewer: UUID) {
        val s = state(viewer)
        if (s.cursor >= board.undoStack.size) return

        val move = board.undoStack.elementAt(s.cursor)
        s.cursor++

        applyRedo(viewer, move)
    }

    suspend fun fakeReset(viewer: UUID) {
        resendFullState(viewer)
        states.remove(viewer)
    }

    suspend fun fakeResetAll() {
        // 모든 viewer를 최신 상태로 강제 동기화
        states.keys.forEach { viewer ->
            resendFullState(viewer)
        }

        // 모든 fakeUndo 상태 제거
        states.clear()
    }

    fun isAtLatest(viewer: UUID): Boolean {
        val s = states[viewer] ?: return true
        return s.cursor == board.undoStack.size
    }

    /* =================================================
       UNDO (과거로 이동)
       ================================================= */

    private suspend fun applyUndo(viewer: UUID, m: MoveState) {

        /* ===== 1️⃣ 프로모션 복구 ===== */
        if (m.promotedFrom != null) {
            // 현재 보이는 promoted piece 제거
            board.eventAdapter.call(
                FakePieceDespawnEvent(viewer, m.movedPiece)
            )

            // pawn 다시 spawn
            val pawn = Piece(
                board,
                PieceType.PAWN,
                m.movedPiece.isWhite,
                m.to
            )
            board.eventAdapter.call(
                FakePieceSpawnEvent(viewer, pawn)
            )
        }

        /* ===== 2️⃣ 이동 되돌리기 ===== */
        board.eventAdapter.call(
            FakePieceMoveEvent(viewer, m.movedPiece, m.to, m.from)
        )

        /* ===== 3️⃣ 캐슬링 rook 복구 ===== */
        if (m.wasCastle && m.rook != null && m.rookFrom != null) {
            board.eventAdapter.call(
                FakePieceMoveEvent(viewer, m.rook, m.rookTo!!, m.rookFrom)
            )
        }

        /* ===== 4️⃣ 앙파상 / 일반 캡처 복구 ===== */
        m.capturedPiece?.let {
            board.eventAdapter.call(
                FakePieceSpawnEvent(viewer, it)
            )
        }
    }

    /* =================================================
       REDO (미래로 이동)
       ================================================= */

    private suspend fun applyRedo(viewer: UUID, m: MoveState) {

        /* ===== 1️⃣ 캡처 제거 ===== */
        m.capturedPiece?.let {
            board.eventAdapter.call(
                FakePieceDespawnEvent(viewer, it)
            )
        }

        /* ===== 2️⃣ 이동 ===== */
        board.eventAdapter.call(
            FakePieceMoveEvent(viewer, m.movedPiece, m.from, m.to)
        )

        /* ===== 3️⃣ 캐슬링 rook 이동 ===== */
        if (m.wasCastle && m.rook != null && m.rookTo != null) {
            board.eventAdapter.call(
                FakePieceMoveEvent(viewer, m.rook, m.rookFrom!!, m.rookTo)
            )
        }

        /* ===== 4️⃣ 프로모션 적용 ===== */
        if (m.promotedFrom != null) {
            board.eventAdapter.call(
                FakePieceDespawnEvent(viewer, m.movedPiece)
            )

            val promoted = Piece(
                board,
                m.promotedFrom,
                m.movedPiece.isWhite,
                m.to
            )
            board.eventAdapter.call(
                FakePieceSpawnEvent(viewer, promoted)
            )
        }
    }

    /* =================================================
       전체 상태 재전송
       ================================================= */

    private suspend fun resendFullState(viewer: UUID) {

        // 🔥 1. viewer 화면 완전 초기화
        board.eventAdapter.call(
            FakeClearBoardEvent(viewer)
        )

        // 🔥 2. 현재 실제 board 상태 재전송
        board.pieces.forEach { piece ->
            board.eventAdapter.call(
                FakePieceSpawnEvent(viewer, piece)
            )
        }
    }
}