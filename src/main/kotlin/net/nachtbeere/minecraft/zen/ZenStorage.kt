package net.nachtbeere.minecraft.zen

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.nachtbeere.minecraft.zen.model.*
import org.bukkit.entity.Player
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayDeque

//object KotlinSqlLogger: SqlLogger {
//    private val logger = KotlinLogging.logger {}
//    override fun log(context: StatementContext, transaction: Transaction) {
//        logger.info { "Query: ${context.expandArgs(transaction)}"}
//    }
//}

data class ZenQueuedTask(
    val player: Player,
    val resultCode: ZenResultCode,
    val method: Any
)

class ZenStorage(private val config: ZenStorageConfig) {
    private val taskMap: HashMap<UUID, Int> = hashMapOf()
    private val ds: DataSource = ZenStorageDriverFactory(config).create()
    private val database = Database.connect(ds)

    init {
        database.useConnection { conn ->
            conn.prepareStatement(zenUserCreateQuery).use { it.executeUpdate() }
            conn.prepareStatement(zenVoteHistoryCreateQuery).use { it.executeUpdate() }
            conn.prepareStatement(zenRewardBufferCreateQuery).use { it.executeUpdate() }
            conn.prepareStatement(zenRewardHistoryCreateQuery).use { it.executeUpdate() }
        }
    }

    fun initialize() {
        purgeExpiredRewardBuffers()
        bulkSetExpiredRewardBuffers()
    }

    fun writeTaskId(uuid: UUID, taskId: Int) {
        taskMap[uuid] = taskId
    }

    fun fetchTaskId(uuid: UUID): Int? {
        return taskMap[uuid]
    }

    fun writeUser(playerUUID: UUID, playerName: String) {
        database.useTransaction {
            val user = ZenUser {
                this.id = playerUUID
                this.username = playerName
                this.updatedAt = ZenChrono.utcNow()
            }
            database.sequenceOf(ZenUsers).add(user)
        }
    }

    fun updateUserDate(playerUUID: UUID) {
        database.useTransaction {
            database.update(ZenUsers) {
                set(it.updatedAt, ZenChrono.utcNow())
                where {
                    it.id eq playerUUID
                }
            }
        }
    }

    fun increaseUserVoteCount(playerUUID: UUID) {
        database.useTransaction {
            database.update(ZenUsers) {
                set(it.totalVote, it.totalVote + 1)
                where {
                    it.id eq playerUUID
                }
            }
        }
    }

    fun writeVoteHistory(playerUUID: UUID, playerName: String, voteSource: String, voteDate: LocalDateTime, expiryDate: LocalDateTime) {
        database.useTransaction {
            database.insert(ZenVoteHistories) {
                set(it.uuid, playerUUID)
                set(it.username, playerName)
                set(it.votedFrom, voteSource)
                set(it.votedAt, voteDate)
                set(it.expiredAt, expiryDate)
            }
        }
    }

    fun writeRewardHistory(playerUUID: UUID) {
        if (config.useRewardHistory) {
            database.useTransaction {
                database.insert(ZenRewardHistories) {
                    set(it.uuid, playerUUID)
                    set(it.receivedAt, ZenChrono.utcNow())
                }
            }
        }
    }

    fun writeRewardBuffer(playerUUID: UUID) {
        database.useTransaction {
            database.insert(ZenRewardBuffers) {
                set(it.uuid, playerUUID)
                set(it.rewardAt, ZenChrono.utcNow())
            }
        }
    }

    fun isZenUserExist(uuid: UUID): Boolean {
        return fetchZenUser(uuid) != null
    }

    fun fetchZenUser(uuid: UUID): ZenUser? {
        val user = database.useTransaction {
            database.sequenceOf(ZenUsers).find { it.id eq uuid }
        }
        return user
    }

    fun fetchLatestVoteHistory(uuid: UUID): ZenVoteHistory? {
        val history = database.useTransaction {
            database.sequenceOf(ZenVoteHistories)
                .sortedBy { it.votedAt.desc() }
                .find { ZenVoteHistories.uuid eq uuid }
        }
        return history
    }

