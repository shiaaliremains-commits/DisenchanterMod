package my.disenchanter.client

import my.disenchanter.SelectEnchantPayload
import my.disenchanter.client.mixin.ScreenInvoker
import my.disenchanter.util.DisenchantExtension
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.ItemEnchantments

object DisenchanterClient : ClientModInitializer {

    override fun onInitializeClient() {
        // الربط المباشر مع واجهة حجر الجلخ فور فتحها
        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            if (screen is GrindstoneScreen) {
                var lastTop: ItemStack = ItemStack.EMPTY
                var lastBottom: ItemStack = ItemStack.EMPTY
                val buttons = ArrayList<Button>()

                ScreenEvents.afterTick(screen).register { s ->
                    val gScreen = s as GrindstoneScreen
                    val top = gScreen.menu.getSlot(0).item
                    val bottom = gScreen.menu.getSlot(1).item

                    // تحديث الأزرار فور تغيير أو وضع أي غرض بالخانات
                    if (!ItemStack.matches(top, lastTop) || !ItemStack.matches(bottom, lastBottom)) {
                        lastTop = top.copy()
                        lastBottom = bottom.copy()

                        val invoker = gScreen as? ScreenInvoker
                        if (invoker != null) {
                            for (b in buttons) invoker.invokeRemoveWidget(b)
                        }
                        buttons.clear()

                        val enchants = getEnchantments(top)
                        if (enchants.isEmpty) return@register

                        val ext = gScreen.menu as? DisenchantExtension ?: return@register

                        // إحداثيات مدروسة للهاتف تضمن ظهور الأزرار دائماً
                        val leftPos = (gScreen.width - 176) / 2
                        val topPos = (gScreen.height - 166) / 2
                        val spaceRight = gScreen.width - (leftPos + 176)
                        val btnW = 110
                        val x = if (spaceRight >= btnW + 6) leftPos + 180 else maxOf(4, leftPos - btnW - 4)
                        val y = topPos + 8
                        var i = 0

                        for (holder in enchants.keySet()) {
                            // إذا الخانة السفلية أداة/سيف، نعرض فقط التطويرات المسموحة له
                            if (!bottom.isEmpty && !bottom.`is`(Items.BOOK) && !bottom.`is`(Items.ENCHANTED_BOOK)) {
                                if (!holder.value().canEnchant(bottom)) continue
                            }

                            val id = runCatching { holder.registeredName }.getOrNull() ?: holder.toString()
                            val level = enchants.getLevel(holder)
                            val selected = ext.`disenchanter$getSelected`().contains(id)

                            val text = Component.literal(if (selected) "[✔] " else "[  ]")
                                .withStyle(if (selected) ChatFormatting.GREEN else ChatFormatting.GRAY)
                                .append(Enchantment.getFullname(holder, level))

                            val btn = Button.builder(text) { _ ->
                                ext.`disenchanter$toggle`(id)
                                ClientPlayNetworking.send(SelectEnchantPayload(id))
                                lastTop = ItemStack.EMPTY // تحديث فوري
                            }.bounds(x, y + (i * 20), btnW, 18).build()

                            buttons.add(btn)
                            invoker?.invokeAddRenderableWidget(btn)
                            i++
                        }
                    }
                }
            }
        }
    }

    private fun getEnchantments(stack: ItemStack): ItemEnchantments {
        if (stack.isEmpty) return ItemEnchantments.EMPTY
        val stored = stack.get(DataComponents.STORED_ENCHANTMENTS)
        if (stored != null && !stored.isEmpty) return stored
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
    }
}
