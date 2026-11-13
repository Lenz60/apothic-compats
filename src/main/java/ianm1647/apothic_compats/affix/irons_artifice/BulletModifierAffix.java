package ianm1647.apothic_compats.affix.irons_artifice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import ianm1647.apothic_compats.ApothicCompats;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

public class BulletModifierAffix extends Affix {

    public static final Codec<BulletModifierAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ForgeRegistries.MOB_EFFECTS.getCodec().fieldOf("mob_effect").forGetter(a -> a.effect),
            Target.CODEC.fieldOf("target").forGetter(a -> a.target),
            LootRarity.mapCodec(EffectData.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
            PlaceboCodecs.nullableField(Codec.BOOL, "stack_on_reapply", false).forGetter(a -> a.stackOnReapply),
            PlaceboCodecs.nullableField(Codec.intRange(1, 255), "stacking_limit", 255).forGetter(a -> a.stackingLimit))
        .apply(inst, BulletModifierAffix::new));

    protected final MobEffect effect;
    protected final Target target;
    protected final Map<LootRarity, EffectData> values;
    protected final Set<LootCategory> types;
    protected final boolean stackOnReapply;
    protected final int stackingLimit;

    public BulletModifierAffix(MobEffect effect, Target target, Map<LootRarity, EffectData> values, Set<LootCategory> types, boolean stackOnReapply, int stackingLimit) {
        super(AffixType.POTION);
        this.effect = effect;
        this.target = target;
        this.values = values;
        this.types = types;
        this.stackOnReapply = stackOnReapply;
        this.stackingLimit = stackingLimit;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        EffectData data = this.values.get(rarity);
        MobEffectInstance effectInst = new MobEffectInstance(this.effect, data.duration.getInt(level), data.amplifier.getInt(level));
        MutableComponent comp = this.target.toComponent(toComponent(effectInst));
        int cooldown = data.cooldown;
        if (cooldown != 0) {
            comp.append(" ").append(Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown)));
        }
        if (this.stackOnReapply) {
            comp.append(" ").append(Component.translatable("affix.apotheosis.stacking"));
        }
        return comp;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    public static void onBulletImpact(BulletImpactEvent e) {
        HitResult res = e.getRayTraceResult();
        if (res.getType() != HitResult.Type.ENTITY || !(res instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Bullet bullet = e.getBullet();
        for (var inst : ApothicCompats.streamEntityAffixes(bullet).toList()) {
            if (inst.affix().get() instanceof BulletModifierAffix affix) {
                LootRarity rarity = inst.rarity().get();
                float level = inst.level();
                switch (affix.target) {
                    case BULLET_SELF -> {
                        if (bullet.getOwner() instanceof LivingEntity owner) {
                            affix.applyEffect(owner, rarity, level);
                        }
                    }
                    case BULLET_TARGET -> affix.applyEffect(target, rarity, level);
                }
            }
        }
    }

    private void applyEffect(LivingEntity target, LootRarity rarity, float level) {
        if (target.level().isClientSide()) {
            return;
        }
        EffectData data = this.values.get(rarity);
        int cooldown = data.cooldown;
        if (cooldown != 0 && Affix.isOnCooldown(this.getId(), cooldown, target)) {
            return;
        }
        MobEffectInstance inst = target.getEffect(this.effect);
        if (this.stackOnReapply && inst != null) {
            int duration = Math.max(inst.getDuration(), data.duration.getInt(level));
            int amp = Math.min(this.stackingLimit, inst.getAmplifier() + 1 + data.amplifier.getInt(level));
            target.removeEffect(this.effect);
            target.addEffect(new MobEffectInstance(this.effect, duration, amp, inst.isAmbient(), inst.isVisible()));
        }
        else {
            target.addEffect(data.build(this.effect));
        }
        if (cooldown != 0) {
            Affix.startCooldown(this.getId(), target);
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public static Component toComponent(MobEffectInstance inst) {
        MutableComponent mutablecomponent = Component.translatable(inst.getDescriptionId());
        MobEffect mobeffect = inst.getEffect();

        if (inst.getAmplifier() > 0) {
            mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + inst.getAmplifier()));
        }

        if (inst.getDuration() > 20) {
            mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(inst, 1));
        }

        return mutablecomponent.withStyle(mobeffect.getCategory().getTooltipFormatting());
    }

    public static record EffectData(StepFunction duration, StepFunction amplifier, int cooldown) {

        public static final Codec<EffectData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                StepFunction.CODEC.fieldOf("duration").forGetter(EffectData::duration),
                StepFunction.CODEC.fieldOf("amplifier").forGetter(EffectData::amplifier),
                PlaceboCodecs.nullableField(Codec.INT, "cooldown", 0).forGetter(EffectData::cooldown))
            .apply(inst, EffectData::new));

        public MobEffectInstance build(MobEffect effect) {
            return new MobEffectInstance(effect, this.duration.getInt(1.0F), this.amplifier.getInt(1.0F));
        }
    }

    public static enum Target {
        BULLET_SELF("bullet_self"),
        BULLET_TARGET("bullet_target");

        public static final Codec<Target> CODEC = PlaceboCodecs.enumCodec(Target.class);

        private final String id;

        Target(String id) {
            this.id = id;
        }

        public MutableComponent toComponent(Object... args) {
            return Component.translatable("affix.apothic_compats.target." + this.id, args);
        }
    }
}
