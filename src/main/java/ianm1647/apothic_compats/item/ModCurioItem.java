package ianm1647.apothic_compats.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import ianm1647.apothic_compats.affix.CurioAttributeAffix;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class ModCurioItem extends Item implements ICurioItem {
    public ModCurioItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        for (AffixInstance inst : AffixHelper.getAffixes(stack).values()) {
            if (inst.affix().get() instanceof CurioAttributeAffix affix) {
                map.put(affix.getAttribute(), affix.getModifier(inst.rarity().get(), inst.level(), uuid));
            }
        }
        return map;
    }
}