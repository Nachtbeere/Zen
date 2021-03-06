package net.nachtbeere.minecraft.zen.logic

import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.InheritanceNode
import net.nachtbeere.minecraft.zen.PermissionMethod
import org.bukkit.Bukkit
import org.bukkit.plugin.RegisteredServiceProvider
import java.util.*
import java.util.stream.Collectors

class ZenPermissionFactory() {
    fun create(permissionMethod: String, groups: List<String>): ZenPermissionHandler {
        return when (permissionMethod.toUpperCase()) {
            PermissionMethod.LUCKPERM.name -> LuckpermHandler(groups[0], groups)
            else -> throw NullPointerException()
        }
    }
}

abstract class ZenPermissionHandler() {
    fun infoLog(msg: String) {
        Bukkit.getLogger().info(msg)
    }

    fun warnLog(msg: String) {
        Bukkit.getLogger().warning(msg)
    }

    abstract fun getPermissionGroup(uuid: UUID): String
    abstract fun setPermissionGroup(uuid: UUID, group: String)
    abstract fun getUUIDByUsername(username: String): UUID?
    abstract fun getPermissionGroupByUsername(username: String): String?
}

class LuckpermHandler(private val defaultGroup: String, private val indexedGroup: List<String>): ZenPermissionHandler() {
    private var provider = getProvider()
    private var api: LuckPerms? = null

    init {
        if (provider == null) {
            warnLog("Can't found Permission method!")
        } else {
            api = provider!!.provider
        }
    }

    @JvmName("zenGetPermissionProvider")
    private fun getProvider(): RegisteredServiceProvider<LuckPerms>? {
        return Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
    }

    override fun getPermissionGroup(uuid: UUID): String {
        val groups = getPermissionGroupSet(getUser(uuid))
        return groups.last()
    }

    override fun setPermissionGroup(uuid: UUID, group: String) {
        infoLog("set permission group to $group")
        purgeNonDefaultPermissionGroup(uuid)
        if (group != defaultGroup) {
            api!!.userManager.modifyUser(uuid) {
                println("set group $group")
                val result = it.data().add(InheritanceNode.builder(group).build())
                println("set result: $result")
            }
        }
    }

    override fun getUUIDByUsername(username: String): UUID? {
        return getUserByUsername(username)?.uniqueId
    }

    override fun getPermissionGroupByUsername(username: String): String? {
        val user = getUserByUsername(username)
        return if (user != null) {
            val groups = getPermissionGroupSet(user)
            groups.last()
        } else {
            null
        }
    }

    private fun purgeNonDefaultPermissionGroup(uuid: UUID) {
        api!!.userManager.modifyUser(uuid) {
            val groups = getPermissionGroupSet(it)
            for (group in groups) {
                if (group != defaultGroup) {
                    println("purge group: $group")
                    val result = it.data().remove(InheritanceNode.builder(group).build())
                    println("purge result: $result")
                }
            }
        }
    }

    private fun getUser(uuid: UUID): User {
        return api!!.userManager.getUser(uuid)!!
    }

    private fun getUserByUsername(username: String): User? {
        return api!!.userManager.getUser(username)
    }

    private fun getPermissionGroupSet(user: User): List<String> {
        // java.util.stream.Stream has bug in docker java(Java 1.8 (OpenJDK 64-Bit Server VM 25.212-b04))
        // use for-loop.
        val groups = arrayListOf<String>()
        for (group in indexedGroup) {
            if (user.nodes.contains(InheritanceNode.builder(group).build())) {
                groups.add(group)
            }
        }
        infoLog("permission groups: $groups")
        return groups.sortedBy { indexedGroup.indexOf(it) }
    }
}