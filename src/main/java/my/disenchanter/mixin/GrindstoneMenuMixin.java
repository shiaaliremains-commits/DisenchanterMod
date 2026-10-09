package my.disenchanter.mixin;

import my.disenchanter.util.DisenchantExtension;
import my.disenchanter.util.DisenchantResultSlot;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin implements DisenchantExtension {

    @Shadow @Final private Container repairSlots;
    @Shadow @Final private Container resultSlots;
    @Shadow @Final private ContainerLevelAccess access;

    @Unique private final Set<String> disenchanter$selected = new HashSet<>();
    @Unique private boolean disenchanter$isExtracting = false;

    @Override
    public Set<String> disenchanter$getSelected() {
        return this.disenchanter$selected;
    }

    @Override
    public void disenchanter$toggle(String enchantId) {
        if (this.disenchanter$selected.contains(enchantId)) {
            this.disenchanter$selected.remove(enchantId);
        } else {
            this.disenchanter$selected.add(enchantId);
        }
        ((AbstractContainerMenu) (Object) this).slotsChanged(this.repairSlots);
    }

    @Override
    public boolean disenchanter$isExtracting() {
        return this.disenchanter$isExtracting;
    }

    @Unique
    private static ItemEnchantments disenchanter$getEnchants(ItemStack stack) {
        if (stack.isEmpty()) return ItemEnchantments.EMPTY;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) return stored;
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void setupSlots(int i, Inventory inventory, ContainerLevelAccess containerLevelAccess, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // تعديل الخانة العلوية والسفلية لتقبل الكتب مع الحفاظ على فهرس الشفت كليك
        Slot slot0 = menu.slots.get(0);
        Slot customSlot0 = new Slot(this.repairSlots, 0, slot0.x, slot0.y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        };
        customSlot0.index = 0;
        menu.slots.set(0, customSlot0);

        Slot slot1 = menu.slots.get(1);
        Slot customSlot1 = new Slot(this.repairSlots, 1, slot1.x, slot1.y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        };
        customSlot1.index = 1;
        menu.slots.set(1, customSlot1);

        if (menu.slots.size() > 2) {
            Slot orig = menu.slots.get(2);
            DisenchantResultSlot customSlot2 = new DisenchantResultSlot((GrindstoneMenu) (Object) this, orig, this.repairSlots, this.resultSlots, this.access);
            customSlot2.index = 2;
            menu.slots.set(2, customSlot2);
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void createDisenchantResult(CallbackInfo ci) {
        ItemStack top = this.repairSlots.getItem(0);
        ItemStack bottom = this.repairSlots.getItem(1);

        if (!top.isEmpty() && !bottom.isEmpty() && (bottom.is(Items.BOOK) || bottom.is(Items.ENCHANTED_BOOK))) {
            ItemEnchantments enchants = disenchanter$getEnchants(top);

            if (!enchants.isEmpty()) {
                this.disenchanter$isExtracting = true;

                if (this.disenchanter$selected.isEmpty()) {
                    for (Holder<Enchantment> holder : enchants.keySet()) {
                        this.disenchanter$selected.add(holder.getRegisteredName());
                        break;
                    }
                }

                ItemStack resultBook = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable bookEnchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

                for (Holder<Enchantment> holder : enchants.keySet()) {
                    if (this.disenchanter$selected.contains(holder.getRegisteredName())) {
                        bookEnchants.set(holder, enchants.getLevel(holder));
                    }
                }

                if (!bookEnchants.toImmutable().isEmpty()) {
                    EnchantmentHelper.setEnchantments(resultBook, bookEnchants.toImmutable());
                    this.resultSlots.setItem(0, resultBook);
                    ((AbstractContainerMenu) (Object) this).broadcastChanges();
                    ci.cancel();
                    return;
                }
            }
        }
        this.disenchanter$isExtracting = false;
    }
}
