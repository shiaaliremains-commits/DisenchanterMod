package my.disenchanter

import my.disenchanter.block.DisenchanterBlocks
import my.disenchanter.menu.DisenchanterMenu
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object Disenchanter : ModInitializer {
    const val MOD_ID = "disenchanter"

    override fun onInitialize() {
        DisenchanterBlocks.register()

        PayloadTypeRegistry.serverboundPlay().register(SelectEnchantPayload.TYPE, SelectEnchantPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(SelectEnchantPayload.TYPE) { payload, context ->
            val player = context.player()
            val menu = player.containerMenu
            if (menu is DisenchanterMenu) {
                menu.toggleEnchant(payload.enchantId)
            }
        }
    }
}
