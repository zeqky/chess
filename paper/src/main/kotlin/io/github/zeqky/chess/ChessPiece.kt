package io.github.zeqky.chess

import io.github.zeqky.chess.core.Piece
import io.github.zeqky.chess.core.Square
import io.github.zeqky.fount.fake.FakeEntity
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

class ChessPiece(val board: ChessBoard, val piece: Piece) {

    var loc: Location = board.a1.clone().add(piece.square.y - 1.0, 0.0, piece.square.x - 1.0)
        get() = field.clone()

    val fakeEntityByPlayer = HashMap<ChessPlayer, FakeEntity<*>>()
    val fakeEntityLoc = HashMap<ChessPlayer, Location>()
    var currentSquare: Square = piece.square
    fun spawn() {
        val playerList = board.players
        for (player in playerList) {
            fakeEntityByPlayer[player] = player.fakeEntityServer.spawnEntity(loc, ArmorStand::class.java).apply {
                updateMetadata {
                    customName(text(piece.pieceType.toString()).color(if (piece.isWhite) NamedTextColor.WHITE else NamedTextColor.BLACK))
                    isCustomNameVisible = true
                }
            }
        }
    }

    fun spawnToPlayer(player: ChessPlayer) {
        fakeEntityByPlayer[player] = player.fakeEntityServer.spawnEntity(loc, ArmorStand::class.java).apply {
            updateMetadata {
                customName(text(piece.pieceType.toString()).color(if (piece.isWhite) NamedTextColor.WHITE else NamedTextColor.BLACK))
                isCustomNameVisible = true
            }
        }
    }

    fun fakeMoveTo(player: ChessPlayer, from: Square, to: Square) {
        val entity = fakeEntityByPlayer[player] ?: return
        val dx = to.x - from.x
        val dy = to.y - from.y
        entity.move(dy.toDouble(), 0.0, dx.toDouble())
    }

    fun moveTo(square: Square) {
        val dx = square.x - currentSquare.x
        val dy = square.y - currentSquare.y
        currentSquare = square
        fakeEntityByPlayer.forEach { (player, fakeEntity) ->
            fakeEntity.move(dy.toDouble(), 0.0, dx.toDouble())
            loc = fakeEntity.location
        }
    }

    fun despawn() {
        val it = fakeEntityByPlayer.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            entry.value.remove()
            it.remove()
        }
    }

    fun fakeDespawn(player: ChessPlayer) {
        fakeEntityByPlayer[player]?.remove()
        fakeEntityByPlayer.remove(player)
    }
}