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
    val rewardAt: Column<LocalDateTime> = datetime("reward_at")
    val isExpired: Column<Boolean> = bool("is_expired").default(false)
}

class ZenRewardBufferDAO(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<ZenRewardBufferDAO>(ZenRewardBuffers)

    var uuid by ZenRewardBuffers.uuid
    var rewardAt by ZenRewardBuffers.rewardAt
    var isExpired by ZenRewardBuffers.isExpired

    fun dump(): ZenRewardBuffer {
        return ZenRewardBuffer(id.value, uuid, rewardAt, isExpired)
    }
}

class ZenRewardBuffer(
    val id: Long,
    val uuid: UUID,
    val rewardAt: LocalDateTime,
    val isExpired: Boolean
): ZenModelBase() {
    val stringUUID: String = uuid.toString()
    val offsetRewardAt = utcTimeToOffset(rewardAt)
}

data class ZenReward(
    val type: String,
    val code: String
) {}