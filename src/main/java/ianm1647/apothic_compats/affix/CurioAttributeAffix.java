package ianm1647.apothic_compats.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import ianm1647.apothic_compats.item.ModCurioItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;

/**
 * An attribute affix variant for loot curios. Since curio slots are not vanilla equipment slots,
 * attribute application is handled by {@link ModCurioItem} through the Curios API.
 */
public class CurioAttributeAffix extends Affix {

    public static final Codec<CurioAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ForgeRegistries.ATTRIBUTES.getCodec().fieldOf("attribute").forGetter(a -> a.attribute),
            PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values))
        .apply(inst, CurioAttributeAffix::new));

    protected final Attribute attribute;
    protected final Operation operation;
    protected final Map<LootRarity, StepFunction> values;

    public CurioAttributeAffix(Attribute attribute, Operation operation, Map<LootRarity, StepFunction> values) {
        super(AffixType.STAT);
        this.attribute = attribute;
        this.operation = operation;
        this.values = values;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return stack.getItem() instanceof ModCurioItem && this.values.containsKey(rarity);
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public AttributeModifier getModifier(LootRarity rarity, float level, UUID id) {
        return new AttributeModifier(id, "apothic_compats:curio_affix", this.values.get(rarity).get(level), this.operation);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        double value = this.values.get(rarity).get(level);
        MutableComponent valueComp = IFormattableAttribute.toValueComponent(this.attribute, this.operation, value < 0 ? -value : value, AttributesLib.getTooltipFlag());
        if (value > 0.0D) {
            return Component.translatable("attributeslib.modifier.plus", valueComp, Component.translatable(this.attribute.getDescriptionId())).withStyle(ChatFormatting.BLUE);
        }
        return Component.translatable("attributeslib.modifier.take", valueComp, Component.translatable(this.attribute.getDescriptionId())).withStyle(ChatFormatting.RED);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}
