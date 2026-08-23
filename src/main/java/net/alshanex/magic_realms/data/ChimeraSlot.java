package net.alshanex.magic_realms.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Represents one of the 6 body-part slots that make up a Chimera.
 */
public enum ChimeraSlot implements StringRepresentable {
    HEAD("head"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm"),
    TORSO("torso"),
    LEFT_LEG("left_leg"),
    RIGHT_LEG("right_leg");

    public static final Codec<ChimeraSlot> CODEC = StringRepresentable.fromEnum(ChimeraSlot::values);

    private final String name;

    ChimeraSlot(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static ChimeraSlot fromName(String name) {
        for (ChimeraSlot slot : values()) {
            if (slot.name.equals(name)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Unknown chimera slot: " + name);
    }

    /**
     * Whether this slot contributes to movement speed.
     */
    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    /**
     * Whether this slot is the "main arm" (right arm by default).
     */
    public boolean isMainArm() {
        return this == RIGHT_ARM;
    }

    /**
     * Whether this slot is the torso.
     */
    public boolean isTorso() {
        return this == TORSO;
    }
}
