package net.nachtbeere.minecraft.zen

import com.vexsoftware.votifier.model.VotifierEvent
import net.nachtbeere.minecraft.zen.logic.*
import org.bukkit.entity.Player
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList

data class ZenUserGrade(
    val index: Int,
    val name: String,
)

data class ZenReward(
    val type: String,
    val code: String,
    val enchant: String
)

data class ZenResult(
    val code: ZenResultCode,
    val detail: Int
)

class ZenLogic(private val logicConfig: ZenLogicConfig): ZenLogicBase() {
    private val inspector = ZenInspector(logicConfig)
    private val permission = ZenPermissionFactory().create(logicConfig.permissionMethod, logicConfig.grades)
    private val dataController = ZenDataController()
    private val rewards = ZenRewardFactory().create(logicConfig.voteRewardType, logicConfig.voteRewardCode,
         logicConfig.voteRewardAmount, logicConfig.voteRewardName,
        logicConfig.isVoteRewardHasEnchant, logicConfig.availableVoteRewardEnchant)

    fun inspectPlayer(player: Player, isLateInspect: Boolean): ArrayList<ZenResult> {
        log("execute inspect player")
        val resultDataArray = arrayListOf<ZenResult>()
        inspector.preInspect(player)
        val grade = packGrade(permission.getPermissionGroup(player.uniqueId))
        val latest = dataController.fetchLatestVoteHistory(player.uniqueId)
        log(grade.toString())
        val resultData = inspector.inspect(uuid=player.uniqueId, currentGrade=grade, latest=latest, isLateInspect=isLateInspect)
        resultDataArray.add(resultData)
        log(resultData.code.toString())
        when (resultData.code) {
            ZenResultCode.SUCCESS_GRADE_INCREASED -> increaseGrade(player.uniqueId, logicConfig.grades[resultData.detail])
            ZenResultCode.SUCCESS_GRADE_DECREASED -> decreaseGrade(player.uniqueId, logicConfig.grades[resultData.detail])
            ZenResultCode.SUCCESS_STEADY -> steadyGrade(player.uniqueId)
            else -> {}
        }
        if (!inspector.isVoteToday(latest)) {
            resultDataArray.add(ZenResult(ZenResultCode.VOTE_NEEDED, -1))
        }
        log("inspect player result: $resultDataArray")
        return resultDataArray
    }

    fun proceedVote(event: VotifierEvent): ArrayList<ZenResult> {
        val resultDataArray = arrayListOf<ZenResult>()
        val uuid = permission.getUUIDByUsername(event.vote.username)
        if (uuid != null) {
            val voteAt = ZenChrono.timestampToDateTime(event.vote.timeStamp.toLong())
            dataController.writeVoteHistory(
                uuid, event.vote.username, event.vote.serviceName, voteAt, currentPlugin.chrono!!.endOfCycle())
            if (voteAt > currentPlugin.chrono!!.beginOfDay()) {
                resultDataArray.add(ZenResult(ZenResultCode.VOTE_SUCCESS_TODAY, -1))
                val grade = permission.getPermissionGroupByUsername(event.vote.username)
                if (grade != null) {
                    log("proceed vote user current grade: $grade")
                    if (grade == logicConfig.grades.first()) {
                        val offline = dataController.fetchOfflinePlayer(uuid)
                        if (offline != null && offline.isOnline) {
                            resultDataArray.add(ZenResult(ZenResultCode.SUCCESS_GRADE_INCREASED, 0))
                        }
                    }
                }
            } else {
                resultDataArray.add(ZenResult(ZenResultCode.VOTE_SUCCESS, -1))
                if (voteAt >= currentPlugin.chrono!!.beginOfPreviousCycle() &&
                    voteAt <= currentPlugin.chrono!!.endOfPreviousCycle()) {
                    resultDataArray.add(ZenResult(ZenResultCode.VOTE_NEED_INSPECT, -1))
                }
            }
        }
        log("proceed vote result: $resultDataArray")
        return resultDataArray
    }

    fun deliverReward(player: Player): ArrayList<ZenResult> {
        val resultDataArray = arrayListOf<ZenResult>()
        val result = rewards.sendReward(player)
        if (result.code == ZenResultCode.VOTE_REWARD_BUFFERED) {
            dataController.writeRewardBuffer(player.uniqueId)
        } else {
            dataController.writeRewardHistory(player.uniqueId)
        }
        resultDataArray.add(result)
        return resultDataArray
    }

    fun deliverBufferedReward(player: Player): ArrayList<ZenResult> {
        log("execute deliver buffered reward")
        val resultDataArray = arrayListOf<ZenResult>()
        val bufferedRewards = dataController.fetchBufferedReward(player.uniqueId)
        for (reward in bufferedRewards) {
            val result = rewards.sendReward(player)
            if (result.code == ZenResultCode.VOTE_REWARD_DELIVERED) {
                dataController.setExpireRewardBuffer(reward.id)
            }
            resultDataArray.add(result)
        }
        log("deliver buffered reward result: $resultDataArray")
        return resultDataArray
    }

    fun instantPromotion(player: Player) {
        increaseGrade(player.uniqueId, logicConfig.grades[1])
    }

    private fun increaseGrade(uuid: UUID, possibleGradeLabel: String) {
        permission.setPermissionGroup(uuid, possibleGradeLabel)
        dataController.updateUser(uuid)
    }

    private fun decreaseGrade(uuid: UUID, possibleGradeLabel: String) {
        permission.setPermissionGroup(uuid, possibleGradeLabel)
        dataController.updateUser(uuid)
    }

    private fun steadyGrade(uuid: UUID) {
        dataController.updateUser(uuid)
    }

    private fun gradeIndex(grade: String): Int {
        return logicConfig.grades.indexOf(grade)
    }

    private fun packGrade(currentGroup: String): ZenUserGrade {
        return ZenUserGrade(index=gradeIndex(currentGroup), name=currentGroup)
    }
}