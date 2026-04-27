package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SkinCategory implements StringRepresentable {
    SKIN("skin"),
    EYES("eyes"),
    HAIR("hair"),
    CLOTHES("clothes");

    public static final Codec<SkinCategory> CODEC = StringRepresentable.fromEnum(SkinCategory::values);

    private final String name;

    SkinCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
