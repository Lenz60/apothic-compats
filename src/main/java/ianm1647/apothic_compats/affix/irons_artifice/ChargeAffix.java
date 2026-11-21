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
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class ChargeAffix extends Affix {

    public static final Codec<ChargeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
            .apply(inst, ChargeAffix::new));

    private static final SpellEntry SPELL = new SpellEntry("charge", 3);

    protected final Set<LootRarity> rarities;

    public ChargeAffix(Set<LootRarity> rarities) {
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
        // Only re-activate if the player doesn't already have the Charge effect.
        if (player.hasEffect(MobEffectRegistry.CHARGED.get())) {
            return;
        }
        int difficulty = HostilityCompat.getPlayerDifficulty(player);
        AbstractSpell spell = SPELL.spell();
        if (spell == null) {
            return;
        }
        int spellLevel = SPELL.resolveLevel(difficulty);
        spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
    }
}
