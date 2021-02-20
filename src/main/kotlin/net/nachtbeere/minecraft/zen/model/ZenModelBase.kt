package net.nachtbeere.minecraft.zen.model

import net.nachtbeere.minecraft.zen.Zen
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.Table
import java.nio.ByteBuffer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

fun <E : Any> BaseTable<E>.mySqlUuid(name: String): Column<UUID> {
    return registerColumn(name, UuidMySqlType)
}

object UuidMySqlType : SqlType<UUID>(java.sql.Types.OTHER, "mySqlUuid") {
    // https://github.com/kotlin-orm/ktorm/issues/243 code from AxelG1

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UUID) {
        val ba = asBytes(parameter)
        ps.setObject(index, ba)
    }

    override fun doGetResult(rs: ResultSet, index: Int): UUID? {
        val ba = rs.getObject(index) as ByteArray? ?: return null
        return  asUuid(ba)
    }

    fun asUuid(bytes: ByteArray?): UUID {
        val bb: ByteBuffer = ByteBuffer.wrap(bytes)
        val firstLong: Long = bb.getLong()
        val secondLong: Long = bb.getLong()
        return UUID(firstLong, secondLong)
    }

    fun asBytes(uuid: UUID): ByteArray {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}

fun <E : Any> BaseTable<E>.dateTimeUtc(name: String): Column<LocalDateTime> {
    return registerColumn(name, DateTimeUtcType)
}

object DateTimeUtcType : SqlType<LocalDateTime>(java.sql.Types.TIMESTAMP, "dateTimeUtc") {
    override fun doGetResult(rs: ResultSet, index: Int): LocalDateTime? {
        return OffsetDateTime.ofInstant(rs.getTimestamp(index).toInstant(), ZoneOffset.UTC).toLocalDateTime()
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocalDateTime) {
        ps.setTimestamp(index, Timestamp.valueOf(parameter))
    }
}

open class ZenModelBase() {
    var minecraftServer: Server = bukkitServer()
    var currentPlugin: Zen = currentPlugin() as Zen

    private fun bukkitServer(): Server = Bukkit.getServer()

    private fun currentPlugin(): Plugin = minecraftServer.pluginManager.getPlugin(Zen.packageName) as Plugin

    fun utcTimeToOffset(utcTime: LocalDateTime): OffsetDateTime {
        return currentPlugin.chrono!!.toZonedTime(utcTime)
    }

}