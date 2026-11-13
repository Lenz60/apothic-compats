package ianm1647.apothic_compats;

import com.aetherteam.aether.item.combat.DartShooterItem;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import ianm1647.apothic_compats.item.ModCurioItem;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Comp {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ApothicCompats.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ApothicCompats.MODID);

    public static class Attributes {
        public static class Artifice {
            public static final RegistryObject<Attribute> BULLET_SPREAD =
                    ATTRIBUTES.register("bullet_spread", () -> new RangedAttribute("apothic_compats:bullet_spread", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> FIRE_RATE =
                    ATTRIBUTES.register("fire_rate", () -> new RangedAttribute("apothic_compats:fire_rate", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> GUN_DAMAGE =
                    ATTRIBUTES.register("gun_damage", () -> new RangedAttribute("apothic_compats:gun_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> BULLET_KNOCKBACK =
                    ATTRIBUTES.register("bullet_knockback", () -> new RangedAttribute("apothic_compats:bullet_knockback", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> RELOAD_SPEED =
                    ATTRIBUTES.register("reload_speed", () -> new RangedAttribute("apothic_compats:reload_speed", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> BULLET_PIERCE =
                    ATTRIBUTES.register("bullet_pierce", () -> new RangedAttribute("apothic_compats:bullet_pierce", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> LEECH =
                    ATTRIBUTES.register("bullet_leech", () -> new RangedAttribute("apothic_compats:bullet_leech", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            public static final RegistryObject<Attribute> GUN_RECOIL =
                    ATTRIBUTES.register("gun_recoil", () -> new RangedAttribute("apothic_compats:gun_recoil", 0.0D, 0.0D, 2048.0D).setSyncable(true));

        /** Empty method: forces this class to initialize its RegistryObjects early, at mod construction. */
        private static void forceLoad() {}
        }
    }

    public static class Curios {
        public static final RegistryObject<Item> BACK_PLATE = ITEMS.register("back_plate", Curios::curioItem);
        public static final RegistryObject<Item> FLORID_BELT = ITEMS.register("florid_belt", Curios::curioItem);
        public static final RegistryObject<Item> BODY_CHAIN = ITEMS.register("body_chain", Curios::curioItem);
        public static final RegistryObject<Item> FLASHY_BRACELET = ITEMS.register("flashy_bracelet", Curios::curioItem);
        public static final RegistryObject<Item> FANCY_CHARM = ITEMS.register("fancy_charm", Curios::curioItem);
        public static final RegistryObject<Item> EMBELLISHED_CURIO = ITEMS.register("embellished_curio", Curios::curioItem);
        public static final RegistryObject<Item> ADORNED_BOOTS = ITEMS.register("adorned_boots", Curios::curioItem);
        public static final RegistryObject<Item> SHOWY_GLOVES = ITEMS.register("showy_gloves", Curios::curioItem);
        public static final RegistryObject<Item> HEAD_COVER = ITEMS.register("head_cover", Curios::curioItem);
        public static final RegistryObject<Item> ORNATE_RING = ITEMS.register("ornate_ring", Curios::curioItem);
        public static final RegistryObject<Item> ORNAMENTED_NECKLACE = ITEMS.register("ornamented_necklace", Curios::curioItem);

        private static Item curioItem() {
            // Lazily construct the ICurioItem implementation only when Curios is present,
            // so the interface class is never resolved when Curios is absent.
            if (ModList.get().isLoaded("curios")) {
                return new ModCurioItem();
            }
            return new Item(new Item.Properties().stacksTo(1));
        }
    }

    public static class LootCategories {
        private static final EquipmentSlot[] MAINHAND = { EquipmentSlot.MAINHAND };
        private static final EquipmentSlot[] NO_SLOTS = {};

        public static LootCategory GUN;
        public static LootCategory DART_SHOOTER;
        public static LootCategory SCYTHE;
        public static LootCategory STAFF;
        public static LootCategory POTATO_CANNON;

        public static LootCategory BACK;
        public static LootCategory BELT;
        public static LootCategory BODY;
        public static LootCategory BRACELET;
        public static LootCategory CHARM;
        public static LootCategory CURIO;
        public static LootCategory FEET;
        public static LootCategory HANDS;
        public static LootCategory HEAD;
        public static LootCategory RING;
        public static LootCategory NECKLACE;

        public static void register() {
            GUN = LootCategory.register(null, "gun", s -> ModList.get().isLoaded("irons_artifice") && s.getItem() instanceof GunItem, MAINHAND);
            DART_SHOOTER = LootCategory.register(null, "dart_shooter", s -> ModList.get().isLoaded("aether") && s.getItem() instanceof DartShooterItem, MAINHAND);
            SCYTHE = LootCategory.register(null, "scythe", s -> ModList.get().isLoaded("malum") && s.getItem() instanceof MalumScytheItem, MAINHAND);
            STAFF = LootCategory.register(null, "staff", s -> ModList.get().isLoaded("malum") && s.getItem() instanceof AbstractStaffItem, MAINHAND);
            POTATO_CANNON = LootCategory.register(null, "potato_cannon", s -> ModList.get().isLoaded("create") && s.getItem() instanceof PotatoCannonItem, MAINHAND);

            BACK = curio("back", Curios.BACK_PLATE);
            BELT = curio("belt", Curios.FLORID_BELT);
            BODY = curio("body", Curios.BODY_CHAIN);
            BRACELET = curio("bracelet", Curios.FLASHY_BRACELET);
            CHARM = curio("charm", Curios.FANCY_CHARM);
            CURIO = curio("curio", Curios.EMBELLISHED_CURIO);
            FEET = curio("feet", Curios.ADORNED_BOOTS);
            HANDS = curio("hands", Curios.SHOWY_GLOVES);
            HEAD = curio("head", Curios.HEAD_COVER);
            RING = curio("ring", Curios.ORNATE_RING);
            NECKLACE = curio("necklace", Curios.ORNAMENTED_NECKLACE);
        }

        private static LootCategory curio(String name, RegistryObject<Item> item) {
            return LootCategory.register(null, name, s -> s.getItem() == item.get(), NO_SLOTS);
        }

        public static boolean isGun(LootCategory cat) {
            return cat == GUN;
        }

        public static boolean isDartShooter(LootCategory cat) {
            return cat == DART_SHOOTER;
        }

        public static boolean isScythe(LootCategory cat) {
            return cat == SCYTHE;
        }

        public static boolean isStaff(LootCategory cat) {
            return cat == STAFF;
        }

        public static boolean isPotatoCannon(LootCategory cat) {
            return cat == POTATO_CANNON;
        }
    }

    public static void bootstrap(IEventBus bus) {
        ATTRIBUTES.register(bus);
        ITEMS.register(bus);
        LootCategories.register();
        // Force lazy nested-class initialization NOW (during mod construction),
        // so all DeferredRegister.register() calls happen before RegisterEvent fires.
        if (ModList.get().isLoaded("irons_artifice")) {
            Attributes.Artifice.forceLoad();
        }
    }
}
