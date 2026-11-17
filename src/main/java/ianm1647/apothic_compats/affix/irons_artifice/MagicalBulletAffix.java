package ianm1647.apothic_compats.affix.irons_artifice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import ianm1647.apothic_compats.Comp;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MagicalBulletAffix extends Affix {

    public static final Codec<MagicalBulletAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
            .apply(inst, MagicalBulletAffix::new));

    /**
     * NBT key on the target storing the game tick when a crowd-control debuff
     * (Gust/Root) was last applied by this affix.
     */
    private static final String LAST_DEBUFF_TICK_KEY = "apothic_compats_magical_bullet_debuff_tick";

    /**
     * Bullets from one shot (shotgun pellets, etc.) land on the same tick, so any
     * debuff applied within this window counts as "already debuffed by this shot".
     */
    private static final long SHOT_WINDOW_TICKS = 2L;

    private enum ProcOutcome {
        DAMAGE,
        BUFF,
        GUST,
        ROOT
    }

    private record ProcProfile(int damageWeight, int buffWeight, int debuffWeight, int minLevel, int maxLevel) {
        int totalWeight() {
            return damageWeight + buffWeight + debuffWeight;
        }

        int nonDebuffWeight() {
            return damageWeight + buffWeight;
        }
    }

    protected final Set<LootRarity> rarities;

    public MagicalBulletAffix(Set<LootRarity> rarities) {
        super(AffixType.ABILITY);
        this.rarities = rarities;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return Comp.LootCategories.isGun(cat) && this.rarities.contains(rarity);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    // -------------------------------------------------------------------------
    // Proc entry point
    // -------------------------------------------------------------------------

    public static void triggerProc(ServerLevel level, LivingEntity shooter, LivingEntity target, AffixInstance inst) {
        if (level == null || level.isClientSide() || !(shooter instanceof ServerPlayer player)) {
            return;
        }

        ProcProfile profile = getProcProfile(inst.rarity().get());

        // A debuff (Gust/Root) is only allowed if the target has a valid hit and is
        // not already debuffed. Otherwise this bullet falls back to damage/buff.
        boolean debuffAllowed = target != null && !hasActiveDebuff(level, target);

        ProcOutcome outcome = selectOutcome(profile, debuffAllowed);
        int spellLevel = ThreadLocalRandom.current().nextInt(profile.minLevel(), profile.maxLevel() + 1);

        switch (outcome) {
            case BUFF -> applyBuff(player, spellLevel);
            case DAMAGE -> applyDamage(player, target, spellLevel);
            case GUST -> applyGust(level, player, target);
            case ROOT -> {
                if (!applyRoot(level, player, target)) {
                    // Target can't be rooted: don't waste the proc, deal damage instead
                    applyDamage(player, target, spellLevel);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Outcome selection
    // -------------------------------------------------------------------------

    private static ProcOutcome selectOutcome(ProcProfile profile, boolean debuffAllowed) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (!debuffAllowed) {
            // Target already debuffed (or no target): re-roll between damage and buff only,
            // keeping their relative weights.
            int roll = random.nextInt(profile.nonDebuffWeight());
            return roll < profile.damageWeight() ? ProcOutcome.DAMAGE : ProcOutcome.BUFF;
        }

        int roll = random.nextInt(profile.totalWeight());
        if (roll < profile.damageWeight()) {
            return ProcOutcome.DAMAGE;
        }
        roll -= profile.damageWeight();
        if (roll < profile.buffWeight()) {
            return ProcOutcome.BUFF;
        }
        // Debuff slice: 50% Gust, 50% Root
        return random.nextBoolean() ? ProcOutcome.GUST : ProcOutcome.ROOT;
    }

    private static ProcProfile getProcProfile(LootRarity rarity) {
        int ordinal = rarity.ordinal();
        if (ordinal <= 0) {
            return new ProcProfile(80, 12, 8, 2, 3);
        }
        if (ordinal == 1) {
            return new ProcProfile(75, 15, 10, 2, 3);
        }
        if (ordinal == 2) {
            return new ProcProfile(70, 18, 12, 3, 4);
        }
        if (ordinal == 3) {
            return new ProcProfile(65, 22, 13, 4, 5);
        }
        return new ProcProfile(60, 25, 15, 5, 6);
    }

    // -------------------------------------------------------------------------
    // Debuff tracking (anti-stacking)
    // -------------------------------------------------------------------------

    private static boolean hasActiveDebuff(ServerLevel level, LivingEntity target) {
        // Root is a persistent state we can read directly
        if (target.getVehicle() instanceof RootEntity) {
            return true;
        }
        // Gust is instant, so track it by tick. Covers multiple bullets of one shot.
        long lastTick = target.getPersistentData().getLong(LAST_DEBUFF_TICK_KEY);
        return lastTick > 0 && level.getGameTime() - lastTick <= SHOT_WINDOW_TICKS;
    }

    private static void markDebuffed(ServerLevel level, LivingEntity target) {
        if (target != null) {
            target.getPersistentData().putLong(LAST_DEBUFF_TICK_KEY, level.getGameTime());
        }
    }

    // -------------------------------------------------------------------------
    // Effects
    // -------------------------------------------------------------------------

    private static void applyBuff(ServerPlayer player, int spellLevel) {
        // Haste: self-targeted, instant on the shooter
        AbstractSpell spell = SpellRegistry.HASTE_SPELL.get();
        int duration = (int) (spell.getSpellPower(spellLevel, player) * 20);
        int amplifier = spellLevel - 1;
        player.addEffect(new MobEffectInstance(MobEffectRegistry.HASTENED.get(), duration, amplifier));
    }

    private static void applyDamage(ServerPlayer player, LivingEntity target, int spellLevel) {
        if (target == null) {
            return;
        }
        // Direct damage, no GuidingBoltProjectile entity (avoids server lag)
        AbstractSpell spell = SpellRegistry.GUIDING_BOLT_SPELL.get();
        float damage = spell.getSpellPower(spellLevel, player) * 0.5f;
        target.hurt(player.damageSources().magic(), damage);
        target.playSound(SoundRegistry.GUIDING_BOLT_CAST.get(), 1.0f, 1.0f);
        // Guiding bolt debuff (slows and magnetizes). This is an effect, not the
        // Gust/Root crowd-control, so it is intentionally not part of the anti-stack
        // gate.
        target.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT.get(), 20 * 30, spellLevel - 1));
    }

    private static void applyGust(ServerLevel level, ServerPlayer player, LivingEntity target) {
        // AoE knockback at level 6, no targeting data needed
        SpellRegistry.GUST_SPELL.get().castSpell(level, 10, player, CastSource.NONE, true);
        markDebuffed(level, target);
    }

    /**
     * @return true if root was cast, false if the target is immune to it
     */
    private static boolean applyRoot(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (target.getType().is(ModTags.CANT_ROOT)) {
            return false;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(new TargetEntityCastData(target));
        SpellRegistry.ROOT_SPELL.get().castSpell(level, 5, player, CastSource.NONE, true);
        markDebuffed(level, target);
        return true;
    }
}