    fun fetchVoteHistories(uuid: UUID, previousBegin: LocalDateTime, previousEnd: LocalDateTime): ArrayDeque<ZenVoteHistory> {
        val histories = ArrayDeque<ZenVoteHistory>()
        database.useTransaction {
            database.from(ZenVoteHistories).select().where {
                (ZenVoteHistories.uuid eq uuid) and (ZenVoteHistories.votedAt.between(previousBegin..previousEnd))
            }.orderBy(ZenVoteHistories.votedAt.desc())
                .map {
                    histories.add(ZenVoteHistories.createEntity(it, withReferences = false))
                }
        }
        return histories
    }

    fun fetchRewardHistories(uuid: UUID): ArrayDeque<ZenRewardHistory> {
        val histories = ArrayDeque<ZenRewardHistory>()
        database.useTransaction {
            database.from(ZenRewardHistories).select().where {
                ZenRewardHistories.uuid eq uuid
            }.orderBy(ZenRewardHistories.receivedAt.desc())
                .map {
                    histories.add(ZenRewardHistories.createEntity(it, withReferences = false))
                }
        }
        return histories
    }

    fun fetchRewardBuffers(uuid: UUID): ArrayDeque<ZenRewardBuffer> {
        val buffers = ArrayDeque<ZenRewardBuffer>()
        database.useTransaction {
            database.from(ZenRewardBuffers).select().where {
                (ZenRewardBuffers.uuid eq uuid) and (ZenRewardBuffers.isExpired eq false)
            }.orderBy(ZenRewardBuffers.rewardAt.asc())
                .map {
                    buffers.add(ZenRewardBuffers.createEntity(it, withReferences = false))
                }
        }
        return buffers
    }

    fun setExpireRewardBuffer(recordId: Long) {
        database.useTransaction {
            database.update(ZenRewardBuffers) {
                set(it.isExpired, true)
                where {
                    it.id eq recordId
                }
            }
        }

    }

    fun bulkSetExpiredRewardBuffers() {
        database.useTransaction {
            database.update(ZenRewardBuffers) {
                set(it.isExpired, true)
                where {
                    it.rewardAt less ZenChrono.expiryDate(config.rewardBufferExpires)
                }
            }
        }
    }

    fun purgeExpiredRewardBuffers() {
        database.useTransaction {
            database.delete(ZenRewardBuffers) {
                it.isExpired eq true
            }
        }
    }

    fun purgeExpiredVoteHistory(previousBegin: LocalDateTime) {
        database.useTransaction {
            database.delete(ZenVoteHistories) {
                it.expiredAt less previousBegin
            }
        }
    }
}

class ZenStorageDriverFactory(private val config: ZenStorageConfig) {
    var hikariConfig: HikariConfig = HikariConfig()
    val jdbcURL: StringBuilder = StringBuilder()

    fun create(): DataSource {
        hikariConfig.username = config.storageUsername
        hikariConfig.password = config.storagePassword
        return when (config.storageMethod) {
            "sqlite" -> sqlite()
            "mysql" -> mysql()
            else -> throw NullPointerException()
        }
    }

    private fun sqlite(): DataSource {
        hikariConfig.driverClassName = "org.sqlite.JDBC"
        hikariConfig.jdbcUrl = jdbcURL.append("jdbc:sqlite:/")
                                      .append(config.dataPath)
                                      .append("/")
                                      .append(config.storageDatabase)
                                      .append(".db")
                                      .toString()
        return HikariDataSource(hikariConfig)
    }

    private fun mysql(): DataSource {
        hikariConfig.driverClassName = "com.mysql.jdbc.Driver"
        hikariConfig.jdbcUrl = jdbcURL.append("jdbc:mysql://")
                                      .append(config.storageAddress)
                                      .append(":")
                                      .append(config.storagePort)
                                      .append("/")
                                      .append(config.storageDatabase)
                                      .toString()
        return HikariDataSource(hikariConfig)
    }

}