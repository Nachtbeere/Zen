package net.nachtbeere.minecraft.zen

import org.bukkit.Server
import org.bukkit.entity.Player

class ZenBroadcast(private val config: ZenMessageConfig) {
    fun broadcast(code: ZenResultCode, server: Server) {
        server.broadcastMessage(config.getMessage(code))
    }

    fun messageToPlayer(code: ZenResultCode, player: Player) {
        player.sendMessage(
            StringBuilder()
                .append(config.messagePrefix)
                .append(" ")
                .append(config.getMessage(code))
                .append(" ")
                .append(config.messageSuffix)
                .toString()
        )
    }

}