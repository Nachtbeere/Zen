package net.nachtbeere.minecraft.zen.logic

import net.nachtbeere.minecraft.zen.ZenLogicConfig
import net.nachtbeere.minecraft.zen.ZenResult
import net.nachtbeere.minecraft.zen.ZenResultCode
import net.nachtbeere.minecraft.zen.ZenUserGrade
import net.nachtbeere.minecraft.zen.model.ZenUser
import net.nachtbeere.minecraft.zen.model.ZenVoteHistory
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.ArrayDeque

data class ZenInspectedPlayer(
    val user: ZenUser,
    val latest: ZenVoteHistory?,
    val histories: ArrayDeque<ZenVoteHistory>,
    val grade: ZenUserGrade
)


class ZenInspector(private val config: ZenLogicConfig): ZenLogicBase() {
    fun preInspect(player: Player) {
        if (!currentPlugin.storage!!.isZenUserExist(player.uniqueId)) {
            currentPlugin.storage!!.writeUser(player.uniqueId, player.displayName)
        }
    }

    fun inspect(uuid: UUID, currentGrade: ZenUserGrade, latest: ZenVoteHistory?, isLateInspect: Boolean): ZenResult {
        val inspectedPlayer = ZenInspectedPlayer(
            user = currentPlugin.storage!!.fetchZenUser(uuid)!!,
            latest = latest,
            histories = currentPlugin.storage!!.fetchVoteHistories(
                uuid,
                currentPlugin.chrono!!.beginOfPreviousCycle(),
                currentPlugin.chrono!!.endOfPreviousCycle()
            ),
            grade = currentGrade
        )
        log(inspectedPlayer.toString())
        return if (inspectedPlayer.grade.index >= 0) {
            val beginOfCycle = currentPlugin.chrono!!.beginOfCycle()
            if (inspectedPlayer.latest == null) {
                ZenResult(ZenResultCode.FAILED_NO_VOTE_BEFORE, 0)
            } else {
                if (inspectedPlayer.grade.index == 0) {
                    if (inspectedPlayer.latest.votedAt > beginOfCycle) {
                        ZenResult(ZenResultCode.SUCCESS_GRADE_INCREASED, 1)
                    } else if (isNextGradeAvailable(inspectedPlayer.histories, inspectedPlayer.grade.index)) {
                        ZenResult(ZenResultCode.SUCCESS_GRADE_INCREASED, possibleMaxGrade(
                            inspectedPlayer.grade.index, inspectedPlayer.histories.size))
                    } else {
                        ZenResult(ZenResultCode.FAILED_NOT_ENOUGH_VOTES, -1)
                    }
                } else if (!isLateInspect && inspectedPlayer.user.updatedAt > beginOfCycle) {
                    ZenResult(ZenResultCode.SUCCESS_WAIT_FOR_NEXT_CYCLE, -1)
                } else {
                    if (isNextGradeAvailable(inspectedPlayer.histories, inspectedPlayer.grade.index)) {
                        if (inspectedPlayer.grade.index == config.grades.lastIndex) {
                            ZenResult(ZenResultCode.SUCCESS_ALREADY_HIGHEST, -1)
                        } else {
                            ZenResult(ZenResultCode.SUCCESS_GRADE_INCREASED, possibleMaxGrade(
                                inspectedPlayer.grade.index, inspectedPlayer.histories.size))
                        }
                    } else {
                        if (isSteady(inspectedPlayer.histories, inspectedPlayer.grade.index)) {
                            ZenResult(ZenResultCode.SUCCESS_STEADY, -1)
                        } else {
                            ZenResult(ZenResultCode.SUCCESS_GRADE_DECREASED, possibleMinGrade(
                                inspectedPlayer.grade.index, inspectedPlayer.histories.size))
                        }
                    }
                }
            }
        } else {
            ZenResult(ZenResultCode.FAILED, -1)
        }
    }

    fun isVoteToday(latest: ZenVoteHistory?): Boolean {
        return if (latest != null) {
            latest.votedAt > currentPlugin.chrono!!.beginOfDay()
        } else {
            false
        }
    }

    private fun isNextGradeAvailable(userRecords: ArrayDeque<ZenVoteHistory>?, gradeIndex: Int): Boolean {
        var next = gradeIndex + 1
        if (next > config.grades.lastIndex) {
            next = config.grades.lastIndex
        }
        return if (userRecords != null) {
            userRecords.count() >= config.votesForGrade[next]
        } else {
            false
        }
    }

    private fun isSteady(userRecords: ArrayDeque<ZenVoteHistory>?, gradeIndex: Int): Boolean {
        return if (userRecords != null) {
            userRecords.count() >= config.votesForGrade[gradeIndex]
        } else {
            false
        }
    }

    private fun possibleMaxGrade(gradeIndex: Int, totalVote: Int): Int {
        val next = nextGradeIndex(gradeIndex)
        return if (next < config.grades.lastIndex && config.votesForGrade[next] < totalVote) {
            possibleMaxGrade(next, totalVote)
        } else {
            next
        }
    }

    private fun possibleMinGrade(gradeIndex: Int, totalVote: Int): Int {
        val previous = previousGradeIndex(gradeIndex)
        return if (previous > 0 && config.votesForGrade[previous] > totalVote) {
            possibleMinGrade(previous, totalVote)
        } else {
            previous
        }
    }

    private fun nextGradeIndex(gradeIndex: Int): Int {
        val next = gradeIndex + 1
        return if (next <= config.grades.lastIndex) {
            next
        } else {
            gradeIndex
        }
    }

    private fun previousGradeIndex(gradeIndex: Int): Int {
        val previous = gradeIndex - 1
        return if (previous >= 0) {
            previous
        } else {
            gradeIndex
        }
    }

    private fun cleanUp() {
        futureTask {

        }
    }

}