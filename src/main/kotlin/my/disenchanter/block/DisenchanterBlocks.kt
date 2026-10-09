package my.disenchanter.block

import my.disenchanter.Disenchanter
import my.disenchanter.menu.DisenchanterMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

object DisenchanterBlocks {
    val ID = Identifier.fromNamespaceAndPath(Disenchanter.MOD_ID, "disenchanter_table")

    // إنشاء مفاتيح التعريف الإجبارية لإصدار 26.3
    val BLOCK_KEY: ResourceKey<Block> = ResourceKey.create(Registries.BLOCK, ID)
    val ITEM_KEY: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, ID)

    val TABLE_BLOCK = DisenchanterTableBlock(
        BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)
            .setId(BLOCK_KEY) // شرط ماينكرافت 26.3 الإجباري
            .strength(4.0f, 1200.0f)
            .sound(SoundType.DEEPSLATE)
    )

    val TABLE_ITEM = BlockItem(
        TABLE_BLOCK,
        Item.Properties().setId(ITEM_KEY)
    )

    val MENU_TYPE: MenuType<DisenchanterMenu> = MenuType(
        { id, inv -> DisenchanterMenu(id, inv) },
        FeatureFlags.DEFAULT_FLAGS
    )

    fun register() {
        Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY, TABLE_BLOCK)
        Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, TABLE_ITEM)
        Registry.register(BuiltInRegistries.MENU, ID, MENU_TYPE)
    }
}

class DisenchanterTableBlock(props: Properties) : Block(props) {
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            player.openMenu(state.getMenuProvider(level, pos))
        }
        return InteractionResult.SUCCESS
    }

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): MenuProvider {
        return SimpleMenuProvider(
            { id: Int, inv: Inventory, _: Player ->
                DisenchanterMenu(id, inv, ContainerLevelAccess.create(level, pos))
            },
            Component.translatable("container.disenchanter.disenchanter_table")
        )
    }
}
