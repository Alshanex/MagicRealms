package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.data.ContractedMercenaryEntry;

import java.util.List;

/**
 * Client copy of the local player's contracted mercenaries in their current dimension.
 */
public final class ClientContractedMercenaries {

    private static volatile List<ContractedMercenaryEntry> entries = List.of();

    private ClientContractedMercenaries() {}

    public static List<ContractedMercenaryEntry> get() {
        return entries;
    }

    public static void set(List<ContractedMercenaryEntry> list) {
        entries = List.copyOf(list);
    }

    /** Call on disconnect so a different world/server never shows stale mercenaries. */
    public static void clear() {
        entries = List.of();
    }
}