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
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class HexAffix extends Affix {

    public static final Codec<HexAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
            .apply(inst, HexAffix::new));

    /**
     * NBT key on the target storing the game tick when a crowd-control debuff
     * was last applied by this affix.
     */
    private static final String LAST_DEBUFF_TICK_KEY = "apothic_compats_hex_debuff_tick";

    /** Bullets from one shot land on the same tick; anything within this window is handled once. */
    private static final long SHOT_WINDOW_TICKS = 2L;

    private static final List<SpellEntry> DEBUFF_SPELLS = List.of(
            new SpellEntry("slow", 4, 6),
            new SpellEntry("gust", 6, 10),
            new SpellEntry("root", 5, 10));

    protected final Set<LootRarity> rarities;

    public HexAffix(Set<LootRarity> rarities) {
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

    public static void triggerProc(ServerLevel level, LivingEntity shooter, LivingEntity target, AffixInstance inst) {
        if (level == null || level.isClientSide() || !(shooter instanceof ServerPlayer player)) {
            return;
        }
        if (target == null || hasActiveDebuff(level, target)) {
            return;
        }
        int difficulty = HostilityCompat.getPlayerDifficulty(player);

        SpellEntry entry = DEBUFF_SPELLS.get(ThreadLocalRandom.current().nextInt(DEBUFF_SPELLS.size()));
        AbstractSpell spell = entry.spell();
        if (spell == null) {
            return;
        }
        int spellLevel = entry.resolveLevel(difficulty);

        switch (entry.id()) {
            case "gust" -> {
                spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
                markDebuffed(level, target);
            }
            case "root" -> {
                if (target.getType().is(ModTags.CANT_ROOT)) {
                    // Can't be rooted: skip the proc rather than dealing damage.
                    return;
                }
                castTargeted(level, player, target, spell, spellLevel);
            }
            default -> castTargeted(level, player, target, spell, spellLevel); // slow
        }
    }

    private static boolean hasActiveDebuff(ServerLevel level, LivingEntity target) {
        if (target.getVehicle() instanceof RootEntity) {
            return true;
        }
        long lastTick = target.getPersistentData().getLong(LAST_DEBUFF_TICK_KEY);
        return lastTick > 0 && level.getGameTime() - lastTick <= SHOT_WINDOW_TICKS;
    }

    private static void markDebuffed(ServerLevel level, LivingEntity target) {
        if (target != null) {
            target.getPersistentData().putLong(LAST_DEBUFF_TICK_KEY, level.getGameTime());
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
