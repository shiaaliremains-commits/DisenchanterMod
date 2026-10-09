package my.disenchanter.client

import my.disenchanter.SelectEnchantPayload
import my.disenchanter.menu.DisenchanterMenu
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment

class DisenchanterScreen(
    menu: DisenchanterMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<DisenchanterMenu>(menu, inventory, title) {

    private val buttons = ArrayList<Button>()
    private var lastTop = ItemStack.EMPTY

    override fun containerTick() {
        super.containerTick()
        val top = menu.inputContainer.getItem(0)
        if (!ItemStack.matches(top, lastTop)) {
            lastTop = top.copy()
            refreshButtons()
        }
    }

    private fun refreshButtons() {
        for (b in buttons) removeWidget(b)
        buttons.clear()

        val top = menu.inputContainer.getItem(0)
        if (top.isEmpty) return

        val enchants = menu.getEnchants(top)
        if (enchants.isEmpty) return

        val x = leftPos + imageWidth + 6
        val y = topPos + 10
        var i = 0

        for (h in enchants.keySet()) {
            val id = h.registeredName
            val lvl = enchants.getLevel(h)
            val selected = menu.selectedEnchants.contains(id)

            val text = Component.literal(if (selected) "[✔] " else "[  ]")
                .withStyle(if (selected) ChatFormatting.GREEN else ChatFormatting.DARK_GRAY)
                .append(Enchantment.getFullname(h, lvl))

            val btn = Button.builder(text) { _ ->
                menu.toggleEnchant(id)
                ClientPlayNetworking.send(SelectEnchantPayload(id))
                lastTop = ItemStack.EMPTY
            }.bounds(x, y + (i * 20), 125, 18).build()

            buttons.add(btn)
            addRenderableWidget(btn)
            i++
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        val bg = 0xF20F1626.toInt()
        val border = 0xFF283A5E.toInt()
        val slotBorder = 0xFF3E5A8F.toInt()

        // رسم كارت الواجهة بالمدخلات الخمسة الصحيحة
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, bg)
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, border)
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, border)
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, border)
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, border)

        // عنوان الطاولة
        val title = Component.literal("✦ DISENCHANTER TABLE ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, 0xFFE24D.toInt())

        // مربعات الخانات
        drawSlot(graphics, leftPos + 31, topPos + 21, slotBorder)
        drawSlot(graphics, leftPos + 31, topPos + 53, slotBorder)
        drawSlot(graphics, leftPos + 101, topPos + 37, 0xFF00FF66.toInt())

        // سهم التدفق السحري
        graphics.centeredText(font, Component.literal("➔").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), leftPos + 72, topPos + 40, 0xFF00E5FF.toInt())
    }

    private fun drawSlot(g: GuiGraphicsExtractor, x: Int, y: Int, color: Int) {
        g.fill(x, y, x + 18, y + 18, 0xAA080C14.toInt())
        g.fill(x, y, x + 18, y + 1, color)
        g.fill(x, y + 17, x + 18, y + 18, color)
        g.fill(x, y, x + 1, y + 18, color)
        g.fill(x + 17, y, x + 18, y + 18, color)
    }
}
