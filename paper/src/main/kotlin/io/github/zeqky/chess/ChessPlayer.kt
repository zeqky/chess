package io.github.zeqky.chess

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.WrappedParticle
import io.github.zeqky.chess.core.Square
import io.github.zeqky.fount.fake.FakeEntityServer
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.Player

class ChessPlayer(val isWhite: Boolean, val isAI: Boolean) {
    lateinit var bukkitPlayer: Player
    lateinit var board: ChessBoard

    lateinit var fakeEntityServer: FakeEntityServer

    val name: String
        get() {
            return bukkitPlayer.name
        }

    val protocol = ProtocolLibrary.getProtocolManager()

    fun showAvailableSquares(squares: List<Square>) {
        squares.forEach {
            val loc = board.a1.clone().add(it.y - 1.0, 1.1, it.x - 1.0)
            val packet = PacketContainer(PacketType.Play.Server.WORLD_PARTICLES)
            //파티클 종류
            val dustOptions = Particle.DustOptions(Color.WHITE, 1.0f)
            packet.newParticles.write(0, WrappedParticle.create(Particle.DUST, dustOptions))
            //위치
            packet.doubles.write(0, loc.x)
            packet.doubles.write(1, loc.y)
            packet.doubles.write(2, loc.z)
            //오프셋
            packet.float.write(0, 0f).write(1, 0f).write(2, 0f)

            //속도
            packet.float.write(3, 0f)

            //개수
            packet.integers.write(0, 1)

            //long distances
            packet.booleans.write(0, false)

            //always visible
            packet.booleans.write(1, true)

            try {
                protocol.sendServerPacket(bukkitPlayer, packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun messageTurn() {
        val text = text("당신의 차례입니다!")
        bukkitPlayer.sendActionBar {
            if (isWhite) {
                text.color(NamedTextColor.WHITE)
            } else {
                text.color(NamedTextColor.BLACK)
            }
        }
    }

    private fun messageScores() {

    }

    fun update() {
        messageScores()
        if (board.currentPlayer == this) {
            messageTurn()
        }
    }
}