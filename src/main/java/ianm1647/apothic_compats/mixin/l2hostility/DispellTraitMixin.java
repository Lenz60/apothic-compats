package ianm1647.apothic_compats.mixin.l2hostility;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import ianm1647.apothic_compats.gem.l2hostility.HostilityGemBonuses;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.DispellTrait", remap = false)
public class DispellTraitMixin {
    @ModifyVariable(method = "postHurtImpl", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), ordinal = 0, remap = false)
    private List<ItemStack> filterResistantGems(List<ItemStack> stacks, int level, LivingEntity attacker, LivingEntity target) {
        List<ItemStack> filtered = new ArrayList<>(stacks);
        filtered.removeIf(stack -> {
            LootCategory category = LootCategory.forItem(stack);
            return SocketHelper.getGems(stack).stream().anyMatch(instance -> instance.gem().isBound()
                    && instance.rarity().isBound()
                    && instance.gem().get().getBonus(category, instance.rarity().get())
                    .filter(HostilityGemBonuses.DispellResist.class::isInstance).isPresent());
        });
        return filtered;
    }
}