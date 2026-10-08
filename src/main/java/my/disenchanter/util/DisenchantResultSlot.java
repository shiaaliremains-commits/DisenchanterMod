package my.disenchanter.util;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class DisenchantResultSlot extends Slot {
    private final GrindstoneMenu menu;
    private final Slot originalSlot;
    private final Container repairSlots;
    private final ContainerLevelAccess access;

    public DisenchantResultSlot(GrindstoneMenu menu, Slot originalSlot, Container repairSlots, Container resultSlots, ContainerLevelAccess access) {
        super(resultSlots, 2, originalSlot.x, originalSlot.y);
        this.menu = menu;
        this.originalSlot = originalSlot;
        this.repairSlots = repairSlots;
        this.access = access;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack resultStack) {
        DisenchantExtension ext = (DisenchantExtension) this.menu;
        if (ext.disenchanter$isExtracting()) {
            ItemStack top = this.repairSlots.getItem(0);
            ItemStack bottom = this.repairSlots.getItem(1);

            // استهلاك كتاب واحد فقط من الستاك
            bottom.shrink(1);
            this.repairSlots.setItem(1, bottom.isEmpty() ? ItemStack.EMPTY : bottom);

            // إزالة التطويرات المختارة فقط من الأداة
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(top);
            ItemEnchantments.Mutable remaining = new ItemEnchantments.Mutable(enchants);

            for (Holder<Enchantment> holder : enchants.keySet()) {
                String id = holder.getRegisteredName();
                if (ext.disenchanter$getSelected().contains(id)) {
                    remaining.set(holder, 0);
                }
            }

            ItemStack updatedTop = top.copy();
            ItemEnchantments remainingEnchants = remaining.toImmutable();

            if (remainingEnchants.isEmpty() && updatedTop.is(Items.ENCHANTED_BOOK)) {
                updatedTop = new ItemStack(Items.BOOK);
            } else {
                EnchantmentHelper.setEnchantments(updatedTop, remainingEnchants);
            }

            this.repairSlots.setItem(0, updatedTop);

            this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 1.0f));
        } else {
            this.originalSlot.onTake(player, resultStack);
        }
    }
}
