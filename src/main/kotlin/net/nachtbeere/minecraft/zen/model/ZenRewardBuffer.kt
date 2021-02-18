package net.nachtbeere.minecraft.zen.model

import com.vexsoftware.votifier.model.VotifierEvent
import net.nachtbeere.minecraft.zen.ZenChrono
import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

object ZenRewardBuffers: LongIdTable(name="zen_reward_buffer") {
    val uuid: Column<UUID> = uuid("uuid").references(ZenUsers.id)
    val rewardType: Column<String> = varchar("reward_type", 16)
    val rewardCode: Column<String> = varchar("reward_code", 16)
    val rewardFrom: Column<String> = varchar("reward_from", 36)
    val rewardAt: Column<LocalDateTime> = datetime("reward_at")
    val isExpired: Column<Boolean> = bool("is_expired").default(false)
}

class ZenRewardBufferDAO(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<ZenRewardBufferDAO>(ZenRewardBuffers)

    var uuid by ZenRewardBuffers.uuid
    var rewardType by ZenRewardBuffers.rewardType
    var rewardCode by ZenRewardBuffers.rewardCode
    var rewardFrom by ZenRewardBuffers.rewardFrom
    var rewardAt by ZenRewardBuffers.rewardAt
    var isExpired by ZenRewardBuffers.isExpired

    fun load(buffer: ZenRewardBuffer) {
        this.uuid = buffer.uuid
        this.rewardType = buffer.rewardType
        this.rewardCode = buffer.rewardCode
        this.rewardFrom = buffer.rewardFrom
        this.rewardAt = buffer.rewardAt
        this.isExpired = buffer.isExpired
    }

    fun dump(): ZenRewardBuffer {
        return ZenRewardBuffer(uuid, rewardType, rewardCode, rewardFrom, rewardAt, isExpired)
    }
}

class ZenRewardBuffer(
    val uuid: UUID,
    val rewardType: String,
    val rewardCode: String,
    val rewardFrom: String,
    val rewardAt: LocalDateTime,
    val isExpired: Boolean
): ZenModelBase() {
    val stringUUID: String = uuid.toString()
    val offsetRewardAt = utcTimeToOffset(rewardAt)
    companion object {
        fun rewardBufferOf(event: VotifierEvent, reward: ZenReward): ZenRewardBuffer {
            return ZenRewardBuffer(
                uuid = Bukkit.getPlayerUniqueId(event.vote.username)!!,
                rewardType = reward.type,
                rewardCode = reward.code,
                rewardFrom = event.vote.serviceName,
                rewardAt = ZenChrono.utcNow(),
                isExpired = false
            )
        }
    }
}

data class ZenReward(
    val type: String,
    val code: String
) {}