package ianm1647.apothic_compats.affix.irons_artifice;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;

/**
 * A spell that can be rolled by a gun spell affix. If minLevel == maxLevel it is a
 * fixed level and never scales. damageMultiplier is only used by direct-damage
 * spells (spell power * multiplier).
 */
public record SpellEntry(String id, int minLevel, int maxLevel, float damageMultiplier) {
    SpellEntry(String id, int level) {
        this(id, level, level, 0.5f);
    }

    SpellEntry(String id, int minLevel, int maxLevel) {
        this(id, minLevel, maxLevel, 0.5f);
    }

    AbstractSpell spell() {
        AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks", id));
        return spell == SpellRegistry.none() ? null : spell;
    }

    /**
     * Difficulty 1-100 -> min level, 101-200 -> min+1, ... capped at maxLevel.
     * (e.g. level 3-5: difficulty 100 -> 3, difficulty 150 -> 4)
     */
    int resolveLevel(int difficulty) {
        if (maxLevel <= minLevel) {
            return minLevel;
        }
        int bonus = difficulty <= 0 ? 0 : (difficulty - 1) / 100;
        return Math.min(maxLevel, minLevel + bonus);
    }
}
