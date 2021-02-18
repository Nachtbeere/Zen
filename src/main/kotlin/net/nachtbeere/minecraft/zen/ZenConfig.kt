package net.nachtbeere.minecraft.zen

import org.bukkit.Bukkit
import org.bukkit.configuration.MemorySection
import java.time.*
import java.time.zone.ZoneRulesException
import java.util.*
import kotlin.collections.HashMap

class ZenConfig(
    unsafeChronoConfig: MemorySection, unsafeStorageConfig: MemorySection,
    unsafeLogicConfig: MemorySection, unsafeMessageConfig: MemorySection,
    unsafeVoteProvider: MutableList<MutableMap<String, LinkedHashMap<String, String>>>, dataPath: String) {
    val chrono = ZenChronoConfig(unsafeConfig = unsafeChronoConfig)
    val storage = ZenStorageConfig(unsafeConfig = unsafeStorageConfig, dataPath)
    val logic = ZenLogicConfig(unsafeConfig = unsafeLogicConfig)
    val message = ZenMessageConfig(unsafeConfig = unsafeMessageConfig, unsafeVoteProvider=unsafeVoteProvider)
}

class ZenChronoConfig(unsafeConfig: MemorySection) {
    val cycleCriteria = Companion
    val timezone: ZoneId = ZoneId.of(fetchTimezone(unsafeConfig.getString("timezone")!!))
    val cycleUnit: String = cycleUnitConverter(unsafeConfig.getString("cycle_unit")!!)
    val cycleBeginAt: Int = cycleBeginConverter(unsafeConfig.getString("cycle_begin_at")!!)

    private fun cycleUnitConverter(unsafeCycle: String): String {
        return when (unsafeCycle) {
            cycleCriteria.DAY -> cycleCriteria.DAY
            cycleCriteria.WEEK -> cycleCriteria.WEEK
            cycleCriteria.MONTH -> cycleCriteria.MONTH
            cycleCriteria.YEAR -> cycleCriteria.YEAR
            else -> throw NullPointerException()
        }
    }

    private fun cycleBeginConverter(unsafeCycleBeginAt: String): Int {
        return when (cycleUnit) {
            cycleCriteria.DAY -> if (cycleCriteria.isTimeInDay(unsafeCycleBeginAt.toInt())) unsafeCycleBeginAt.toInt() else 0
            cycleCriteria.WEEK -> DAYOFWEEK.indexOf(unsafeCycleBeginAt.toLowerCase()) + 1
            cycleCriteria.MONTH -> if (cycleCriteria.isTimeInDay(unsafeCycleBeginAt.toInt())) unsafeCycleBeginAt.toInt() else 0
            cycleCriteria.YEAR -> if (cycleCriteria.isMonthInYear(unsafeCycleBeginAt.toInt())) unsafeCycleBeginAt.toInt() else 1
            else -> throw NullPointerException()
        }
    }

    private fun fetchTimezone(timezone: String): String {
        return try {
            ZoneId.of(timezone)
            timezone
        } catch (e: ZoneRulesException) {
            Bukkit.getPluginManager().getPlugin(Zen.packageName)!!.logger
                .warning("The Timezone is incorrect. It will be set to UTC for default. please check your config file.")
            "UTC"
        }
    }

    companion object {
        const val DAY: String = "day"
        const val WEEK: String = "week"
        const val MONTH: String = "month"
        const val YEAR: String = "year"
        val DAYOFWEEK: Array<String> = arrayOf(
            DayOfWeek.MONDAY.name.toLowerCase(),
            DayOfWeek.TUESDAY.name.toLowerCase(),
            DayOfWeek.WEDNESDAY.name.toLowerCase(),
            DayOfWeek.THURSDAY.name.toLowerCase(),
            DayOfWeek.FRIDAY.name.toLowerCase(),
            DayOfWeek.SATURDAY.name.toLowerCase(),
            DayOfWeek.SUNDAY.name.toLowerCase()
        )

        fun isTimeInDay(unsafeTime: Int): Boolean {
            return unsafeTime in 0..24
        }

        fun isMonthInYear(unsafeMonth: Int): Boolean {
            return unsafeMonth in 1..12
        }
    }
}

class ZenStorageConfig(unsafeConfig: MemorySection, val dataPath: String) {
    val storageMethod: String = unsafeConfig.getString("storage_method")!!
    val storageAddress: String = unsafeConfig.getString("storage_address")!!
    val storagePort: String = unsafeConfig.getString("storage_port")!!
    val storageDatabase: String = unsafeConfig.getString("storage_database")!!
    val storageUsername: String = unsafeConfig.getString("storage_username")!!
    val storagePassword: String = unsafeConfig.getString("storage_password")!!
    val useRewardHistory: Boolean = unsafeConfig.getBoolean("use_reward_history")
    val rewardBufferExpires: Long = unsafeConfig.getLong("reward_buffer_expires")
}

