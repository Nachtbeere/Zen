package net.nachtbeere.minecraft.zen.model

import net.nachtbeere.minecraft.zen.Zen
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import java.time.LocalDateTime
import java.time.OffsetDateTime

open class ZenModelBase() {
    var minecraftServer: Server = bukkitServer()
    var currentPlugin: Zen = currentPlugin() as Zen

    private fun bukkitServer(): Server = Bukkit.getServer()

    private fun currentPlugin(): Plugin = minecraftServer.pluginManager.getPlugin(Zen.packageName) as Plugin

    fun utcTimeToOffset(utcTime: LocalDateTime): OffsetDateTime {
        return currentPlugin.chrono!!.toZonedTime(utcTime)
    }
}