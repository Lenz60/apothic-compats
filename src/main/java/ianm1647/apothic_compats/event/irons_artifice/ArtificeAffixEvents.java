package ianm1647.apothic_compats.event.irons_artifice;

import ianm1647.apothic_compats.affix.irons_artifice.BulletModifierAffix;
import ianm1647.apothic_compats.affix.irons_artifice.ChargeAffix;
import ianm1647.apothic_compats.affix.irons_artifice.FortifyAffix;
import ianm1647.apothic_compats.affix.irons_artifice.HexAffix;
import ianm1647.apothic_compats.affix.irons_artifice.InfiniteAmmoAffix;
import ianm1647.apothic_compats.affix.irons_artifice.OakskinAffix;
import ianm1647.apothic_compats.affix.irons_artifice.MagicalBulletAffix;
import ianm1647.apothic_compats.affix.irons_artifice.MultiShotAffix;
import ianm1647.apothic_compats.affix.irons_artifice.TelepathicGunAffix;
import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.ironsspellbooks.entity.spells.small_magic_arrow.SmallMagicArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArtificeAffixEvents {

    /** How long (in ticks) to keep in-ground Arrow Volley arrows before despawning them (3 seconds = 60 ticks). */
    private static final int DESPAWN_DELAY_TICKS = 60;

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
            var affix = inst.affix().get();
            if (affix instanceof MagicalBulletAffix) {
                MagicalBulletAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
            } else if (affix instanceof FortifyAffix) {
                FortifyAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
            } else if (affix instanceof OakskinAffix) {
                OakskinAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
            } else if (affix instanceof ChargeAffix) {
                ChargeAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
            } else if (affix instanceof HexAffix) {
                HexAffix.triggerProc((ServerLevel) shooter.level(), shooter, target, inst);
            }
        }

        // Despawn in-ground Arrow Volley arrows after 3 seconds of their last cast.
        if (!shooter.level().isClientSide()) {
            cleanupOldVolleyArrows((ServerLevel) shooter.level(), (ServerPlayer) shooter);
        }
    }

    /**
     * Removes in-ground {@link SmallMagicArrow} projectiles owned by the given player
     * once {@link MagicalBulletAffix#LAST_VOLLEY_CAST_TICK_KEY} indicates the last
     * Arrow Volley cast is older than {@link #DESPAWN_DELAY_TICKS}.
     */
    private static void cleanupOldVolleyArrows(ServerLevel level, ServerPlayer player) {
        long lastCast = player.getPersistentData().getLong(MagicalBulletAffix.LAST_VOLLEY_CAST_TICK_KEY);
        if (lastCast <= 0 || level.getGameTime() - lastCast < DESPAWN_DELAY_TICKS) {
            return;
        }
        var worldBounds = new AABB(-30000000, -30000000, -30000000, 30000000, 30000000, 30000000);
        for (var arrow : level.getEntitiesOfClass(SmallMagicArrow.class, worldBounds,
                arrow -> arrow.getOwner() == player)) {
            arrow.discard();
        }
        player.getPersistentData().putLong(MagicalBulletAffix.LAST_VOLLEY_CAST_TICK_KEY, 0);
    }
}
