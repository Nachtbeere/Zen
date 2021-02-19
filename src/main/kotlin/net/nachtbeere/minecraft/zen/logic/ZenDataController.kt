package net.nachtbeere.minecraft.zen.logic

import net.nachtbeere.minecraft.zen.ZenChrono
import net.nachtbeere.minecraft.zen.model.ZenRewardBuffer
import net.nachtbeere.minecraft.zen.model.ZenVoteHistory
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import sun.java2d.DisposerRecord
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayDeque

class ZenDataController(): ZenLogicBase() {
    fun updateUser(uuid: UUID) {
        currentPlugin.storage!!.updateUserDate(uuid)
    }

    fun fetchLatestVoteHistory(uuid: UUID): ZenVoteHistory? {
        return currentPlugin.storage!!.fetchLatestVoteHistory(uuid)
    }

    fun writeVoteHistory(uuid: UUID, username: String, voteFrom: String, voteAt: LocalDateTime, expiredAt: LocalDateTime) {
        currentPlugin.storage!!.increaseUserVoteCount(uuid)
        currentPlugin.storage!!.writeVoteHistory(
            uuid,
            username,
            voteFrom,
            voteAt,
            expiredAt
        )
    }

    fun writeRewardHistory(uuid: UUID) {
        currentPlugin.storage!!.writeRewardHistory(uuid)
    }

    fun writeRewardBuffer(uuid: UUID) {
        currentPlugin.storage!!.writeRewardBuffer(uuid)
    }

    fun setExpireRewardBuffer(recordId: Long) {
        currentPlugin.storage!!.setExpireRewardBuffer(recordId)
    }

    fun fetchBufferedReward(uuid: UUID): ArrayDeque<ZenRewardBuffer> {
        return currentPlugin.storage!!.fetchRewardBuffers(uuid)
    }

    fun fetchOfflinePlayer(uuid: UUID): OfflinePlayer? {
        val offline = minecraftServer.getOfflinePlayer(uuid)
        return if (offline.hasPlayedBefore()) {
            offline
        } else {
            null
        }
    }

    fun fetchOnlinePlayer(uuid: UUID): Player? {
        return minecraftServer.getPlayer(uuid)
    }

    fun fetchOfflinePlayerFromOutside(username: String): OfflinePlayer {
        return minecraftServer.getOfflinePlayer(username)
    }
}