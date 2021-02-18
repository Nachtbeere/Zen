package net.nachtbeere.minecraft.zen.model

import com.vexsoftware.votifier.model.VotifierEvent
import net.nachtbeere.minecraft.zen.ZenChrono
import net.nachtbeere.minecraft.zen.model.ZenRewardBuffers.references
import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

object ZenRewardHistories: LongIdTable(name="zen_reward_history") {
    val uuid: Column<UUID> = uuid("uuid").references(ZenUsers.id)
    val reward: Column<String> = varchar("reward", 36)
    val rewardFrom: Column<String> = varchar("reward_from", 36)
    val receivedAt: Column<LocalDateTime> = datetime("received_at")
}

class ZenRewardHistoryDAO(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<ZenRewardHistoryDAO>(ZenRewardHistories)

    var uuid by ZenRewardHistories.uuid
    var reward by ZenRewardHistories.reward
    var rewardFrom by ZenRewardHistories.rewardFrom
    var receivedAt by ZenRewardHistories.receivedAt

    fun load(history: ZenRewardHistory) {
        this.uuid = history.uuid
        this.reward = history.reward
        this.rewardFrom = history.rewardFrom
        this.receivedAt = history.receivedAt
    }

    fun dump(): ZenRewardHistory {
        return ZenRewardHistory(uuid, reward, rewardFrom, receivedAt)
    }
}

class ZenRewardHistory(
    val uuid: UUID,
    val reward: String,
    val rewardFrom: String,
    val receivedAt: LocalDateTime,
): ZenModelBase() {
    val stringUUID: String = uuid.toString()
    val offsetReceivedAt = utcTimeToOffset(receivedAt)
    companion object {
        fun rewardRecordOf(event: VotifierEvent, reward: String): ZenRewardHistory {
            return ZenRewardHistory(
                uuid=Bukkit.getPlayerUniqueId(event.vote.username)!!,
                reward=reward,
                rewardFrom=event.vote.serviceName,
                receivedAt=ZenChrono.utcNow()
            )
        }
    }
}