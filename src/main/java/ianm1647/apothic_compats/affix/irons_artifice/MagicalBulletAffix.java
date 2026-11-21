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
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
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




    private static final String GUIDING_BOLT_ID = "guiding_bolt";
    private static final String ARROW_VOLLEY_ID = "arrow_volley";

    /**
     * NBT key on the shooter storing the game tick when Arrow Volley was last cast
     * by this affix.
     */
    private static final String LAST_VOLLEY_TICK_KEY = "apothic_compats_magical_bullet_volley_tick";

    /**
     * NBT key on the shooter storing the game tick when Arrow Volley's projectile
     * volley was last spawned (used to despawn in-ground arrows after a delay).
     */
    public static final String LAST_VOLLEY_CAST_TICK_KEY = "apothic_compats_magical_bullet_volley_cast_tick";

    /**
     * Bullets from one shot (shotgun pellets, etc.) land on the same tick, so
     * anything applied within this window counts as "already handled by this shot".
     */
    private static final long SHOT_WINDOW_TICKS = 2L;



    private static final List<SpellEntry> DAMAGE_SPELLS = List.of(
            new SpellEntry(GUIDING_BOLT_ID, 5, 10),
            new SpellEntry("fireball", 3, 5),
            new SpellEntry("magic_missile", 5, 10),
            new SpellEntry("blood_needles", 5, 10),
            new SpellEntry("lightning_bolt", 5, 10),
            new SpellEntry(ARROW_VOLLEY_ID, 4, 6),
            new SpellEntry("eldritch_blast", 3, 5));




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

        int difficulty = HostilityCompat.getPlayerDifficulty(player);
        applyRandomDamage(level, player, target, difficulty);
    }


    // -------------------------------------------------------------------------
    // Anti-stacking
    // -------------------------------------------------------------------------

    /**
     * Arrow Volley may only be cast once per shot, no matter how many bullets the
     * shot has.
     */
    private static boolean isVolleyAllowed(ServerLevel level, ServerPlayer player) {
        long lastTick = player.getPersistentData().getLong(LAST_VOLLEY_TICK_KEY);
        return !(lastTick > 0 && level.getGameTime() - lastTick <= SHOT_WINDOW_TICKS);
    }

    private static void markVolley(ServerLevel level, ServerPlayer player) {
        long now = level.getGameTime();
        player.getPersistentData().putLong(LAST_VOLLEY_TICK_KEY, now);
        player.getPersistentData().putLong(LAST_VOLLEY_CAST_TICK_KEY, now);
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



}