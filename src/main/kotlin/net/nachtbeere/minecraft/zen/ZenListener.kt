package net.nachtbeere.minecraft.zen

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import com.vexsoftware.votifier.model.VotifierEvent
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable

class ZenListener(private val pluginInstance: Zen): Listener {
    private val logic = ZenLogic(pluginInstance.zenConfig!!.logic)
    private val broadcast = ZenBroadcast(pluginInstance.zenConfig!!.message)

    init {
        pluginInstance.server.pluginManager.registerEvents(this, pluginInstance)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerVoted(event: VotifierEvent) {
        pluginInstance.logger.info("player vote execute")
        val player = pluginInstance.server.getPlayer(event.vote.username)
        if (player != null && player.isOnline()) {
            val resultData = logic.proceedVote(event)
            resultData.addAll(logic.deliverReward(player))
            for (result in resultData) {
                when (result.code) {
                    ZenResultCode.VOTE_SUCCESS_TODAY -> broadcast.broadcast(result.code, pluginInstance.server)
                    ZenResultCode.VOTE_REWARD_DELIVERED -> broadcast.messageToPlayer(result.code, player)
                    ZenResultCode.VOTE_REWARD_BUFFERED -> broadcast.messageToPlayer(result.code, player)
                    ZenResultCode.VOTE_NEED_INSPECT -> {
                        pluginInstance.scheduler!!.reserveTask(
                            ZenQueuedTask(player, result.code, logic)
                        )
                    }
                    ZenResultCode.SUCCESS_GRADE_INCREASED -> {
                        logic.instantPromotion(player)
                        broadcast.messageToPlayer(result.code, player)
                    }
                    else -> {}
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        pluginInstance.logger.info("player join execute")
        val resultData = logic.inspectPlayer(event.player, isLateInspect=false)
        resultData.addAll(logic.deliverBufferedReward(event.player))
        for (result in resultData) {
            broadcast.messageToPlayer(result.code, event.player)
        }
    }
}