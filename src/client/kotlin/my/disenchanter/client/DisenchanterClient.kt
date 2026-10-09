package my.disenchanter.client

import my.disenchanter.SelectEnchantPayload
import my.disenchanter.client.mixin.ScreenInvoker
import my.disenchanter.util.DisenchantExtension
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
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
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val screen = getCurrentScreen(client)
            if (screen is GrindstoneScreen) {
                if (activeScreen != screen) {
                    activeScreen = screen
                    lastTopItem = ItemStack.EMPTY
                    buttons.clear()
                }
                val top = screen.menu.getSlot(0).item
                if (!ItemStack.matches(top, lastTopItem)) {
                    lastTopItem = top.copy()
                    refreshButtons(screen)
                }
            } else {
                activeScreen = null
                buttons.clear()
            }
        }
    }

    private fun getCurrentScreen(mc: Minecraft): Screen? {
        val method = mc.javaClass.methods.firstOrNull {
            (it.name == "screen" || it.name == "getScreen") && it.parameterCount == 0 && Screen::class.java.isAssignableFrom(it.returnType)
        }
        if (method != null) {
            return runCatching { method.invoke(mc) as? Screen }.getOrNull()
        }
        val field = mc.javaClass.fields.firstOrNull {
            Screen::class.java.isAssignableFrom(it.type)
        } ?: mc.javaClass.declaredFields.firstOrNull {
            Screen::class.java.isAssignableFrom(it.type)
        }
        field?.isAccessible = true
        return runCatching { field?.get(mc) as? Screen }.getOrNull()
    }

    private fun refreshButtons(screen: GrindstoneScreen) {
        val invoker = screen as? ScreenInvoker ?: return
        for (btn in buttons) {
            invoker.invokeRemoveWidget(btn)
        }
        buttons.clear()

        val top = screen.menu.getSlot(0).item
        if (top.isEmpty) return

        val enchants = EnchantmentHelper.getEnchantmentsForCrafting(top)
        if (enchants.isEmpty()) return

        val ext = screen.menu as? DisenchantExtension ?: return
        val x = (screen.width - 176) / 2 + 180
        val y = (screen.height - 166) / 2 + 8
        var i = 0

        for (holder in enchants.keySet()) {
            val id = runCatching { holder.registeredName }.getOrNull() ?: holder.toString()
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
            invoker.invokeAddRenderableWidget(btn)
            i++
        }
    }
}
