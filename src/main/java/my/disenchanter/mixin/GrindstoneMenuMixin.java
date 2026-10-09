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
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu implements DisenchantExtension {

    @Shadow @Final private Container repairSlots;
    @Shadow @Final private Container resultSlots;
    @Shadow @Final private ContainerLevelAccess access;

    protected GrindstoneMenuMixin() {
        super(null, 0);
    }

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
        this.slotsChanged(this.repairSlots);
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
    private void wrapResultSlot(int i, Inventory inventory, ContainerLevelAccess containerLevelAccess, CallbackInfo ci) {
        if (this.slots.size() > 2) {
            Slot orig = this.slots.get(2);
            this.slots.set(2, new DisenchantResultSlot((GrindstoneMenu) (Object) this, orig, this.repairSlots, this.resultSlots, this.access));
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void createDisenchantResult(CallbackInfo ci) {
        ItemStack top = this.repairSlots.getItem(0);
        ItemStack bottom = this.repairSlots.getItem(1);

        if (!top.isEmpty() && !bottom.isEmpty()) {
            ItemEnchantments enchants = disenchanter$getEnchants(top);

            if (!enchants.isEmpty()) {
                this.disenchanter$isExtracting = true;

                if (this.disenchanter$selected.isEmpty()) {
                    for (Holder<Enchantment> holder : enchants.keySet()) {
                        this.disenchanter$selected.add(holder.getRegisteredName());
                        break;
                    }
                }

                // الحالة 1: الغرض السفلي كتاب (عادي أو مسحور) -> استخراج التطوير لكتاب
                if (bottom.is(Items.BOOK) || bottom.is(Items.ENCHANTED_BOOK)) {
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
                        this.broadcastChanges();
                        ci.cancel();
                        return;
                    }
                }
                // الحالة 2: الغرض العلوي كتاب مسحور، والغرض السفلي أداة/درع -> نقل التطوير للسلاح
                else if (top.is(Items.ENCHANTED_BOOK)) {
                    ItemStack resultItem = bottom.copy();
                    resultItem.setCount(1);
                    ItemEnchantments bottomEnchants = disenchanter$getEnchants(bottom);
                    ItemEnchantments.Mutable newEnchants = new ItemEnchantments.Mutable(bottomEnchants);

                    for (Holder<Enchantment> holder : enchants.keySet()) {
                        if (this.disenchanter$selected.contains(holder.getRegisteredName())) {
                            newEnchants.set(holder, enchants.getLevel(holder));
                        }
                    }

                    EnchantmentHelper.setEnchantments(resultItem, newEnchants.toImmutable());
                    this.resultSlots.setItem(0, resultItem);
                    this.broadcastChanges();
                    ci.cancel();
                    return;
                }
            }
        }
        this.disenchanter$isExtracting = false;
    }
}
