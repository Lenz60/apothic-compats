package ianm1647.apothic_compats;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import ianm1647.apothic_compats.affix.ModAffixRegistry;
import ianm1647.apothic_compats.event.AffixEvents;
import ianm1647.apothic_compats.event.AttributeEvents;
import ianm1647.apothic_compats.event.irons_artifice.ArtificeAffixEvents;
import ianm1647.apothic_compats.event.irons_artifice.ArtificeAttributeEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Mod(ApothicCompats.MODID)
public class ApothicCompats {
    public static final String MODID = "apothic_compats";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /**
     * Custom damage type used to re-apply projectile damage as magic damage that bypasses armor,
     * replicating 1.21 damage-tag behavior on 1.20.1.
     */
    public static final ResourceKey<DamageType> MAGIC_PROJECTILE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "magic_projectile"));
    public static final TagKey<DamageType> MAGIC_DAMAGE_TAG = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MODID, "magic_projectile"));

    public ApothicCompats() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AffixEvents());
        MinecraftForge.EVENT_BUS.register(new AttributeEvents());
        if (ModList.get().isLoaded("irons_artifice")) {
            modEventBus.addListener(ArtificeAttributeEvents::applyAttribs);
            MinecraftForge.EVENT_BUS.register(new ArtificeAffixEvents());
            MinecraftForge.EVENT_BUS.register(new ArtificeAttributeEvents());
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.STARTUP_CONFIG);

        Comp.bootstrap(modEventBus);

        ModAffixRegistry.registerAffixes();
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }

    /**
     * Reads affix data from any entity's persistent NBT (the same format {@link AffixHelper} writes for arrows).
     */
    public static Stream<AffixInstance> streamEntityAffixes(Entity entity) {
        CompoundTag afxData = entity.getPersistentData().getCompound(AffixHelper.AFFIX_DATA);
        if (!afxData.contains(AffixHelper.AFFIXES)) return Stream.empty();
        CompoundTag affixes = afxData.getCompound(AffixHelper.AFFIXES);
        DynamicHolder<LootRarity> rarity = AffixHelper.getRarity(afxData);
        if (!rarity.isBound()) rarity = RarityRegistry.getMinRarity();
        Map<DynamicHolder<? extends Affix>, AffixInstance> map = new HashMap<>();
        for (String key : affixes.getAllKeys()) {
            DynamicHolder<Affix> affix = AffixRegistry.INSTANCE.holder(new ResourceLocation(key));
            if (!affix.isBound()) continue;
            map.put(affix, new AffixInstance(affix, ItemStack.EMPTY, rarity, affixes.getFloat(key)));
        }
        return map.values().stream();
    }

    /**
     * 1.20.1 replacement for adding damage tags to a damage source: cancels the incoming attack and
     * re-applies it as a custom magic damage type that bypasses armor, but only if the projectile
     * carries the requested affix.
     */
    public static void convertDamageToMagic(LivingAttackEvent e, Class<? extends Affix> affixClass) {
        DamageSource src = e.getSource();
        if (src.is(MAGIC_DAMAGE_TAG)) return;
        Entity direct = src.getDirectEntity();
        if (direct == null) return;
        if (streamEntityAffixes(direct).noneMatch(a -> a.affix().isBound() && affixClass.isInstance(a.affix().get()))) return;
        e.setCanceled(true);
        LivingEntity target = e.getEntity();
        Holder<DamageType> holder = target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MAGIC_PROJECTILE);
        DamageSource magic = new DamageSource(holder, direct, src.getEntity());
        target.hurt(magic, e.getAmount());
    }
}
