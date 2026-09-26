package net.alshanex.magic_realms.entity.humans.client;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class HeldItemPosers {

    private static final List<HeldItemPoser> POSERS = new ArrayList<>();

    private HeldItemPosers() {}

    public static void register(HeldItemPoser poser) { POSERS.add(poser); }

    public static boolean isEmpty() { return POSERS.isEmpty(); }

    @Nullable
    public static HeldItemPoser find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (HeldItemPoser p : POSERS) {
            if (p.appliesTo(stack)) return p;
        }
        return null;
    }
}
