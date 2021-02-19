package net.nachtbeere.minecraft.zen.logic

import net.nachtbeere.minecraft.zen.RewardType
import net.nachtbeere.minecraft.zen.ZenResult
import net.nachtbeere.minecraft.zen.ZenResultCode
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class ZenRewardFactory() {
    fun create(voteRewardType: RewardType, voteRewardCode: String, voteRewardAmount: Int, voteRewardName: String,
               isVoteRewardHasEnchant: Boolean, availableVoteRewardEnchant: String): ZenRewardHandler {
        return when(voteRewardType) {
            RewardType.MINECRAFT -> MinecraftItemRewardHandler(voteRewardCode, voteRewardAmount, voteRewardName,
                isVoteRewardHasEnchant, availableVoteRewardEnchant)
            else -> throw NullPointerException()
        }

    }
}

abstract class ZenRewardHandler() {
    fun infoLog(msg: String) {
        Bukkit.getLogger().info(msg)
    }

    fun warnLog(msg: String) {
        Bukkit.getLogger().warning(msg)
    }

    abstract fun sendReward(player: Player): ZenResult
}

class MinecraftItemRewardHandler(private val itemCode: String, private val amount: Int, private val customName: String,
                                 private val isHasEnchant: Boolean, private val availableEnchant: String)
    : ZenRewardHandler() {
    val rewardItem: ItemStack = makeItem()

    private fun makeItem(): ItemStack {
        val target = Material.valueOf(itemCode.toUpperCase())
        val item = ItemStack(target, amount)
        if (customName != "") {
            val itemMeta = item.itemMeta
            itemMeta.setDisplayName(customName)
            item.itemMeta = itemMeta
        }
        if (isHasEnchant) {
            val enchant = Enchantment.getByName(availableEnchant.toUpperCase())!!
            item.addUnsafeEnchantment(enchant, 1)
        }
        return item
    }

    private fun addItemToPlayer(player: Player): ZenResult {
        player.inventory.addItem(rewardItem)
        player.updateInventory()
        return ZenResult(ZenResultCode.VOTE_REWARD_DELIVERED, amount)
    }

    override fun sendReward(player: Player): ZenResult {
        return if (player.inventory.firstEmpty() > -1) {
            addItemToPlayer(player)
        } else {
            val existItem = player.inventory.contents.filter{ it?.itemMeta == rewardItem.itemMeta }
            if (existItem.size > 0) {
                existItem.forEach {
                    if (it.amount + rewardItem.amount <= 64) {
                        return addItemToPlayer(player)
                    }
                }
            }
            ZenResult(ZenResultCode.VOTE_REWARD_BUFFERED, -1)
        }
    }
}