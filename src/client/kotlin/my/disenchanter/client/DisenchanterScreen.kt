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
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment

class DisenchanterScreen(
    menu: DisenchanterMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<DisenchanterMenu>(menu, inventory, title) {

    private val CONTAINER_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/grindstone.png")
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
        val y = topPos + 8
        var i = 0

        for (h in enchants.keySet()) {
            val id = h.registeredName
            val lvl = enchants.getLevel(h)
            val selected = menu.selectedEnchants.contains(id)

            val text = Component.literal(if (selected) "[✔] " else "[  ]")
                .withStyle(if (selected) ChatFormatting.GREEN else ChatFormatting.GRAY)
                .append(Enchantment.getFullname(h, lvl))

            val btn = Button.builder(text) { _ ->
                menu.toggleEnchant(id)
                ClientPlayNetworking.send(SelectEnchantPayload(id))
                lastTop = ItemStack.EMPTY
            }.bounds(x, y + (i * 20), 120, 18).build()

            buttons.add(btn)
            addRenderableWidget(btn)
            i++
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // رسم واجهة ماينكرافت الرمادية الأصلية بالصيغة الرسمية الدقيقة لإصدار 26.3
        val u1 = imageWidth / 256.0f
        val v1 = imageHeight / 256.0f
        graphics.blit(
            CONTAINER_TEXTURE,
            leftPos, topPos,
            leftPos + imageWidth, topPos + imageHeight,
            0.0f, u1,
            0.0f, v1
        )

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
}
