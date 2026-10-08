package my.disenchanter

import my.disenchanter.util.DisenchantExtension
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.world.inventory.GrindstoneMenu

object Disenchanter : ModInitializer {
    const val MOD_ID = "disenchanter"

    override fun onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(SelectEnchantPayload.TYPE, SelectEnchantPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(SelectEnchantPayload.TYPE) { payload, context ->
            val player = context.player()
            val menu = player.containerMenu
            if (menu is GrindstoneMenu && menu is DisenchantExtension) {
                menu.`disenchanter$toggle`(payload.enchantId)
            }
        }
    }
}
