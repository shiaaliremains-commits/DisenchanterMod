package my.disenchanter.mixin;

import my.disenchanter.util.DisenchantExtension;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
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
        ((GrindstoneMenu) (Object) this).createResult();
    }

    @Override
    public boolean disenchanter$isExtracting() {
        return this.disenchanter$isExtracting;
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void createDisenchantResult(CallbackInfo ci) {
        ItemStack top = this.repairSlots.getItem(0);
        ItemStack bottom = this.repairSlots.getItem(1);

        // الشرط: الغرض العلوي مطور، والغرض السفلي كتاب عادي أو كتاب مسحور
        if (!top.isEmpty() && !bottom.isEmpty() && (bottom.is(Items.BOOK) || bottom.is(Items.ENCHANTED_BOOK))) {
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(top);
            if (!enchants.isEmpty()) {
                this.disenchanter$isExtracting = true;

                // إذا لم يتم تحديد أي شيء، نحدد أول تطويرة افتراضياً
                if (this.disenchanter$selected.isEmpty()) {
                    for (Holder<Enchantment> holder : enchants.keySet()) {
                        this.disenchanter$selected.add(holder.getRegisteredName());
                        break;
                    }
                }

                // بناء كتاب مسحور يحتوي على التطويرات المختارة فقط
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

    // استهلاك كتاب واحد وتفريغ التطويرات المختارة فقط من السيف بأمان
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void onTakeExtracted(Player player, ItemStack itemStack, CallbackInfo ci) {
        if (this.disenchanter$isExtracting) {
            ItemStack top = this.repairSlots.getItem(0);
            ItemStack bottom = this.repairSlots.getItem(1);

            // 1. استهلاك كتاب واحد فقط من الستاك
            bottom.shrink(1);
            this.repairSlots.setItem(1, bottom.isEmpty() ? ItemStack.EMPTY : bottom);

            // 2. إزالة التطويرات المختارة من السيف مع إبقاء الباقي
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(top);
            ItemEnchantments.Mutable remaining = new ItemEnchantments.Mutable(enchants);

            for (Holder<Enchantment> holder : enchants.keySet()) {
                if (this.disenchanter$selected.contains(holder.getRegisteredName())) {
                    remaining.set(holder, 0); // حذف التطويرة المنقولة
                }
            }

            ItemStack updatedTop = top.copy();
            if (remaining.toImmutable().isEmpty() && updatedTop.is(Items.ENCHANTED_BOOK)) {
                updatedTop = new ItemStack(Items.BOOK); // تحويل الكتاب المسحور إلى كتاب عادي إذا فرغ
            } else {
                EnchantmentHelper.setEnchantments(updatedTop, remaining.toImmutable());
            }

            this.repairSlots.setItem(0, updatedTop);

            // صوت النجاح
            this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 1.0f));

            ci.cancel();
        }
    }
}
