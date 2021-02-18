package net.nachtbeere.minecraft.zen

import org.bukkit.configuration.MemorySection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Zen : JavaPlugin() {
    companion object {
        const val packageName = "zen"
    }

    private var zenListener: ZenListener? = null
    var zenConfig: ZenConfig? = null
    var storage: ZenStorage? = null
    var chrono: ZenChrono? = null
    var scheduler: ZenScheduler? = null

    private fun loadConfig() {
        zenConfig = ZenConfig(
            unsafeChronoConfig = this.config.get("chrono") as MemorySection,
            unsafeStorageConfig = this.config.get("storage") as MemorySection,
            unsafeLogicConfig = this.config.get("logic") as MemorySection,
            unsafeMessageConfig = this.config.get("message") as MemorySection,
            unsafeVoteProvider = this.config.getMapList("vote_provider") as MutableList<MutableMap<String, LinkedHashMap<String, String>>>,
            dataPath = this.dataFolder.absolutePath.toString()
        )
    }

    override fun onLoad() {
        if (!(File(this.dataFolder, "config.yml")).exists()) this.saveDefaultConfig()
        loadConfig()
        this.chrono = ZenChrono(config = this.zenConfig!!.chrono)
        this.chrono!!.dryRun()
        this.storage = ZenStorage(config = this.zenConfig!!.storage)
        this.storage!!.initialize()
        this.scheduler = ZenScheduler()
    }

    override fun onEnable() {
        zenListener = ZenListener(
            pluginInstance = this
        )
    }

    override fun onDisable() {
        this.zenListener = null
        this.storage = null
        this.chrono = null
        this.zenConfig = null
    }
}