package net.nachtbeere.minecraft.zen.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDateTime
import java.util.*

val zenRewardHistoryCreateQuery =
    "CREATE TABLE IF NOT EXISTS ${ZenRewardHistories.tableName} (" +
            "${ZenRewardHistories.id.name} INTEGER PRIMARY KEY AUTOINCREMENT," +
            "${ZenRewardHistories.uuid.name} UUID NOT NULL," +
            "${ZenRewardHistories.receivedAt.name} TEXT NOT NULL," +
            "CONSTRAINT fk_${ZenRewardHistories.tableName}_uuid_id FOREIGN KEY (${ZenRewardHistories.uuid.name}) " +
            "REFERENCES ${ZenUsers.tableName}(${ZenUsers.id.name}) ON DELETE RESTRICT ON UPDATE RESTRICT)"

object ZenRewardHistories: Table<ZenRewardHistory>("zen_reward_history") {
    val id = long("id").primaryKey().bindTo { it.id }
    val uuid = mySqlUuid("uuid").references(ZenUsers) { it.user }
    val receivedAt = dateTimeUtc("received_at").bindTo { it.receivedAt }
}

interface ZenRewardHistory: Entity<ZenRewardHistory> {
    companion object: Entity.Factory<ZenRewardHistory>()
    val id: Long
    var uuid: UUID
    var receivedAt: LocalDateTime
    var user: ZenUser
}