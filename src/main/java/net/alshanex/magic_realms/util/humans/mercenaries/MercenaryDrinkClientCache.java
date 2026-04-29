package net.alshanex.magic_realms.util.humans.mercenaries;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MercenaryDrinkClientCache {

    private static final Map<UUID, ItemStack> ENTRIES = new ConcurrentHashMap<>();

    private MercenaryDrinkClientCache() {}

    public static void set(UUID mercenaryUUID, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            ENTRIES.remove(mercenaryUUID);
        } else {
            ENTRIES.put(mercenaryUUID, stack);
        }
    }

    public static ItemStack get(UUID mercenaryUUID) {
        ItemStack stack = ENTRIES.get(mercenaryUUID);
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
