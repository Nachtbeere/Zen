package net.nachtbeere.minecraft.zen.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDateTime
import java.util.*

val zenRewardBufferCreateQuery =
    "CREATE TABLE IF NOT EXISTS ${ZenRewardBuffers.tableName} (" +
            "${ZenRewardBuffers.id.name} INTEGER PRIMARY KEY AUTOINCREMENT," +
            "${ZenRewardBuffers.uuid.name} UUID NOT NULL," +
            "${ZenRewardBuffers.rewardAt.name} TEXT NOT NULL," +
            "${ZenRewardBuffers.isExpired.name} BOOLEAN DEFAULT 0 NOT NULL," +
            "CONSTRAINT fk_${ZenVoteHistories.tableName}_uuid_id FOREIGN KEY (${ZenRewardBuffers.uuid.name}) " +
            "REFERENCES ${ZenUsers.tableName}(${ZenUsers.id.name}) ON DELETE RESTRICT ON UPDATE RESTRICT)"

object ZenRewardBuffers: Table<ZenRewardBuffer>("zen_reward_buffer") {
    val id = long("id").primaryKey().bindTo { it.id }
    val uuid = mySqlUuid("uuid").references(ZenUsers) { it.user }
    val rewardAt = dateTimeUtc("reward_at").bindTo { it.rewardAt }
    val isExpired = boolean("is_expired").bindTo { it.isExpired }
}

interface ZenRewardBuffer: Entity<ZenRewardBuffer> {
    companion object: Entity.Factory<ZenRewardBuffer>()
    val id: Long
    var uuid: UUID
    var rewardAt: LocalDateTime
    var isExpired: Boolean
    var user: ZenUser
}

data class ZenReward(
    val type: String,
    val code: String
) {}