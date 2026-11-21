package ianm1647.apothic_compats.event.irons_artifice;

import ianm1647.apothic_compats.affix.irons_artifice.BulletModifierAffix;
import ianm1647.apothic_compats.affix.irons_artifice.InfiniteAmmoAffix;
import ianm1647.apothic_compats.affix.irons_artifice.MagicalBulletAffix;
import ianm1647.apothic_compats.affix.irons_artifice.MultiShotAffix;
import ianm1647.apothic_compats.affix.irons_artifice.TelepathicGunAffix;
import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArtificeAffixEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void gunShootEvents(ComposeShotEvent e) {
        MultiShotAffix.multiplyBullets(e);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        TelepathicGunAffix.drops(e);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void preventAmmoConsumption(AmmoEvent.Consume e) {
        InfiniteAmmoAffix.preventAmmoConsumption(e);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void bulletImpact(BulletImpactEvent e) {
        BulletModifierAffix.onBulletImpact(e);

        Bullet bullet = e.getBullet();
        if (bullet == null || !(bullet.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }

        // Extract the entity the bullet hit (works at gun range, not melee reach)
        LivingEntity target = null;
        HitResult hitResult = e.getRayTraceResult();
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult ehr) {
            if (ehr.getEntity() instanceof LivingEntity livingTarget) {
                target = livingTarget;
            }
        }

        var affixes = ianm1647.apothic_compats.ApothicCompats.streamEntityAffixes(bullet).toList();
        for (var inst : affixes) {
            if (inst.affix().get() instanceof MagicalBulletAffix) {
                MagicalBulletAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
                break;
            }
        }
    }
}
