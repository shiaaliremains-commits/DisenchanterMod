package my.disenchanter.client

import my.disenchanter.SelectEnchantPayload
import my.disenchanter.util.DisenchantExtension
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

object DisenchanterClient : ClientModInitializer {
    private var lastTopItem: ItemStack = ItemStack.EMPTY
    private val buttons = ArrayList<Button>()
    private var activeScreen: GrindstoneScreen? = null

    override fun onInitializeClient() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is GrindstoneScreen) {
                activeScreen = screen
                lastTopItem = ItemStack.EMPTY
                buttons.clear()

                ScreenEvents.afterTick(screen).register { _ ->
                    val top = screen.menu.getSlot(0).item
                    if (!ItemStack.matches(top, lastTopItem)) {
                        lastTopItem = top.copy()
                        refreshButtons(screen)
                    }
                }

                ScreenEvents.remove(screen).register { _ ->
                    activeScreen = null
                    buttons.clear()
                }
            }
        }
    }

    private fun refreshButtons(screen: GrindstoneScreen) {
        val screenButtons = Screens.getWidgets(screen)
        for (btn in buttons) {
            screenButtons.remove(btn)
        }
        buttons.clear()

        val top = screen.menu.getSlot(0).item
        if (top.isEmpty) return

        val enchants = EnchantmentHelper.getEnchantmentsForCrafting(top)
        if (enchants.isEmpty) return

        val ext = screen.menu as? DisenchantExtension ?: return
        val x = (screen.width - 176) / 2 + 180
        val y = (screen.height - 166) / 2 + 8
        var i = 0

        for (holder in enchants.keySet()) {
            val id = holder.registeredName
            val level = enchants.getLevel(holder)
            val selected = ext.`disenchanter$getSelected`().contains(id)

            val text = Component.literal(if (selected) "[✔] " else "[  ]")
                .withStyle(if (selected) ChatFormatting.GREEN else ChatFormatting.GRAY)
                .append(Enchantment.getFullname(holder, level))

            val btn = Button.builder(text) { _ ->
                ext.`disenchanter$toggle`(id)
                ClientPlayNetworking.send(SelectEnchantPayload(id))
                refreshButtons(screen)
            }.bounds(x, y + (i * 20), 120, 18).build()

            buttons.add(btn)
            screenButtons.add(btn)
            i++
        }
    }
}
