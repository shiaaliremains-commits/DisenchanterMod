package my.disenchanter.client.mixin;

import my.disenchanter.SelectEnchantPayload;
import my.disenchanter.util.DisenchantExtension;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
    public GrindstoneScreenMixin(GrindstoneMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Unique private final List<Button> disenchanter$buttons = new ArrayList<>();
    @Unique private ItemStack disenchanter$lastTop = ItemStack.EMPTY;

    // الحقن في دالة tick الرسمية المتوافقة 100% مع إصدار 26.3
    @Inject(method = "tick", at = @At("TAIL"))
    private void updateEnchantButtons(CallbackInfo ci) {
        ItemStack top = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(top, this.disenchanter$lastTop)) {
            this.disenchanter$lastTop = top.copy();
            this.disenchanter$refresh();
        }
    }

    @Unique
    private void disenchanter$refresh() {
        for (Button btn : this.disenchanter$buttons) {
            this.removeWidget(btn);
        }
        this.disenchanter$buttons.clear();

        ItemStack top = this.menu.getSlot(0).getItem();
        if (top.isEmpty()) return;

        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(top);
        if (enchants.isEmpty()) return;

        DisenchantExtension ext = (DisenchantExtension) this.menu;
        int x = this.leftPos + this.imageWidth + 4;
        int y = this.topPos + 8;
        int i = 0;

        for (Holder<Enchantment> holder : enchants.keySet()) {
            String id = holder.getRegisteredName();
            int level = enchants.getLevel(holder);
            boolean selected = ext.disenchanter$getSelected().contains(id);

            Component text = Component.literal(selected ? "[✔] " : "[  ]")
                    .withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(Enchantment.getFullname(holder, level));

            Button btn = Button.builder(text, b -> {
                ext.disenchanter$toggle(id);
                ClientPlayNetworking.send(new SelectEnchantPayload(id));
                this.disenchanter$refresh();
            }).bounds(x, y + (i * 20), 120, 18).build();

            this.disenchanter$buttons.add(btn);
            this.addRenderableWidget(btn);
            i++;
        }
    }
}
