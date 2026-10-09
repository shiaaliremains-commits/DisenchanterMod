package my.disenchanter.block

import my.disenchanter.Disenchanter
import my.disenchanter.menu.DisenchanterMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
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

    val TABLE_BLOCK = DisenchanterTableBlock(
        BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)
            .strength(4.0f, 1200.0f)
            .sound(SoundType.DEEPSLATE)
    )

    val TABLE_ITEM = BlockItem(TABLE_BLOCK, Item.Properties())

    val MENU_TYPE: MenuType<DisenchanterMenu> = MenuType(
        { id, inv -> DisenchanterMenu(id, inv) },
        FeatureFlags.DEFAULT_FLAGS
    )

    fun register() {
        Registry.register(BuiltInRegistries.BLOCK, ID, TABLE_BLOCK)
        Registry.register(BuiltInRegistries.ITEM, ID, TABLE_ITEM)
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
