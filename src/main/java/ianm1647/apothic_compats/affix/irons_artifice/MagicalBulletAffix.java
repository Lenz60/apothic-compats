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
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MagicalBulletAffix extends Affix {

    public static final Codec<MagicalBulletAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
            .apply(inst, MagicalBulletAffix::new));

    /**
     * NBT key on the target storing the game tick when a crowd-control debuff
     * (Gust/Root/Slow) was last applied by this affix.
     */
    private static final String LAST_DEBUFF_TICK_KEY = "apothic_compats_magical_bullet_debuff_tick";

    /**
     * NBT key on the shooter storing the game tick when Arrow Volley was last cast
     * by this affix.
     */
    private static final String LAST_VOLLEY_TICK_KEY = "apothic_compats_magical_bullet_volley_tick";

    /**
     * Bullets from one shot (shotgun pellets, etc.) land on the same tick, so
     * anything
     * applied within this window counts as "already handled by this shot".
     */
    private static final long SHOT_WINDOW_TICKS = 2L;

    /** Difficulty levels needed for +1 spell level. */
    private static final int DIFFICULTY_PER_SPELL_LEVEL = 100;

    private static final String ARROW_VOLLEY_ID = "arrow_volley";
    private static final String GUIDING_BOLT_ID = "guiding_bolt";

    private enum ProcOutcome {
        DAMAGE,
        BUFF,
        DEBUFF
    }

    /**
     * A spell that can be rolled. If minLevel == maxLevel it is a fixed level and
     * never scales.
     * damageMultiplier is only used by direct-damage spells (spell power *
     * multiplier).
     */
    private record SpellEntry(String id, int minLevel, int maxLevel, float damageMultiplier) {
        SpellEntry(String id, int level) {
            this(id, level, level, 0.5f);
        }

        SpellEntry(String id, int minLevel, int maxLevel) {
            this(id, minLevel, maxLevel, 0.5f);
        }

        AbstractSpell spell() {
            AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", id));
            return spell == SpellRegistry.none() ? null : spell;
        }

        /**
         * Difficulty 1-100 -> min level, 101-200 -> min+1, ... capped at maxLevel.
         * (e.g. level 3-5: difficulty 100 -> 3, difficulty 150 -> 4)
         */
        int resolveLevel(int difficulty) {
            if (maxLevel <= minLevel) {
                return minLevel;
            }
            int bonus = difficulty <= 0 ? 0 : (difficulty - 1) / DIFFICULTY_PER_SPELL_LEVEL;
            return Math.min(maxLevel, minLevel + bonus);
        }
    }

    private static final List<SpellEntry> DAMAGE_SPELLS = List.of(
            new SpellEntry(GUIDING_BOLT_ID, 5, 10),
            new SpellEntry("fireball", 3, 5),
            new SpellEntry("magic_missile", 5, 10),
            new SpellEntry("blood_needles", 5, 10),
            new SpellEntry("lightning_bolt", 5, 10),
            new SpellEntry(ARROW_VOLLEY_ID, 4, 6),
            new SpellEntry("eldritch_blast", 3, 5));

    private static final List<SpellEntry> BUFF_SPELLS = List.of(
            new SpellEntry("fortify", 10),
            new SpellEntry("oakskin", 8),
            new SpellEntry("charge", 3));

    private static final List<SpellEntry> DEBUFF_SPELLS = List.of(
            new SpellEntry("slow", 4, 6),
            new SpellEntry("gust", 6, 10),
            new SpellEntry("root", 5, 10));

    /**
     * Outcome weights per rarity. haste* is the original Haste buff, which stays in
     * the buff pool
     * and keeps its original rarity-based level range.
     */
    private record ProcProfile(int damageWeight, int buffWeight, int debuffWeight, int hasteMin, int hasteMax) {
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
        int difficulty = HostilityCompat.getPlayerDifficulty(player);

        // A debuff is only allowed if the target has a valid hit and is not already
        // debuffed. Otherwise this bullet falls back to damage/buff.
        boolean debuffAllowed = target != null && !hasActiveDebuff(level, target);

        switch (selectOutcome(profile, debuffAllowed)) {
            case BUFF -> applyRandomBuff(level, player, profile, difficulty);
            case DAMAGE -> applyRandomDamage(level, player, target, difficulty);
            case DEBUFF -> applyRandomDebuff(level, player, target, difficulty);
        }
    }

    // -------------------------------------------------------------------------
    // Outcome selection
    // -------------------------------------------------------------------------

    private static ProcOutcome selectOutcome(ProcProfile profile, boolean debuffAllowed) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (!debuffAllowed) {
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
        return ProcOutcome.DEBUFF;
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
    // Anti-stacking
    // -------------------------------------------------------------------------

    private static boolean hasActiveDebuff(ServerLevel level, LivingEntity target) {
        // Root is a persistent state we can read directly
        if (target.getVehicle() instanceof RootEntity) {
            return true;
        }
        // Gust/Slow: track by tick. Covers multiple bullets of one shot.
        long lastTick = target.getPersistentData().getLong(LAST_DEBUFF_TICK_KEY);
        return lastTick > 0 && level.getGameTime() - lastTick <= SHOT_WINDOW_TICKS;
    }

    private static void markDebuffed(ServerLevel level, LivingEntity target) {
        if (target != null) {
            target.getPersistentData().putLong(LAST_DEBUFF_TICK_KEY, level.getGameTime());
        }
    }

    /**
     * Arrow Volley may only be cast once per shot, no matter how many bullets the
     * shot has.
     */
    private static boolean isVolleyAllowed(ServerLevel level, ServerPlayer player) {
        long lastTick = player.getPersistentData().getLong(LAST_VOLLEY_TICK_KEY);
        return !(lastTick > 0 && level.getGameTime() - lastTick <= SHOT_WINDOW_TICKS);
    }

    private static void markVolley(ServerLevel level, ServerPlayer player) {
        player.getPersistentData().putLong(LAST_VOLLEY_TICK_KEY, level.getGameTime());
    }

    // -------------------------------------------------------------------------
    // Damage
    // -------------------------------------------------------------------------

    private static void applyRandomDamage(ServerLevel level, ServerPlayer player, LivingEntity target, int difficulty) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Roll the spell. If Arrow Volley was already cast by this shot, re-roll among
        // the others.
        boolean volleyAllowed = isVolleyAllowed(level, player);
        List<SpellEntry> pool = DAMAGE_SPELLS.stream()
                .filter(e -> volleyAllowed || !e.id().equals(ARROW_VOLLEY_ID))
                .toList();
        SpellEntry entry = pool.get(random.nextInt(pool.size()));

        AbstractSpell spell = entry.spell();
        if (spell == null) {
            return; // spell not present in this Iron's Spellbooks version
        }
        int spellLevel = entry.resolveLevel(difficulty);

        if (entry.id().equals(ARROW_VOLLEY_ID)) {
            // The one damage spell that keeps its real cast + animation
            markVolley(level, player);
            spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
            return;
        }

        if (target == null) {
            return;
        }

        // Direct damage, no projectile/AoE entity and no cast animation (avoids server
        // lag): sound only
        float damage = spell.getSpellPower(spellLevel, player) * entry.damageMultiplier();
        target.invulnerableTime = 0; // the bullet itself just hit, don't let i-frames swallow the proc
        target.hurt(spell.getDamageSource(player), damage);
        spell.getCastFinishSound().ifPresent(sound -> target.playSound(sound, 1.0f, 1.0f));

        if (entry.id().equals(GUIDING_BOLT_ID)) {
            // Guiding bolt debuff (slows and magnetizes). This is an effect, not the
            // Gust/Root/Slow crowd-control, so it is intentionally not part of the
            // anti-stack gate.
            target.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT.get(), 20 * 30, spellLevel - 1));
        }
    }

    // -------------------------------------------------------------------------
    // Buffs
    // -------------------------------------------------------------------------

    private static void applyRandomBuff(ServerLevel level, ServerPlayer player, ProcProfile profile, int difficulty) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Pool = Haste (original) + Fortify / Oakskin / Charge
        int roll = random.nextInt(BUFF_SPELLS.size() + 1);
        if (roll == BUFF_SPELLS.size()) {
            applyHaste(player, random.nextInt(profile.hasteMin(), profile.hasteMax() + 1));
            return;
        }

        SpellEntry entry = BUFF_SPELLS.get(roll);
        AbstractSpell spell = entry.spell();
        if (spell == null) {
            return;
        }
        // Self-targeted buff, fixed level
        spell.castSpell(level, entry.resolveLevel(difficulty), player, CastSource.NONE, true);
    }

    private static void applyHaste(ServerPlayer player, int spellLevel) {
        // Haste: self-targeted, instant on the shooter
        AbstractSpell spell = SpellRegistry.HASTE_SPELL.get();
        int duration = (int) (spell.getSpellPower(spellLevel, player) * 20);
        int amplifier = spellLevel - 1;
        player.addEffect(new MobEffectInstance(MobEffectRegistry.HASTENED.get(), duration, amplifier));
    }

    // -------------------------------------------------------------------------
    // Debuffs
    // -------------------------------------------------------------------------

    private static void applyRandomDebuff(ServerLevel level, ServerPlayer player, LivingEntity target, int difficulty) {
        SpellEntry entry = DEBUFF_SPELLS.get(ThreadLocalRandom.current().nextInt(DEBUFF_SPELLS.size()));
        AbstractSpell spell = entry.spell();
        if (spell == null || target == null) {
            applyRandomDamage(level, player, target, difficulty);
            return;
        }
        int spellLevel = entry.resolveLevel(difficulty);

        switch (entry.id()) {
            case "gust" -> {
                // AoE knockback, no targeting data needed
                spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
                markDebuffed(level, target);
            }
            case "root" -> {
                if (target.getType().is(ModTags.CANT_ROOT)) {
                    // Target can't be rooted: don't waste the proc, deal damage instead
                    applyRandomDamage(level, player, target, difficulty);
                    return;
                }
                castTargeted(level, player, target, spell, spellLevel);
            }
            default -> castTargeted(level, player, target, spell, spellLevel); // slow
        }
    }

    private static void castTargeted(ServerLevel level, ServerPlayer player, LivingEntity target, AbstractSpell spell,
            int spellLevel) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(new TargetEntityCastData(target));
        spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
        markDebuffed(level, target);
    }
}