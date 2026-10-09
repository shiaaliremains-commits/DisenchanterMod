package my.disenchanter.client

import my.disenchanter.block.DisenchanterBlocks
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screens.MenuScreens

object DisenchanterClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(DisenchanterBlocks.MENU_TYPE, ::DisenchanterScreen)
    }
}
