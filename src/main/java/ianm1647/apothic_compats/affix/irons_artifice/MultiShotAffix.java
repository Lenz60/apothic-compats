package ianm1647.apothic_compats.affix.irons_artifice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import ianm1647.apothic_compats.Comp;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.gun.ShotProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class MultiShotAffix extends Affix {
    public static final Codec<MultiShotAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values))
            .apply(inst, MultiShotAffix::new));

    protected final Map<LootRarity, StepFunction> values;

    public MultiShotAffix(Map<LootRarity, StepFunction> values) {
        super(AffixType.ABILITY);
        this.values = values;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix." + this.getId() + ".desc", fmt(this.values.get(rarity).get(level)));
    }

    public static void multiplyBullets(ComposeShotEvent event) {
        ShotProfile profile = event.getShotProfile();
        ItemStack weapon = profile.itemStack();
        var inst = AffixHelper.getAffixes(weapon);

        for (AffixInstance a : inst.values()) {
            if (a.affix().get() instanceof MultiShotAffix affix) {
                profile.components().modifyValue(ShotComponents.PROJECTILE_COUNT, new ValueModifier(affix.values.get(a.rarity().get()).get(a.level()), ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
            }
        }
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return Comp.LootCategories.isGun(cat) && this.values.containsKey(rarity);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}