class ZenLogicConfig(unsafeConfig: MemorySection) {
    val permissionMethod: String = unsafeConfig.getString("permission_method")!!
    val grades: List<String> = unsafeConfig.getStringList("grades")
    val votesForGrade: List<Int> = unsafeConfig.getIntegerList("votes_for_grade")
}

data class ZenVoteProvider(
    val name: String,
    val url: String
)

class ZenMessageConfig(unsafeConfig: MemorySection, unsafeVoteProvider: MutableList<MutableMap<String, LinkedHashMap<String, String>>>) {
    var messagePrefix = ""
    var messageSuffix = ""
    private val messageMap = HashMap<ZenResultCode, String>()
    private val voteProviderMap = TreeMap<String, ZenVoteProvider>()
    private var primaryVoteProvider: ZenVoteProvider? = null

    init {
        parseVoteProvider(unsafeVoteProvider)
        primaryVoteProvider = voteProviderMap.firstEntry().value
        messagePrefix = unsafeConfig.getString("prefix")!!
        messageSuffix = unsafeConfig.getString("suffix")!!
        messageMap[ZenResultCode.SUCCESS_GRADE_INCREASED] = unsafeConfig.getString("increased")!!
        messageMap[ZenResultCode.SUCCESS_GRADE_DECREASED] = unsafeConfig.getString("decreased")!!
        messageMap[ZenResultCode.SUCCESS_STEADY] = unsafeConfig.getString("steady")!!
        messageMap[ZenResultCode.SUCCESS_WAIT_FOR_NEXT_CYCLE] = unsafeConfig.getString("next_cycle")!!
        messageMap[ZenResultCode.SUCCESS_ALREADY_HIGHEST] = unsafeConfig.getString("already_highest")!!
        messageMap[ZenResultCode.FAILED_NO_VOTE_BEFORE] = unsafeConfig.getString("no_vote_before")!!
        messageMap[ZenResultCode.FAILED_NOT_ENOUGH_VOTES] = unsafeConfig.getString("not_enough_votes")!!
        messageMap[ZenResultCode.FAILED] = unsafeConfig.getString("failed")!!
        messageMap[ZenResultCode.VOTE_NEEDED] = unsafeConfig.getString("vote_needed")!!
            .replace("\${PROVIDER}", primaryVoteProvider!!.name)
            .replace("\${URL}", primaryVoteProvider!!.url)
        messageMap[ZenResultCode.VOTE_SUCCESS] = unsafeConfig.getString("vote_success")!!
        messageMap[ZenResultCode.VOTE_SUCCESS_TODAY] = unsafeConfig.getString("vote_success_today")!!
        messageMap[ZenResultCode.VOTE_REWARD_DELIVERED] = unsafeConfig.getString("vote_reward_delivered")!!
        messageMap[ZenResultCode.VOTE_REWARD_BUFFERED] = unsafeConfig.getString("vote_reward_buffered")!!
        messageMap[ZenResultCode.VOTE_NEED_INSPECT] = unsafeConfig.getString("vote_need_inspect")!!
    }

    fun getMessage(code: ZenResultCode): String {
        return messageMap[code]!!
    }

    private fun parseVoteProvider(unsafe: MutableList<MutableMap<String, LinkedHashMap<String, String>>>) {
        for (node in unsafe) {
            val first = node.values.first()
            voteProviderMap[node.keys.first()] = ZenVoteProvider(first["name"]!!, first["url"]!!)
        }
    }
}

enum class ZenResultCode(val code: Int) {
    SUCCESS_GRADE_INCREASED(20),
    SUCCESS_ALREADY_HIGHEST(22),
    SUCCESS_WAIT_FOR_NEXT_CYCLE(24),
    SUCCESS_GRADE_DECREASED(25),
    SUCCESS_STEADY(26),
    VOTE_NEEDED(30),
    VOTE_SUCCESS(31),
    VOTE_SUCCESS_TODAY(32),
    VOTE_REWARD_DELIVERED(33),
    VOTE_REWARD_BUFFERED(34),
    VOTE_NEED_INSPECT(35),
    FAILED(40),
    FAILED_NOT_ENOUGH_VOTES(43),
    FAILED_NO_VOTE_BEFORE(44)
}