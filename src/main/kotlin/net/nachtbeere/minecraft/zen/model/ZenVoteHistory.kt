package net.nachtbeere.minecraft.zen.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDateTime
import java.util.*

val zenVoteHistoryCreateQuery =
    "CREATE TABLE IF NOT EXISTS ${ZenVoteHistories.tableName} (" +
            "${ZenVoteHistories.id.name} INTEGER PRIMARY KEY AUTOINCREMENT," +
            "${ZenVoteHistories.uuid.name} UUID NOT NULL," +
            "${ZenVoteHistories.username.name} VARCHAR(16) NOT NULL," +
            "${ZenVoteHistories.votedFrom.name} VARCHAR(36) NOT NULL," +
            "${ZenVoteHistories.votedAt.name} TEXT NOT NULL," +
            "${ZenVoteHistories.expiredAt.name} TEXT NOT NULL," +
            "CONSTRAINT fk_${ZenVoteHistories.tableName}_uuid_id FOREIGN KEY (${ZenVoteHistories.uuid.name}) " +
            "REFERENCES ${ZenUsers.tableName}(${ZenUsers.id.name}) ON DELETE RESTRICT ON UPDATE RESTRICT)"

object ZenVoteHistories: Table<ZenVoteHistory>("zen_vote_history") {
    val id = long("id").primaryKey().bindTo { it.id }
    val uuid = mySqlUuid("uuid").references(ZenUsers) { it.user }
    val username = varchar("username").bindTo { it.username }
    val votedFrom = varchar("voted_from").bindTo { it.votedFrom }
    val votedAt = dateTimeUtc("voted_at").bindTo { it.votedAt }
    val expiredAt = dateTimeUtc("expired_at").bindTo { it.expiredAt }
}

interface ZenVoteHistory: Entity<ZenVoteHistory> {
    companion object: Entity.Factory<ZenVoteHistory>()
    val id: Long
    var uuid: UUID
    var username: String
    var votedFrom: String
    var votedAt: LocalDateTime
    var expiredAt: LocalDateTime
    var user: ZenUser
}

