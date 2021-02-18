package net.nachtbeere.minecraft.zen.logic

import net.nachtbeere.minecraft.zen.Zen
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

open class ZenLogicBase() {
    var minecraftServer: Server = bukkitServer()
    var currentPlugin: Zen = currentPlugin() as Zen

    private fun bukkitServer(): Server = Bukkit.getServer()

    private fun currentPlugin(): Plugin = minecraftServer.pluginManager.getPlugin(Zen.packageName) as Plugin

    fun log(msg: String) = minecraftServer.logger.info(msg)

    fun warnLog(msg: String) = minecraftServer.logger.warning(msg)

    fun severeLog(msg: String) = minecraftServer.logger.severe(msg)

    fun futureTask(task: () -> Any): Any? {
        val future = this.minecraftServer.scheduler.callSyncMethod(this.currentPlugin) { task() }
        return try {
            future.get()
        } catch (e: Throwable) {
            this.severeLog(e.toString())
            null
        }
    }

    fun futureTaskLater(seconds: Long, task: () -> Any): BukkitTask {
        return this.minecraftServer.scheduler.runTaskLater(this.currentPlugin, Runnable { task() }, (20 * seconds))
    }

    fun cancelFutureTask(taskId: Int) {
        this.minecraftServer.scheduler.cancelTask(taskId)
    }

    fun futureAsyncTask(task: () -> Any): Any? {
        val async = this.minecraftServer.scheduler.runTaskAsynchronously(this.currentPlugin, Runnable { task() })
        return try {
            async
        } catch (e: Throwable) {
            this.severeLog(e.toString())
            null
        }
    }
}