package ianm1647.apothic_compats.affix.irons_artifice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import ianm1647.apothic_compats.Comp;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
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

    protected final Set<LootRarity> rarities;

    // Lightweight spells only — avoids heavy animation / AoE spells (blizzard, starfall, volley, etc.)
    // to prevent lag when multi-bullet guns (e.g. blunderbuss firing 20+ projectiles per shot)
    // trigger this affix on every bullet impact.
    private static final List<Holder<AbstractSpell>> LIGHTWEIGHT_SPELLS = List.of(
        SpellRegistry.MAGIC_ARROW_SPELL.get(),
        SpellRegistry.FIRE_ARROW_SPELL.get(),
        SpellRegistry.FIREBOLT_SPELL.get(),
        SpellRegistry.ICICLE_SPELL.get(),
        SpellRegistry.MAGIC_MISSILE_SPELL.get(),
        SpellRegistry.GUIDING_BOLT_SPELL.get(),
        SpellRegistry.FIRECRACKER_SPELL.get(),
        SpellRegistry.POISON_ARROW_SPELL.get()
    );

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

    /**
     * Casts a random lightweight spell from the shooter.
     * Server-side only. Used when a bullet from a gun with this affix hits a target.
     */
    public static void castRandomSpell(ServerLevel level, LivingEntity shooter) {
        if (level.isClientSide() || shooter == null || LIGHTWEIGHT_SPELLS.isEmpty()) {
            return;
        }
        Holder<AbstractSpell> spell = LIGHTWEIGHT_SPELLS.get(ThreadLocalRandom.current().nextInt(LIGHTWEIGHT_SPELLS.size()));
        spell.get().cast(level, shooter, 1.0F);
    }
}
