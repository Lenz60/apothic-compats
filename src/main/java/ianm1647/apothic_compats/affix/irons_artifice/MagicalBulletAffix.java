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
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
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

    private enum ProcOutcome {
        DAMAGE,
        BUFF,
        DEBUFF
    }

    private record ProcProfile(int damageWeight, int buffWeight, int debuffWeight, int minLevel, int maxLevel) {
        int totalWeight() {
            return damageWeight + buffWeight + debuffWeight;
        }
    }

    protected final Set<LootRarity> rarities;

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

    public static void triggerProc(ServerLevel level, LivingEntity shooter, LivingEntity target, AffixInstance inst) {
        if (level == null || level.isClientSide() || shooter == null || !(shooter instanceof ServerPlayer player)) {
            return;
        }

        LootRarity rarity = inst.rarity().get();
        ProcProfile profile = getProcProfile(rarity);

        ProcOutcome outcome = selectOutcome(profile);
        int spellLevel = ThreadLocalRandom.current().nextInt(profile.minLevel(), profile.maxLevel() + 1);
        AbstractSpell spell = switch (outcome) {
            case DAMAGE -> SpellRegistry.GUIDING_BOLT_SPELL.get();
            case BUFF -> SpellRegistry.HASTE_SPELL.get();
            case DEBUFF -> SpellRegistry.ROOT_SPELL.get();
        };

        switch (outcome) {
            case BUFF -> {
                // Haste: self-targeted, instant cast on the shooter
                int duration = (int) (spell.getSpellPower(spellLevel, player) * 20);
                int amplifier = spellLevel - 1;
                player.addEffect(new MobEffectInstance(MobEffectRegistry.HASTENED.get(), duration, amplifier));
            }
            case DEBUFF -> {
                // Root: target the entity hit by the bullet, works at gun range
                if (target != null && !target.getType().is(ModTags.CANT_ROOT)) {
                    MagicData magicData = MagicData.getPlayerMagicData(player);
                    magicData.setAdditionalCastData(new TargetEntityCastData(target));
                    spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
                }
            }
            case DAMAGE -> spell.castSpell(level, spellLevel, player, CastSource.NONE, true);
        }
    }

    private static ProcOutcome selectOutcome(ProcProfile profile) {
        int roll = ThreadLocalRandom.current().nextInt(profile.totalWeight());
        if (roll < profile.damageWeight()) {
            return ProcOutcome.DAMAGE;
        }
        roll -= profile.damageWeight();
        if (roll < profile.buffWeight()) {
            return ProcOutcome.BUFF;
        }
        return ProcOutcome.DEBUFF;
    }
}
