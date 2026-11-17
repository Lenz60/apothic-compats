package ianm1647.apothic_compats.affix.l2hostility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public class DifficultyLevelAffix extends Affix {
    public static final Codec<DifficultyLevelAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(LootRarity.mapCodec(LevelData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
            .apply(inst, DifficultyLevelAffix::new));

    private final Map<LootRarity, LevelData> values;
    private final Set<LootCategory> types;

    public DifficultyLevelAffix(Map<LootRarity, LevelData> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.values = values;
        this.types = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        LevelData data = values.get(rarity);
        return data == null ? Component.empty() : Component.translatable("affix.apothic_compats.difficulty_level.desc", data.bonus().getInt(level));
    }

    @Override
    public Component getAugmentingText(ItemStack stack, LootRarity rarity, float level) {
        LevelData data = values.get(rarity);
        if (data == null) return Component.empty();
        int current = data.bonus().getInt(level);
        int min = data.bonus().getInt(0);
        int max = data.bonus().getInt(1);
        MutableComponent text = Component.translatable("affix.apothic_compats.difficulty_level.desc", current);
        if (min != max) text.append(Affix.valueBounds(Component.literal(String.valueOf(min)), Component.literal(String.valueOf(max))));
        return text;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory category, LootRarity rarity) {
        return category != null && !category.isNone() && (types.isEmpty() || types.contains(category)) && values.containsKey(rarity);
    }

    public record LevelData(StepFunction bonus) {
        public static final Codec<LevelData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(StepFunction.CODEC.optionalFieldOf("bonus", StepFunction.constant(10)).forGetter(LevelData::bonus))
                .apply(inst, LevelData::new));
    }
}