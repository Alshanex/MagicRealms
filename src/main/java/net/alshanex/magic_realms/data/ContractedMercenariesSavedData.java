package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Contracted mercenaries currently living in <b>this level</b>, grouped by contractor.
 *
 * <p>One instance per dimension (each level has its own data storage). A player only ever sees and teleports the
 * mercenaries in the level they are standing in. When a mercenary changes dimension, the old level drops it and the new
 * level picks it up on join.
 */
public class ContractedMercenariesSavedData extends SavedData {

    private static final String NAME = MagicRealms.MODID + "_contracted_mercenaries";

    private static final SavedData.Factory<ContractedMercenariesSavedData> FACTORY =
            new SavedData.Factory<>(ContractedMercenariesSavedData::new, ContractedMercenariesSavedData::load, null);

    /** player -> (mercenary -> entry), insertion-ordered so the screen lists mercs in contract order. */
    private final Map<UUID, LinkedHashMap<UUID, ContractedMercenaryEntry>> byPlayer = new HashMap<>();
    /** mercenary -> player. */
    private final Map<UUID, UUID> contractorOf = new HashMap<>();
    /** Players whose visible list changed since the last flush. Not persisted. */
    private final Set<UUID> syncDirty = new HashSet<>();

    public static ContractedMercenariesSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    // Mutation

    /** Insert or update. Moves the entry if the mercenary now belongs to a different player. */
    public void upsert(UUID playerId, ContractedMercenaryEntry fresh) {
        UUID mercId = fresh.uuid();

        UUID previousOwner = contractorOf.put(mercId, playerId);
        if (previousOwner != null && !previousOwner.equals(playerId)) {
            removeFromPlayerMap(previousOwner, mercId);
            syncDirty.add(previousOwner);
        }

        ContractedMercenaryEntry old = byPlayer.computeIfAbsent(playerId, k -> new LinkedHashMap<>()).put(mercId, fresh);

        if (old == null || !old.sameDisplayAs(fresh)) {
            syncDirty.add(playerId);
            setDirty();
        } else if (!old.lastPos().equals(fresh.lastPos())) {
            setDirty(); // persist the new position; not worth a client sync
        }
    }

    public boolean removeMercenary(UUID mercId) {
        UUID owner = contractorOf.remove(mercId);
        if (owner == null) return false;
        removeFromPlayerMap(owner, mercId);
        syncDirty.add(owner);
        setDirty();
        return true;
    }

    public void pruneExpired(long gameTime) {
        List<UUID> expired = new ArrayList<>();
        for (Map<UUID, ContractedMercenaryEntry> map : byPlayer.values()) {
            for (ContractedMercenaryEntry e : map.values()) {
                if (e.isExpired(gameTime)) expired.add(e.uuid());
            }
        }
        for (UUID id : expired) removeMercenary(id);
    }

    private void removeFromPlayerMap(UUID playerId, UUID mercId) {
        Map<UUID, ContractedMercenaryEntry> map = byPlayer.get(playerId);
        if (map == null) return;
        map.remove(mercId);
        if (map.isEmpty()) byPlayer.remove(playerId);
    }

    // Queries

    @Nullable
    public ContractedMercenaryEntry getEntry(UUID playerId, UUID mercId) {
        Map<UUID, ContractedMercenaryEntry> map = byPlayer.get(playerId);
        return map == null ? null : map.get(mercId);
    }

    public List<ContractedMercenaryEntry> entriesFor(UUID playerId) {
        Map<UUID, ContractedMercenaryEntry> map = byPlayer.get(playerId);
        return map == null ? List.of() : List.copyOf(map.values());
    }

    /** Contracts this player holds in this level, ignoring ones that expired but haven't been pruned yet. */
    public int countActiveFor(UUID playerId, long gameTime) {
        Map<UUID, ContractedMercenaryEntry> map = byPlayer.get(playerId);
        if (map == null) return 0;
        int count = 0;
        for (ContractedMercenaryEntry e : map.values()) {
            if (!e.isExpired(gameTime)) count++;
        }
        return count;
    }

    /** Returns and clears the players whose list changed. */
    public Set<UUID> drainSyncDirty() {
        if (syncDirty.isEmpty()) return Set.of();
        Set<UUID> copy = Set.copyOf(syncDirty);
        syncDirty.clear();
        return copy;
    }

    // Persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();

        for (Map.Entry<UUID, LinkedHashMap<UUID, ContractedMercenaryEntry>> playerEntry : byPlayer.entrySet()) {
            for (ContractedMercenaryEntry e : playerEntry.getValue().values()) {
                ContractedMercenaryEntry.CODEC.encodeStart(ops, e)
                        .resultOrPartial(err -> MagicRealms.LOGGER.warn("Failed to save contracted mercenary {}: {}", e.uuid(), err))
                        .ifPresent(encoded -> {
                            CompoundTag row = new CompoundTag();
                            row.put("player", NbtUtils.createUUID(playerEntry.getKey()));
                            row.put("entry", encoded);
                            list.add(row);
                        });
            }
        }

        tag.put("entries", list);
        return tag;
    }

    public static ContractedMercenariesSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ContractedMercenariesSavedData data = new ContractedMercenariesSavedData();
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);

        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!row.contains("player") || !row.contains("entry")) continue;
            UUID playerId = NbtUtils.loadUUID(row.get("player"));

            ContractedMercenaryEntry.CODEC.parse(ops, row.get("entry"))
                    .resultOrPartial(err -> MagicRealms.LOGGER.warn("Skipping unreadable contracted mercenary entry: {}", err))
                    .ifPresent(e -> {
                        data.byPlayer.computeIfAbsent(playerId, k -> new LinkedHashMap<>()).put(e.uuid(), e);
                        data.contractorOf.put(e.uuid(), playerId);
                    });
        }
        return data;
    }
}