package ianm1647.apothic_compats.gem.l2hostility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemClass;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class HostilityGemBonuses {
    private HostilityGemBonuses() {}

    public static class DispellResist extends GemBonus {
        public static final Codec<DispellResist> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(gemClass()).apply(inst, DispellResist::new));

        public DispellResist(GemClass gemClass) {
            super(new ResourceLocation("apothic_compats", "dispell_resist"), gemClass);
        }

        @Override public Component getSocketBonusTooltip(ItemStack gem, LootRarity rarity) {
            return Component.translatable("gem_bonus.apothic_compats.dispell_resist.desc").withStyle(ChatFormatting.YELLOW);
        }
        @Override public DispellResist validate() { return this; }
        @Override public boolean supports(LootRarity rarity) { return ancient(rarity); }
        @Override public int getNumberOfUUIDs() { return 0; }
        @Override public Codec<? extends GemBonus> getCodec() { return CODEC; }
    }

    public static class RagnarokResist extends GemBonus {
        public static final Codec<RagnarokResist> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(gemClass()).apply(inst, RagnarokResist::new));

        public RagnarokResist(GemClass gemClass) {
            super(new ResourceLocation("apothic_compats", "ragnarok_resist"), gemClass);
        }

        @Override public Component getSocketBonusTooltip(ItemStack gem, LootRarity rarity) {
            return Component.translatable("gem_bonus.apothic_compats.ragnarok_resist.desc").withStyle(ChatFormatting.YELLOW);
        }
        @Override public RagnarokResist validate() { return this; }
        @Override public boolean supports(LootRarity rarity) { return ancient(rarity); }
        @Override public int getNumberOfUUIDs() { return 0; }
        @Override public Codec<? extends GemBonus> getCodec() { return CODEC; }
    }

    private static boolean ancient(LootRarity rarity) {
        ResourceLocation key = RarityRegistry.INSTANCE.getKey(rarity);
        return key != null && key.getPath().equals("ancient");
    }
}