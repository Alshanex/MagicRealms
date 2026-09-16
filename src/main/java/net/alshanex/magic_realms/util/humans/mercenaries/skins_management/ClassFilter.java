package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import com.mojang.serialization.Codec;
import net.alshanex.magic_realms.util.humans.combat.CombatClass;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

public enum ClassFilter implements StringRepresentable {
    ANY("any"),
    COMMON("common"),
    MAGE("mage"),
    ROGUE("rogue"),
    WARRIOR("warrior");

    public static final Codec<ClassFilter> CODEC = StringRepresentable.fromEnum(ClassFilter::values);

    private final String name;

    ClassFilter(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Matches either the exact class, the ANY wildcard, or the COMMON fallback (COMMON clothes are eligible for any entity class).
     */
    public boolean matches(@Nullable CombatClass combatClass) {
        if (this == ANY) return true;
        if (this == COMMON) return true;
        return combatClass != null && this.name.equalsIgnoreCase(combatClass.skinCategory());
    }

    public boolean isCommonFallback() {
        return this == COMMON;
    }
}
