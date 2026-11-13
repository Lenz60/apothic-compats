package ianm1647.apothic_compats.affix.irons_artifice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import ianm1647.apothic_compats.Comp;
import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.data.PlayableSound;
import io.redspace.irons_artifice.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class InfiniteAmmoAffix extends Affix {
    public static final Codec<InfiniteAmmoAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    PlaceboCodecs.setOf(LootRarity.CODEC).fieldOf("rarities").forGetter(a -> a.rarities))
            .apply(inst, InfiniteAmmoAffix::new));

    protected final Set<LootRarity> rarities;

    public InfiniteAmmoAffix(Set<LootRarity> rarities) {
        super(AffixType.ABILITY);
        this.rarities = rarities;
    }

    public static void preventAmmoConsumption(AmmoEvent.Consume event) {
        if (AffixHelper.streamAffixes(event.getShotProfile().itemStack()).anyMatch(a -> a.affix().get() instanceof InfiniteAmmoAffix)) {
            if (event.getAmmoToConsume() > 0) {
                LivingEntity shooter = event.getEntity();
                event.setCanceled(true);
                PlayableSound.of(SoundRegistry.INFINITY_BULLET, 1.0F, 0.9F, 1.1F).play(shooter.level(), shooter.position(), SoundSource.NEUTRAL);
            }
        }
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return Comp.LootCategories.isGun(cat) && this.rarities.contains(rarity);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}
