package ianm1647.apothic_compats.event;

import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AttributeEvents {

    @SubscribeEvent
    public void proj(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof Projectile proj) {
            if (proj.level().isClientSide || proj.getPersistentData().getBoolean("apothic_compats.proj.done")) return;
            if (proj.getOwner() instanceof LivingEntity le) {
                proj.setDeltaMovement(proj.getDeltaMovement().scale(le.getAttributeValue(ALObjects.Attributes.ARROW_VELOCITY.get())));
            }
            proj.getPersistentData().putBoolean("apothic_compats.proj.done", true);
        }
    }
}
