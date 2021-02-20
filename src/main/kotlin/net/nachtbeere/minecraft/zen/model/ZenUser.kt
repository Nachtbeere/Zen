package net.nachtbeere.minecraft.zen.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import org.ktorm.schema.Column
import java.nio.ByteBuffer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*
import java.util.UUID

val zenUserCreateQuery =
    "CREATE TABLE IF NOT EXISTS ${ZenUsers.tableName} (" +
            "${ZenUsers.id.name} UUID NOT NULL PRIMARY KEY," +
            "${ZenUsers.username.name} VARCHAR(16) NOT NULL," +
            "${ZenUsers.totalVote.name} INT DEFAULT 0 NOT NULL," +
            "${ZenUsers.updatedAt.name} TEXT NOT NULL)"

object ZenUsers: Table<ZenUser>("zen_user") {
    var id = mySqlUuid("id").primaryKey().bindTo { it.id }
    val username = varchar("username").bindTo { it.username }
    val totalVote = int("total_vote").bindTo { it.totalVote }
    val updatedAt = dateTimeUtc("updated_at").bindTo { it.updatedAt }
}

interface ZenUser: Entity<ZenUser> {
    companion object: Entity.Factory<ZenUser>()
    var id: UUID
    var username: String
    var totalVote: Int
    var updatedAt: LocalDateTime
}

