package ianm1647.apothic_compats.mixin.l2hostility;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import ianm1647.apothic_compats.gem.l2hostility.HostilityGemBonuses;
import dev.xkmc.l2hostility.compat.curios.EntitySlotAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.RagnarokTrait", remap = false)
public class RagnarokTraitMixin {
    @Inject(method = "allowSeal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void preventSealingResistantGem(EntitySlotAccess access, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = access.get();
        if (stack.isEmpty()) return;
        LootCategory category = LootCategory.forItem(stack);
        boolean resistant = SocketHelper.getGems(stack).stream().anyMatch(instance -> instance.gem().isBound()
                && instance.rarity().isBound()
                && instance.gem().get().getBonus(category, instance.rarity().get())
                .filter(HostilityGemBonuses.RagnarokResist.class::isInstance).isPresent());
        if (resistant) cir.setReturnValue(false);
    }
}