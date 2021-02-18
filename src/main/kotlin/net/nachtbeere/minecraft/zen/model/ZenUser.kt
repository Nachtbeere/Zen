package net.nachtbeere.minecraft.zen.model

import net.nachtbeere.minecraft.zen.ZenChrono
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

object ZenUsers: UUIDTable(name="zen_user") {
    val username: Column<String> = varchar("username", 16)
    val totalVote: Column<Int> = integer("total_vote").default(0)
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
}

class ZenUserDAO(uuid: EntityID<UUID>): UUIDEntity(uuid) {
    companion object: UUIDEntityClass<ZenUserDAO>(ZenUsers)

    var username by ZenUsers.username
    var totalVote by ZenUsers.totalVote
    var updatedAt by ZenUsers.updatedAt

    fun dump(): ZenUser {
        return ZenUser(id.value, username, totalVote, updatedAt)
    }
}

class ZenUser(
    val id: UUID,
    val username: String,
    val totalVote: Int,
    val updatedAt: LocalDateTime): ZenModelBase() {
    val stringUUID: String = id.toString()
    val offsetUpdatedAt = utcTimeToOffset(updatedAt)
    companion object {
        fun userOf(player: Player): ZenUser {
            return ZenUser(
                id=player.uniqueId,
                username=player.displayName,
                totalVote=0,
                updatedAt=ZenChrono.utcNow()
            )
        }
    }
}