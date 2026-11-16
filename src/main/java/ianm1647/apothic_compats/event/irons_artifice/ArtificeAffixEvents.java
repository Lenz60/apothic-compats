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
import net.minecraft.world.entity.LivingEntity;
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
        if (bullet == null || bullet.getOwner() == null || !(bullet.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        var affixes = ianm1647.apothic_compats.ApothicCompats.streamEntityAffixes(bullet).toList();
        for (var inst : affixes) {
            if (inst.affix().get() instanceof MagicalBulletAffix) {
                MagicalBulletAffix.castRandomSpell(shooter.level(), shooter);
                break;
            }
        }
    }
}
