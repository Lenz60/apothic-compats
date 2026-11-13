package ianm1647.apothic_compats.mixin;

import dev.shadowsoffire.apotheosis.adventure.AdventureEvents;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import ianm1647.apothic_compats.Comp;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AdventureEvents.class, remap = false)
public class AdventureEventsMixin {

    @Redirect(method = "fireArrow", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;isRanged()Z", remap = false))
    public boolean fireProjectile(LootCategory instance) {
        if (instance == Comp.LootCategories.GUN && ModList.get().isLoaded("irons_artifice")) {
            return true;
        }
        if (instance == Comp.LootCategories.DART_SHOOTER) {
            return true;
        }
        return instance.isRanged();
    }

    /**
     * Guards Apotheosis's natural-spawn affix roll (AdventureEvents#special).
     * <p>
     * That handler does {@code LootCategory.forItem(affixItem).getSlots()[0]}, which crashes with an
     * ArrayIndexOutOfBoundsException when the randomly rolled affix loot entry belongs to a loot category
     * without equipment slots - i.e. any of this mod's curio categories (ring, necklace, charm, ...).
     * Vanilla 7.x never hits this because its affix_loot_entries pool contains only weapons/armor.
     * <p>
     * We redirect the {@link LootController#createRandomLootItem} call and return an empty stack when the
     * rolled item maps to a slotless category, so the handler takes its existing early-out instead.
     * Curio loot from chests/bosses and all other loot paths are unaffected.
     */
    @Redirect(method = "special", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/adventure/loot/LootController;createRandomLootItem(Lnet/minecraft/util/RandomSource;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ServerLevelAccessor;)Lnet/minecraft/world/item/ItemStack;", remap = false), remap = false)
    private ItemStack apothicCompats$sanitizeSpawnLoot(RandomSource rand, LootRarity rarity, Player player, ServerLevelAccessor level) {
        ItemStack stack = LootController.createRandomLootItem(rand, rarity, player, level);
        if (!stack.isEmpty() && LootCategory.forItem(stack).getSlots().length == 0) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}
