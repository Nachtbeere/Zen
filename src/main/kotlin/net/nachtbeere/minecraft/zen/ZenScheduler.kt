package net.nachtbeere.minecraft.zen

import net.nachtbeere.minecraft.zen.logic.ZenLogicBase
import org.bukkit.scheduler.BukkitTask
import java.util.*

class ZenScheduler(): ZenLogicBase() {
    fun reserveTask(task: ZenQueuedTask) {
        cancelExistTask(task.player.uniqueId)
        var future: BukkitTask? = null
        when (task.resultCode) {
            ZenResultCode.VOTE_NEED_INSPECT -> {
                 future = futureTaskLater(3) {
                     val broadcast = ZenBroadcast(currentPlugin.zenConfig!!.message)
                     broadcast.messageToPlayer(ZenResultCode.VOTE_NEED_INSPECT, task.player)
                     task.method as ZenLogic
                     val resultData = task.method.inspectPlayer(task.player, isLateInspect=true)
                     for (result in resultData) {
                         if (result.code != ZenResultCode.VOTE_NEEDED) {
                             broadcast.messageToPlayer(result.code, task.player)
                         }
                     }
                }
            }
            else -> {}
        }
        if (future != null) {
            registerTaskId(task.player.uniqueId, future.taskId)
        }
    }

    private fun registerTaskId(uuid: UUID, taskId: Int) {
        currentPlugin.storage!!.writeTaskId(uuid, taskId)
    }

    private fun cancelExistTask(uuid: UUID) {
        val taskId = currentPlugin.storage!!.fetchTaskId(uuid)
        if (taskId != null) {
            cancelFutureTask(taskId)
        }
    }
}