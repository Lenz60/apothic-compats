package ianm1647.apothic_compats.event;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import ianm1647.apothic_compats.ApothicCompats;
import ianm1647.apothic_compats.Comp;
import ianm1647.apothic_compats.affix.aether.MagicalDartAffix;
import ianm1647.apothic_compats.affix.create.MagicalPotatoAffix;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public class AffixEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttack(LivingAttackEvent e) {
        if (ModList.get().isLoaded("aether")) {
            ApothicCompats.convertDamageToMagic(e, MagicalDartAffix.class);
        }
        if (ModList.get().isLoaded("create")) {
            ApothicCompats.convertDamageToMagic(e, MagicalPotatoAffix.class);
        }
    }

    /**
     * Copies affix data from the shooter's weapon onto non-arrow projectiles (potato cannon shots, bullets),
     * mirroring Apotheosis' AdventureEvents#fireArrow handling for arrows (which we widen via mixin
     * to also accept our custom loot categories).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onProjectileJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        if (!(e.getEntity() instanceof Projectile proj) || proj instanceof AbstractArrow) return;
        if (proj.getPersistentData().contains(AffixHelper.AFFIX_DATA)) return;
        if (!(proj.getOwner() instanceof LivingEntity user)) return;
        ItemStack weapon = user.getMainHandItem();
        if (weapon.isEmpty()) return;
        LootCategory cat = LootCategory.forItem(weapon);
        if (cat == Comp.LootCategories.GUN || cat == Comp.LootCategories.POTATO_CANNON) {
            AffixHelper.copyFrom(weapon, proj);
        }
    }
}
