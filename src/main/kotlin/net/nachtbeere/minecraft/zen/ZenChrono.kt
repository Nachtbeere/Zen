package net.nachtbeere.minecraft.zen

import org.bukkit.Bukkit
import java.time.*
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.time.zone.ZoneRulesException

class ZenChrono(val config: ZenChronoConfig) {
    private val log = Bukkit.getPluginManager().getPlugin(Zen.packageName)!!.logger

    fun toZonedTime(utcDateTime: LocalDateTime): OffsetDateTime {
        return utcDateTime.atZone(config.timezone).toOffsetDateTime()
    }

    fun dryRun() {
        log.info("----Zen Dry run Result----")
        log.info("timezone: ${config.timezone}")
        log.info("begin: " + beginOfCycle().toString())
        log.info("end: " + endOfCycle().toString())
        log.info("previous begin: " + beginOfPreviousCycle().toString())
        log.info("previous end: " + endOfPreviousCycle().toString())
        log.info("----End of result----")
    }

    fun beginOfCycle(): LocalDateTime {
        return when (config.cycleUnit) {
            config.cycleCriteria.DAY -> beginOfDay()
            config.cycleCriteria.WEEK -> beginOfWeek()
            else -> throw NullPointerException()
        }
    }

    fun endOfCycle(): LocalDateTime {
        return when (config.cycleUnit) {
            config.cycleCriteria.DAY -> endOfDay()
            config.cycleCriteria.WEEK -> endOfWeek()
            else -> throw NullPointerException()
        }
    }

    fun beginOfPreviousCycle(): LocalDateTime {
        return when (config.cycleUnit) {
            config.cycleCriteria.DAY -> beginOfDay().minusDays(1)
            config.cycleCriteria.WEEK -> beginOfWeek().minusWeeks(1)
            else -> throw NullPointerException()
        }
    }

    fun endOfPreviousCycle(): LocalDateTime {
        return when (config.cycleUnit) {
            config.cycleCriteria.DAY -> endOfDay().minusDays(1)
            config.cycleCriteria.WEEK -> endOfWeek().minusWeeks(1)
            else -> throw NullPointerException()
        }
    }

    fun now(): ZonedDateTime {
        return ZonedDateTime.now(config.timezone)
    }

    fun beginOfDay(): LocalDateTime {
        val today = now()
        return ZonedDateTime.of(
            today.toLocalDate().atStartOfDay(),
            config.timezone
        ).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
    }

    fun beginOfWeek(): LocalDateTime {
        val today = now()
        val begin = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(config.cycleBeginAt)))
        return ZonedDateTime.of(
            begin.toLocalDate().atStartOfDay(),
            config.timezone
        ).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
    }

    fun endOfDay(): LocalDateTime {
        val today = now()
        return ZonedDateTime.of(
            today.toLocalDate(),
            LocalTime.of(23, 59, 59),
            config.timezone
        ).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
    }

    fun endOfYesterday(): LocalDateTime {
        return endOfDay().minusDays(1)
    }

    fun endOfWeek(): LocalDateTime {
        val today = now()
        val end = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(config.cycleBeginAt)))
        return ZonedDateTime.of(
            end.toLocalDate().minusDays(1).plusWeeks(1),
            LocalTime.of(23, 59, 59),
            config.timezone
        ).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
    }

    companion object {
        fun utcNow(): LocalDateTime {
            return OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime()
        }

        fun expiryDate(rewardBufferExpires: Long): LocalDateTime {
            return OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime().minusDays(rewardBufferExpires)
        }

        fun timestampToDateTime(timestamp: Long): LocalDateTime {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC).toLocalDateTime()

        }
    }
}