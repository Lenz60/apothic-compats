package ianm1647.apothic_compats.mixin;

import dev.shadowsoffire.apotheosis.adventure.AdventureEvents;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import ianm1647.apothic_compats.Comp;
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
}
