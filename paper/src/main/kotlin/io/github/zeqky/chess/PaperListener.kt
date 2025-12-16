package io.github.zeqky.chess

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class PaperListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        ChessManager.fakeEntityServer.addPlayer(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        ChessManager.fakeEntityServer.removePlayer(event.player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return

        event.isCancelled = true

        ChessManager.boards.forEach {
            val pos = it.findSquare(block.location)
            it.onClick(pos)
        }
    }
}