package net.alshanex.magic_realms.raid;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

public class CustomRaidManager extends SavedData {
    private static final String FILE_ID = "magic_realms_raids";
    private final Map<Integer, CustomRaid> raidMap = Maps.newHashMap();
    private final ServerLevel level;
    private int nextAvailableID = 0;
    private int tick = 0;
    private final List<Integer> raidsToRemove = new ArrayList<>();

    public CustomRaidManager(ServerLevel level) {
        this.level = level;
        this.setDirty();
    }

    public CustomRaidManager(ServerLevel level, CompoundTag compound, HolderLookup.Provider registries) {
        this.level = level;
        this.nextAvailableID = compound.getInt("NextAvailableID");
        ListTag raids = compound.getList("Raids", Tag.TAG_COMPOUND);

        for (Tag tag : raids) {
            CompoundTag raidTag = (CompoundTag) tag;
            CustomRaid raid = new CustomRaid(level, raidTag);
            this.raidMap.put(raid.getId(), raid);
        }
    }

    public static CustomRaidManager load(CompoundTag compound, HolderLookup.Provider registries, ServerLevel level) {
        return new CustomRaidManager(level, compound, registries);
    }

    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries) {
        compound.putInt("NextAvailableID", this.nextAvailableID);
        ListTag raids = new ListTag();

        for (CustomRaid raid : this.raidMap.values()) {
            CompoundTag raidTag = new CompoundTag();
            raid.save(raidTag);
            raids.add(raidTag);
        }

        compound.put("Raids", raids);
        return compound;
    }

    public static SavedData.Factory<CustomRaidManager> factory(ServerLevel level) {
        return new SavedData.Factory<>(
                () -> new CustomRaidManager(level),
                (compound, registries) -> load(compound, registries, level),
                null
        );
    }

    @Nullable
    public CustomRaid getRaidAt(BlockPos pos, Player player) {
        for (CustomRaid raid : raidMap.values()) {
            if (raid.isActive() && raid.getTargetPlayerUUID().equals(player.getUUID())) {
                return raid;
            }
        }
        return null;
    }

    @Nullable
    public CustomRaid getActiveRaidForPlayer(UUID playerUUID) {
        for (CustomRaid raid : raidMap.values()) {
            if (raid.isActive() && raid.getTargetPlayerUUID().equals(playerUUID)) {
                return raid;
            }
        }
        return null;
    }

    public CustomRaid createRaid(ServerPlayer player, BlockPos pos, int waves ) {
        CustomRaid existingRaid = getActiveRaidForPlayer(player.getUUID());
        if (existingRaid != null) {
            return existingRaid;
        }

        CustomRaid raid = new CustomRaid(nextAvailableID++, level, pos, player, waves);
        raidMap.put(raid.getId(), raid);
        this.setDirty();
        return raid;
    }

    public void tick() {
        tick++;
        raidsToRemove.clear();

        for (CustomRaid raid : raidMap.values()) {
            if (raid.isActive()) {
                raid.tick();

                if (raid.isOver()) {
                    if (tick % 100 == 0) {
                        raidsToRemove.add(raid.getId());
                    }
                }
            } else {
                raidsToRemove.add(raid.getId());
            }
        }

        for (Integer id : raidsToRemove) {
            CustomRaid raid = raidMap.remove(id);
            if (raid != null) {
                raid.stop();
            }
        }

        if (!raidsToRemove.isEmpty()) {
            this.setDirty();
        }
    }

    public Collection<CustomRaid> getAllRaids() {
        return raidMap.values();
    }

    public void stopAllRaids() {
        for (CustomRaid raid : raidMap.values()) {
            raid.stop();
        }
        raidMap.clear();
        this.setDirty();
    }
}
