package io.github.zeqky.chess

import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square
import io.github.zeqky.chess.core.api.CPiece
import io.github.zeqky.fount.fake.FakeEntity
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class ChessPiece(var loc: Location, private val piece: Piece) : CPiece() {
    lateinit var fakeEntity: FakeEntity<ArmorStand>
    var currentSquare: Square = piece.square
    override fun spawn() {
        fakeEntity = ChessManager.fakeEntityServer.spawnEntity(loc, ArmorStand::class.java).apply {
            updateMetadata {
                customName(text(piece.pieceType.toString()).color(if (piece.isWhite) NamedTextColor.WHITE else NamedTextColor.BLACK))
                isCustomNameVisible = true
            }
        }
    }

    override fun moveTo(square: Square) {
        val dx = square.x - currentSquare.x
        val dy = square.y - currentSquare.y
        currentSquare = square
        fakeEntity.move(dy.toDouble(), 0.0, dx.toDouble())
        loc = fakeEntity.location
    }

    override fun despawn() {
        fakeEntity.remove()
    }
}