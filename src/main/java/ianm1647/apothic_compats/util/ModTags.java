package ianm1647.apothic_compats.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> CURIOS_FILTER = itemTag("curios_filter");
        public static final TagKey<Item> CURIOS_BLACKLIST = itemTag("curios_blacklist");

        private static TagKey<Item> itemTag(String path) {
            return ItemTags.create(new ResourceLocation("apothic_compats", path));
        }
    }

    public static class Curios {
        public static final TagKey<Item> FEET = curiosTag("feet");

        private static TagKey<Item> curiosTag(String path) {
            return TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("curios", path));
        }
    }

    public static class Biomes {
        public static final TagKey<Biome> IS_MAGNETIC_CAVES = create("magnetic_caves");
        public static final TagKey<Biome> IS_ABYSSAL_CHASM = create("abyssal_chasm");
        public static final TagKey<Biome> IS_CANDY_CAVITY = create("candy_cavity");
        public static final TagKey<Biome> IS_FORLORN_HOLLOWS = create("forlorn_hollows");
        public static final TagKey<Biome> IS_PRIMORDIAL_CAVES = create("primordial_caves");
        public static final TagKey<Biome> IS_TOXIC_CAVES = create("toxic_caves");

        private static TagKey<Biome> create(String path) {
            return TagKey.create(net.minecraft.core.registries.Registries.BIOME, new ResourceLocation("apothic_compats", path));
        }
    }
}
