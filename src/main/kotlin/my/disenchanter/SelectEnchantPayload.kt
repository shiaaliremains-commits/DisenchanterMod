package my.disenchanter

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class SelectEnchantPayload(val enchantId: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<SelectEnchantPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Disenchanter.MOD_ID, "select_enchant"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SelectEnchantPayload> =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, { p: SelectEnchantPayload -> p.enchantId },
                { id -> SelectEnchantPayload(id) }
            )
    }
}
