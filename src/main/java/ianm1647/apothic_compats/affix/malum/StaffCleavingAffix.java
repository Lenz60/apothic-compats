package ianm1647.apothic_compats.affix.malum;

import com.google.common.base.Predicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.AdventureConfig;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import ianm1647.apothic_compats.Comp;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

public class StaffCleavingAffix extends Affix {
    public static final Codec<StaffCleavingAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(LootRarity.mapCodec(CleaveValues.CODEC).fieldOf("values").forGetter(a -> a.values))
            .apply(inst, StaffCleavingAffix::new));

    protected final Map<LootRarity, CleaveValues> values;
    private static boolean cleaving = false;

    public StaffCleavingAffix(Map<LootRarity, CleaveValues> values) {
        super(AffixType.ABILITY);
        this.values = values;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return Comp.LootCategories.isStaff(cat) && this.values.containsKey(rarity);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix." + this.getId() + ".desc", fmt(100.0F * this.getChance(rarity, level)), this.getTargets(rarity, level));
    }

    @Override
    public Component getAugmentingText(ItemStack stack, LootRarity rarity, float level) {
        MutableComponent comp = this.getDescription(stack, rarity, level);
        float minChance = this.getChance(rarity, 0.0F);
        float maxChance = this.getChance(rarity, 1.0F);
        if (minChance != maxChance) {
            Component minComp = Component.translatable("%s%%", fmt(100.0F * minChance));
            Component maxComp = Component.translatable("%s%%", fmt(100.0F * maxChance));
            comp.append(valueBounds(minComp, maxComp));
        }

        int minTargets = this.getTargets(rarity, 0.0F);
        int maxTargets = this.getTargets(rarity, 1.0F);
        if (minTargets != maxTargets) {
            Component minComp = Component.literal(fmt(minTargets));
            Component maxComp = Component.literal(fmt(maxTargets));
            comp.append(valueBounds(minComp, maxComp));
        }
        return comp;
    }

    private float getChance(LootRarity rarity, float level) {
        return this.values.get(rarity).chance.get(level);
    }

    private int getTargets(LootRarity rarity, float level) {
        return this.values.get(rarity).targets.getInt(level);
    }

    @Override
    public void doPostAttack(ItemStack stack, LootRarity rarity, float level, LivingEntity user, Entity target) {
        if (cleaving || user.level().isClientSide) return;
        if (user instanceof Player player && player.getAttackStrengthScale(0.5F) < 0.98F) return;

        cleaving = true;
        float chance = this.getChance(rarity, level);
        int targets = this.getTargets(rarity, level);
        if (user.level().random.nextFloat() < chance && user instanceof Player) {
            Player player = (Player) user;
            List<Entity> nearby = target.level().getEntities(target, new AABB(target.blockPosition()).inflate(6.0), cleavePredicate(user, target));
            for (Entity e : nearby) {
                if (targets > 0) {
                    player.getAttackStrengthScale(300);
                    player.attack(e);
                    --targets;
                }
            }
        }
        cleaving = false;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public static Predicate<Entity> cleavePredicate(Entity user, Entity target) {
        return (e) -> {
            if (e instanceof Animal && !(target instanceof Animal) || e instanceof AbstractVillager && !(target instanceof AbstractVillager)) {
                return false;
            } else if (!AdventureConfig.cleaveHitsPlayers && e instanceof Player) {
                return false;
            } else if (target instanceof Enemy && !(e instanceof Enemy)) {
                return false;
            } else {
                return e != user && e instanceof LivingEntity le && le.isAlive();
            }
        };
    }

    static record CleaveValues(StepFunction chance, StepFunction targets) {
        public static final Codec<CleaveValues> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(StepFunction.CODEC.fieldOf("chance").forGetter(CleaveValues::chance),
                   StepFunction.CODEC.fieldOf("targets").forGetter(CleaveValues::targets))
            .apply(inst, CleaveValues::new));
    }
}
