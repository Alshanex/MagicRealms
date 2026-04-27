package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import com.mojang.serialization.Codec;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.minecraft.util.StringRepresentable;

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
    public boolean matches(EntityClass entityClass) {
        if (this == ANY) return true;
        if (this == COMMON) return true;
        return entityClass != null && this.name.equalsIgnoreCase(entityClass.getName());
    }

    public boolean isCommonFallback() {
        return this == COMMON;
    }
}
