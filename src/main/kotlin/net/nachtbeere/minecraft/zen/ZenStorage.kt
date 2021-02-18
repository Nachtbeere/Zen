package net.nachtbeere.minecraft.zen

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import net.nachtbeere.minecraft.zen.model.*
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayDeque

object KotlinSqlLogger: SqlLogger {
    private val logger = KotlinLogging.logger {}
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.info { "Query: ${context.expandArgs(transaction)}"}
    }
}

data class ZenQueuedTask(
    val player: Player,
    val resultCode: ZenResultCode,
    val method: Any
)

class ZenStorage(private val config: ZenStorageConfig) {
    val inspectQueue: Queue<ZenQueuedTask> = LinkedList()
    private val taskMap: HashMap<UUID, Int> = hashMapOf()
    private val db: DataSource = ZenStorageDriverFactory(config).create()

    init {
        transaction(Database.connect(this.db)) {
            addLogger(KotlinSqlLogger)
            SchemaUtils.create(ZenUsers)
            SchemaUtils.create(ZenRewardBuffers)
            SchemaUtils.create(ZenVoteHistories)
            SchemaUtils.create(ZenRewardHistories)
        }
    }

    fun writeTaskId(uuid: UUID, taskId: Int) {
        taskMap[uuid] = taskId
    }

    fun fetchTaskId(uuid: UUID): Int? {
        return taskMap[uuid]
    }

    fun initialize() {
        this.purgeExpiredRewardBuffers()
        this.setExpiredRewardBuffers()
    }

    fun writeUser(playerUUID: UUID, playerName: String) {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenUserDAO.new(playerUUID) {
                username = playerName
                totalVote = 0
                updatedAt = ZenChrono.utcNow()
            }
        }
    }

    fun updateUserDate(playerUUID: UUID) {
        transaction {
            addLogger(KotlinSqlLogger)
            val user = ZenUserDAO.findById(playerUUID)
            if (user != null) {
                user.updatedAt = ZenChrono.utcNow()
            }
        }
    }

    fun increaseUserVoteCount(playerUUID: UUID) {
        transaction {
            addLogger(KotlinSqlLogger)
            val user = ZenUserDAO.findById(playerUUID)
            if (user != null) {
                user.totalVote++
            }
        }
    }

    fun writeVoteHistory(playerUUID: UUID, playerName: String, voteSource: String, voteDate: LocalDateTime, expiryDate: LocalDateTime) {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenVoteHistoryDAO.new {
                uuid = playerUUID
                username = playerName
                votedFrom = voteSource
                votedAt = voteDate
                expiredAt = expiryDate
            }
        }
    }

    fun writeRewardHistory(player: Player, rewardData: ZenRewardBuffer) {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardHistoryDAO.new {
                uuid = player.uniqueId
                reward = StringBuilder().append(rewardData.rewardType).append("#").append(rewardData.rewardCode).toString()
                rewardFrom = rewardData.rewardFrom
                receivedAt = ZenChrono.utcNow()
            }
        }
    }

    fun writeRewardBuffer(playerUUID: UUID, ) {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardBufferDAO.new {
                uuid = playerUUID
                rewardType = rewardType
                rewardCode = rewardCode
                rewardFrom = rewardFrom
                rewardAt = ZenChrono.utcNow()
                isExpired = false
            }
        }
    }

    fun isZenUserExist(uuid: UUID): Boolean {
        return fetchZenUser(uuid) != null
    }

    fun fetchZenUser(uuid: UUID): ZenUser? {
        val user = transaction {
            addLogger(KotlinSqlLogger)
            ZenUserDAO.findById(uuid)
        }
        return user?.dump()
    }

    fun fetchLatestVoteHistory(uuid: UUID): ZenVoteHistory? {
        val history = transaction {
            addLogger(KotlinSqlLogger)
            ZenVoteHistoryDAO
                .find { ZenVoteHistories.uuid eq uuid }
                .orderBy(ZenVoteHistories.votedAt to SortOrder.DESC)
                .firstOrNull()
        }
        return history?.dump()
    }

    fun fetchVoteHistories(uuid: UUID, previousBegin: LocalDateTime, previousEnd: LocalDateTime): ArrayDeque<ZenVoteHistory> {
        val histories = ArrayDeque<ZenVoteHistory>()
        transaction {
            addLogger(KotlinSqlLogger)
            println(ZenVoteHistoryDAO
                .find { (ZenVoteHistories.uuid eq uuid) and (ZenVoteHistories.votedAt.between(previousBegin, previousEnd)) }
                .orderBy(ZenVoteHistories.votedAt to SortOrder.DESC).count()
                )
            ZenVoteHistoryDAO
                .find { (ZenVoteHistories.uuid eq uuid) and (ZenVoteHistories.votedAt.between(previousBegin, previousEnd)) }
                .orderBy(ZenVoteHistories.votedAt to SortOrder.DESC)
                .forEach {
                    println(it)
                    histories.add(it.dump()) }
        }
        println(histories)
        return histories
    }

    fun fetchRewardHistories(uuid: UUID): ArrayDeque<ZenRewardHistory> {
        val histories = ArrayDeque<ZenRewardHistory>()
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardHistoryDAO
                .find { ZenRewardHistories.uuid eq uuid }
                .orderBy(ZenRewardHistories.receivedAt to SortOrder.DESC)
                .all{ histories.add(it.dump()) }
        }
        return histories
    }

    fun fetchRewardBuffers(uuid: UUID): ArrayDeque<ZenRewardBuffer> {
        val buffers = ArrayDeque<ZenRewardBuffer>()
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardBufferDAO
                .find { (ZenRewardBuffers.uuid eq uuid) and (ZenRewardBuffers.isExpired eq Op.FALSE) }
                .orderBy(ZenRewardBuffers.rewardAt to SortOrder.ASC)
                .all{ buffers.add(it.dump()) }
        }
        return buffers
    }

    fun setExpiredRewardBuffers() {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardBufferDAO.find {
                ZenRewardBuffers.rewardAt.less(ZenChrono.expiryDate(config.rewardBufferExpires))
            }.forEach {
                it.isExpired = true
            }
        }
    }

    fun purgeExpiredRewardBuffers() {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardBufferDAO.find {
                ZenRewardBuffers.isExpired eq Op.TRUE
            }.forEach {
                it.delete()
            }
        }
    }

    fun purgeExpiredVoteHistory(previousBegin: LocalDateTime) {
        transaction {
            addLogger(KotlinSqlLogger)
            ZenRewardBufferDAO.find {
                ZenVoteHistories.expiredAt.less(previousBegin)
            }.forEach {
                it.delete()
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