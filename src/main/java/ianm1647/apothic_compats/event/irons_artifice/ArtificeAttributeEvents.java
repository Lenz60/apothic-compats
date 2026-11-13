package ianm1647.apothic_compats.event.irons_artifice;

import ianm1647.apothic_compats.Comp;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.gun.ShotProfile;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArtificeAttributeEvents {

    @SubscribeEvent
    public void composeShot(ComposeShotEvent e) {
        if (e.getEntity() instanceof Player player) {
            addAttribute(Comp.Attributes.Artifice.BULLET_SPREAD.get(), ShotComponents.SPREAD, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.FIRE_RATE.get(), ShotComponents.FIRE_RATE, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.GUN_DAMAGE.get(), ShotComponents.DAMAGE, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.BULLET_KNOCKBACK.get(), ShotComponents.KNOCKBACK, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.RELOAD_SPEED.get(), ShotComponents.RELOAD_SPEED_MULTIPLIER, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.BULLET_PIERCE.get(), ShotComponents.PIERCING, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.LEECH.get(), ShotComponents.LEECH, e.getShotProfile(), player);
            addAttribute(Comp.Attributes.Artifice.GUN_RECOIL.get(), ShotComponents.CAMERA_RECOIL_MULTIPLIER, e.getShotProfile(), player);
        }
    }

    public static void applyAttribs(EntityAttributeModificationEvent e) {
        e.getTypes().forEach(type -> {
            e.add(type, Comp.Attributes.Artifice.BULLET_SPREAD.get());
            e.add(type, Comp.Attributes.Artifice.FIRE_RATE.get());
            e.add(type, Comp.Attributes.Artifice.GUN_DAMAGE.get());
            e.add(type, Comp.Attributes.Artifice.BULLET_KNOCKBACK.get());
            e.add(type, Comp.Attributes.Artifice.RELOAD_SPEED.get());
            e.add(type, Comp.Attributes.Artifice.BULLET_PIERCE.get());
            e.add(type, Comp.Attributes.Artifice.LEECH.get());
            e.add(type, Comp.Attributes.Artifice.GUN_RECOIL.get());
        });
    }

    private static void addAttribute(Attribute attribute, ComponentType<Value> component, ShotProfile profile, Player player) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }

        double baseMultiplier = 0.0;
        Value value = profile.peek(component);

        for (AttributeModifier modifier : inst.getModifiers()) {
            double amount = modifier.getAmount();

            switch (modifier.getOperation()) {
                case ADDITION ->
                        value.addModifier(new ValueModifier(amount, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
                case MULTIPLY_BASE ->
                        baseMultiplier += amount;
                case MULTIPLY_TOTAL ->
                        value.addModifier(new ValueModifier(amount, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
            }
        }

        if (baseMultiplier != 0.0) {
            value.addModifier(new ValueModifier(
                    value.base() * baseMultiplier,
                    ValueModifier.Operation.ADD,
                    ValueModifier.Type.BENEFICIAL
            ));
        }
    }
}
