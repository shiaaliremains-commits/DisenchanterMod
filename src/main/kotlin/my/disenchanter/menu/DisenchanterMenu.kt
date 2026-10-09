package my.disenchanter.menu

import my.disenchanter.block.DisenchanterBlocks
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments

class DisenchanterMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val access: ContainerLevelAccess = ContainerLevelAccess.NULL
) : AbstractContainerMenu(DisenchanterBlocks.MENU_TYPE, containerId) {

    val inputContainer: Container = object : SimpleContainer(2) {
        override fun setChanged() {
            super.setChanged()
            slotsChanged(this)
        }
    }

    val resultContainer = ResultContainer()
    val selectedEnchants = HashSet<String>()

    init {
        // الخانة 0: الأداة العلوية
        addSlot(Slot(inputContainer, 0, 32, 22))

        // الخانة 1: الكتاب أو المستقبل السفلي
        addSlot(Slot(inputContainer, 1, 32, 54))

        // الخانة 2: خانة الاستلام المخصصة
        addSlot(object : Slot(resultContainer, 0, 102, 38) {
            override fun mayPlace(stack: ItemStack) = false

            override fun onTake(player: Player, stack: ItemStack) {
                val top = inputContainer.getItem(0)
                val bottom = inputContainer.getItem(1)

                if (!bottom.isEmpty) {
                    bottom.shrink(1)
                    inputContainer.setItem(1, if (bottom.isEmpty) ItemStack.EMPTY else bottom)
                }

                val enchants = getEnchants(top)
                val remaining = ItemEnchantments.Mutable(enchants)

                for (holder in enchants.keySet()) {
                    val id = holder.registeredName
                    if (selectedEnchants.contains(id)) {
                        remaining.set(holder, 0)
                    }
                }

                val updatedTop = top.copy()
                val rem = remaining.toImmutable()
                if (rem.isEmpty && updatedTop.`is`(Items.ENCHANTED_BOOK)) {
                    inputContainer.setItem(0, ItemStack(Items.BOOK))
                } else {
                    setEnchants(updatedTop, rem)
                    inputContainer.setItem(0, updatedTop)
                }

                access.execute { level, pos ->
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f)
                }
            }
        })

        // خانات حقيبة اللاعب
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 88 + row * 18))
            }
        }
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 146))
        }
    }

    fun toggleEnchant(id: String) {
        if (selectedEnchants.contains(id)) selectedEnchants.remove(id)
        else selectedEnchants.add(id)
        createResult()
    }

    fun getEnchants(stack: ItemStack): ItemEnchantments {
        if (stack.isEmpty) return ItemEnchantments.EMPTY
        val stored = stack.get(DataComponents.STORED_ENCHANTMENTS)
        if (stored != null && !stored.isEmpty) return stored
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
    }

    private fun setEnchants(stack: ItemStack, enchants: ItemEnchantments) {
        if (stack.`is`(Items.ENCHANTED_BOOK)) {
            stack.set(DataComponents.STORED_ENCHANTMENTS, enchants)
        } else {
            EnchantmentHelper.setEnchantments(stack, enchants)
        }
    }

    override fun slotsChanged(container: Container) {
        super.slotsChanged(container)
        if (container == inputContainer) {
            createResult()
        }
    }

    fun createResult() {
        val top = inputContainer.getItem(0)
        val bottom = inputContainer.getItem(1)

        if (top.isEmpty || bottom.isEmpty || (!bottom.`is`(Items.BOOK) && !bottom.`is`(Items.ENCHANTED_BOOK))) {
            resultContainer.setItem(0, ItemStack.EMPTY)
            broadcastChanges()
            return
        }

        val enchants = getEnchants(top)
        if (enchants.isEmpty) {
            resultContainer.setItem(0, ItemStack.EMPTY)
            broadcastChanges()
            return
        }

        if (selectedEnchants.isEmpty()) {
            for (h in enchants.keySet()) {
                selectedEnchants.add(h.registeredName)
                break
            }
        }

        val resultBook = ItemStack(Items.ENCHANTED_BOOK)
        val bottomEnchants = getEnchants(bottom)
        val bookEnchants = ItemEnchantments.Mutable(bottomEnchants)

        for (h in enchants.keySet()) {
            if (selectedEnchants.contains(h.registeredName)) {
                val topLvl = enchants.getLevel(h)
                val botLvl = bookEnchants.getLevel(h)
                val finalLvl = if (topLvl == botLvl && topLvl < h.value().maxLevel) topLvl + 1 else maxOf(topLvl, botLvl)
                bookEnchants.set(h, finalLvl)
            }
        }

        val built = bookEnchants.toImmutable()
        if (!built.isEmpty) {
            setEnchants(resultBook, built)
            resultContainer.setItem(0, resultBook)
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY)
        }
        broadcastChanges()
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val itemStack2 = slot.item
            itemStack = itemStack2.copy()
            if (index == 2) {
                if (!moveItemStackTo(itemStack2, 3, 39, true)) return ItemStack.EMPTY
                slot.onQuickCraft(itemStack2, itemStack)
            } else if (index != 0 && index != 1) {
                if (!moveItemStackTo(itemStack2, 0, 2, false)) return ItemStack.EMPTY
            } else if (!moveItemStackTo(itemStack2, 3, 39, false)) {
                return ItemStack.EMPTY
            }

            if (itemStack2.isEmpty) slot.setByPlayer(ItemStack.EMPTY)
            else slot.setChanged()

            if (itemStack2.count == itemStack.count) return ItemStack.EMPTY
            slot.onTake(player, itemStack2)
        }
        return itemStack
    }

    override fun removed(player: Player) {
        super.removed(player)
        access.execute { _, _ -> clearContainer(player, inputContainer) }
    }

    override fun stillValid(player: Player): Boolean {
        return stillValid(access, player, DisenchanterBlocks.TABLE_BLOCK)
    }
}
