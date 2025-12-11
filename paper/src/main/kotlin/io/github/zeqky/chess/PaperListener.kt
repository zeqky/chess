package io.github.zeqky.chess

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PaperListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        ChessManager.fakeEntityServer.addPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        ChessManager.fakeEntityServer.removePlayer(event.player)
    }
}