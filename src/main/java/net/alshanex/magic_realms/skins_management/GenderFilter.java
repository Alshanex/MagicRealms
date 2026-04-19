package net.alshanex.magic_realms.skins_management;

import com.mojang.serialization.Codec;
import net.alshanex.magic_realms.util.humans.mercenaries.Gender;
import net.minecraft.util.StringRepresentable;

public enum GenderFilter implements StringRepresentable {
    ANY("any"),
    MALE("male"),
    FEMALE("female");

    public static final Codec<GenderFilter> CODEC = StringRepresentable.fromEnum(GenderFilter::values);

    private final String name;

    GenderFilter(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean matches(Gender gender) {
        if (this == ANY) return true;
        return gender != null && this.name.equalsIgnoreCase(gender.getName());
    }

    public static GenderFilter fromGender(Gender gender) {
        if (gender == null) return ANY;
        for (GenderFilter f : values()) {
            if (f != ANY && f.name.equalsIgnoreCase(gender.getName())) return f;
        }
        return ANY;
    }
}
