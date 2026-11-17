package ianm1647.apothic_compats.affix.irons_artifice;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.modifier.PostHitEffect;
import io.redspace.irons_artifice.utils.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;

public class MobEffectPostHit implements PostHitEffect {
    private final MobEffect effect;
    private int durationTicks;
    private int amplifier;

    public MobEffectPostHit(MobEffect effect, int durationTicks, int amplifier) {
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    public void addDuration(int ticks) {
        this.durationTicks += ticks;
    }

    public void addAmplifier(int amount) {
        this.amplifier += amount;
    }

    public int getDurationTicks() {
        return this.durationTicks;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public MobEffect getEffect() {
        return this.effect;
    }

    @Override
    public void postHit(ServerLevel level, Bullet bullet, HitResult hitResult, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (this.effect.getCategory() == MobEffectCategory.HARMFUL && !Utils.canHarm(bullet.getOwner(), entity)) {
            return;
        }

        MobEffectInstance existing = living.getEffect(this.effect);
        int duration = Math.max(this.durationTicks, existing != null ? existing.getDuration() : 0);
        int amp = Math.max(this.amplifier, existing != null ? existing.getAmplifier() : 0);
        living.addEffect(new MobEffectInstance(this.effect, duration, amp), bullet.getOwner());
    }
}
