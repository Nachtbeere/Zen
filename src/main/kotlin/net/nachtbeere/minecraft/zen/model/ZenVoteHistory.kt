package net.nachtbeere.minecraft.zen.model

import com.vexsoftware.votifier.model.VotifierEvent
import net.nachtbeere.minecraft.zen.ZenChrono
import net.nachtbeere.minecraft.zen.model.ZenRewardBuffers.references
import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime
import java.util.*

object ZenVoteHistories: LongIdTable(name="zen_vote_history") {
    val uuid: Column<UUID> = uuid("uuid").references(ZenUsers.id)
    val username: Column<String> = varchar("username", 16)
    val votedFrom: Column<String> = varchar("voted_from", 36)
    val votedAt: Column<LocalDateTime> = datetime("voted_at")
    val expiredAt: Column<LocalDateTime> = datetime("expired_at")
}

class ZenVoteHistoryDAO(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<ZenVoteHistoryDAO>(ZenVoteHistories)

    var uuid by ZenVoteHistories.uuid
    var username by ZenVoteHistories.username
    var votedFrom by ZenVoteHistories.votedFrom
    var votedAt by ZenVoteHistories.votedAt
    var expiredAt by ZenVoteHistories.expiredAt

    fun load(history: ZenVoteHistory) {
        this.uuid = history.uuid
        this.username = history.username
        this.votedFrom = history.votedFrom
        this.votedAt = history.votedAt
        this.expiredAt = history.expiredAt
    }

    fun dump(): ZenVoteHistory {
        return ZenVoteHistory(id.value, uuid, username, votedFrom, votedAt, expiredAt)
    }
}

class ZenVoteHistory(
    val id: Long,
    val uuid: UUID,
    val username: String,
    val votedFrom: String,
    val votedAt: LocalDateTime,
    val expiredAt: LocalDateTime): ZenModelBase() {
    val stringUUID: String = uuid.toString()
    val offsetVotedAt = utcTimeToOffset(votedAt)
    val offsetExpiredAt = utcTimeToOffset(expiredAt)
}

