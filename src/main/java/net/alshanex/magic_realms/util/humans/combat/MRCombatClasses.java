package net.alshanex.magic_realms.util.humans.combat;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;

public final class MRCombatClasses {

    private MRCombatClasses() {}

    public static final ResourceLocation MAGE = id("mage");
    public static final ResourceLocation WARRIOR = id("warrior");
    public static final ResourceLocation ROGUE = id("rogue");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, path);
    }
}
