package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.Config;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContractData implements INBTSerializable<CompoundTag> {
    private UUID contractorUUID;
    private long contractStartTime; // Now using game time (ticks)
    private long totalContractDuration; // Duration in ticks
    private boolean isPermanent;

    // Track total contract time by player (in ticks)
    private Map<UUID, Long> totalContractTimeByPlayer = new HashMap<>();

    // Last time update (in game ticks)
    private long lastTimeUpdate = 0;

    // Convert config minutes to ticks (20 ticks per second, 60 seconds per minute)
    private static final long PERMANENT_CONTRACT_REQUIREMENT = Config.minutesUntilPermanent * 60 * 20L; // In ticks
    private static final long ONE_STAR_DURATION = (long) Config.minutesPerContract * 60 * 20L; // In ticks

    public ContractData() {
        this.contractorUUID = null;
        this.contractStartTime = 0;
        this.totalContractDuration = 0;
        this.isPermanent = false;
        this.totalContractTimeByPlayer = new HashMap<>();
        this.lastTimeUpdate = 0;
    }

    public ContractData(UUID contractorUUID, long contractStartTime, long duration) {
        this.contractorUUID = contractorUUID;
        this.contractStartTime = contractStartTime;
        this.totalContractDuration = duration;
        this.isPermanent = false;
        this.totalContractTimeByPlayer = new HashMap<>();
        this.lastTimeUpdate = contractStartTime;
    }

    // Constructor for permanent contracts
    public ContractData(UUID contractorUUID, boolean isPermanent, Level level) {
        this.contractorUUID = contractorUUID;
        this.contractStartTime = level.getGameTime();
        this.totalContractDuration = 0;
        this.isPermanent = isPermanent;
        this.totalContractTimeByPlayer = new HashMap<>();
        this.lastTimeUpdate = this.contractStartTime;
    }

    public static long getBaseDurationForStarLevel(int starLevel) {
        return ONE_STAR_DURATION;
    }

    public boolean hasActiveContract(Level level) {
        if (contractorUUID == null) return false;

        if (isPermanent) return true;

        long currentTime = level.getGameTime();
        return (currentTime - contractStartTime) < totalContractDuration;
    }

    public boolean isContractor(UUID playerUUID, Level level) {
        return hasActiveContract(level) && contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public void setContract(UUID playerUUID, int starLevel, Level level) {
        // Update time of previous contract if exists
        updateTotalContractTime(level);

        this.contractorUUID = playerUUID;
        this.contractStartTime = level.getGameTime();
        this.totalContractDuration = getBaseDurationForStarLevel(starLevel);
        this.isPermanent = false;
        this.lastTimeUpdate = this.contractStartTime;
    }

    public void setPermanentContract(UUID playerUUID, Level level) {
        // Update time before establishing permanent contract
        updateTotalContractTime(level);

        this.contractorUUID = playerUUID;
        this.contractStartTime = level.getGameTime();
        this.totalContractDuration = 0;
        this.isPermanent = true;
        this.lastTimeUpdate = this.contractStartTime;
    }

    public void extendContract(int starLevel) {
        if (!hasActiveContract(null)) {
            throw new IllegalStateException("Cannot extend a contract that is not active");
        }

        if (isPermanent) {
            throw new IllegalStateException("Cannot extend a permanent contract");
        }

        long additionalDuration = getBaseDurationForStarLevel(starLevel);
        this.totalContractDuration += additionalDuration;
    }

    public boolean canEstablishTemporaryContract(UUID playerUUID, Level level) {
        if (isPermanent) {
            return false;
        }

        if (!hasActiveContract(level)) {
            return true;
        }

        return contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public boolean canEstablishPermanentContract(UUID playerUUID, Level level) {
        if (isPermanent) {
            return contractorUUID != null && contractorUUID.equals(playerUUID);
        }

        if (!hasMinimumContractTime(playerUUID, level)) {
            return false;
        }

        if (!hasActiveContract(level)) {
            return true;
        }

        return contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public boolean trySetTemporaryContract(UUID playerUUID, int starLevel, Level level) {
        if (!canEstablishTemporaryContract(playerUUID, level)) {
            return false;
        }

        setContract(playerUUID, starLevel, level);
        return true;
    }

    public boolean trySetPermanentContract(UUID playerUUID, Level level) {
        if (!canEstablishPermanentContract(playerUUID, level)) {
            return false;
        }

        if (isPermanent && contractorUUID != null && contractorUUID.equals(playerUUID)) {
            return true;
        }

        setPermanentContract(playerUUID, level);
        return true;
    }

    public String getContractType(Level level) {
        if (!hasActiveContract(level)) {
            return "None";
        }
        return isPermanent ? "Permanent" : "Temporary";
    }

    public String getDetailedInfo(Level level) {
        if (!hasActiveContract(level)) {
            return "No active contract";
        }

        StringBuilder info = new StringBuilder();
        info.append("Type: ").append(getContractType(level));
        info.append(", Contractor: ").append(contractorUUID);

        if (!isPermanent) {
            info.append(", Remaining: ").append(getTimeDescription(level));
            info.append(", Duration: ").append(totalContractDuration / (60 * 20)).append(" minutes");
        }

        return info.toString();
    }

    public boolean renewContract(UUID playerUUID, int starLevel, Level level) {
        if (contractorUUID == null || !contractorUUID.equals(playerUUID)) {
            return false;
        }

        if (isPermanent) {
            return true;
        }

        if (hasActiveContract(level)) {
            extendContract(starLevel);
        } else {
            setContract(playerUUID, starLevel, level);
        }
        return true;
    }

    public boolean renewPermanentContract(UUID playerUUID, Level level) {
        if (contractorUUID == null || !contractorUUID.equals(playerUUID)) {
            return false;
        }

        if (isPermanent) {
            return true;
        }

        if (!hasMinimumContractTime(playerUUID, level)) {
            return false;
        }

        setPermanentContract(playerUUID, level);
        return true;
    }

    public void clearContract(Level level) {
        // Update total time before clearing
        updateTotalContractTime(level);

        this.contractorUUID = null;
        this.contractStartTime = 0;
        this.totalContractDuration = 0;
        this.isPermanent = false;
        this.lastTimeUpdate = 0;
    }

    /**
     * Updates the total contract time for the current contractor
     * Only updates if there's new time that hasn't been counted
     */
    private void updateTotalContractTime(Level level) {
        if (contractorUUID == null || isPermanent || level == null) {
            return;
        }

        long currentTime = level.getGameTime();
        long contractEndTime = contractStartTime + totalContractDuration;
        long actualEndTime = Math.min(currentTime, contractEndTime);

        // Calculate only new time since last update
        long timeToAdd = 0;
        if (lastTimeUpdate < actualEndTime) {
            timeToAdd = actualEndTime - Math.max(lastTimeUpdate, contractStartTime);
        }

        if (timeToAdd > 0) {
            long previousTime = totalContractTimeByPlayer.getOrDefault(contractorUUID, 0L);
            totalContractTimeByPlayer.put(contractorUUID, previousTime + timeToAdd);
            lastTimeUpdate = actualEndTime;
        }
    }

    public void periodicTimeUpdate(Level level) {
        if (contractorUUID != null && !isPermanent && hasActiveContract(level)) {
            updateTotalContractTime(level);
        }
    }

    public boolean hasMinimumContractTime(UUID playerUUID, Level level) {
        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Add time from current contract if it's the same player and not permanent
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract(level)) {
            long currentTime = level.getGameTime();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        return totalTime >= PERMANENT_CONTRACT_REQUIREMENT;
    }

    public int getTotalContractTimeMinutes(UUID playerUUID, Level level) {
        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Add time from current contract if applicable
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract(level)) {
            long currentTime = level.getGameTime();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        // Convert ticks to minutes (20 ticks per second, 60 seconds per minute)
        return (int) (totalTime / (60 * 20));
    }

    /**
     * Gets remaining minutes needed for permanent contracts
     */
    public int getRemainingMinutesForPermanent(UUID playerUUID, Level level) {
        if (hasMinimumContractTime(playerUUID, level)) {
            return 0;
        }

        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Add current contract time if applicable
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract(level)) {
            long currentTime = level.getGameTime();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        long remainingTime = PERMANENT_CONTRACT_REQUIREMENT - totalTime;
        return (int) (remainingTime / (60 * 20));
    }

    /**
     * Gets minimum required minutes for permanent contracts
     */
    public static int getMinimumRequiredMinutes() {
        return (int) (PERMANENT_CONTRACT_REQUIREMENT / (60 * 20));
    }

    public UUID getContractorUUID() {
        return contractorUUID;
    }

    public long getRemainingTime(Level level) {
        if (!hasActiveContract(level)) return 0;

        if (isPermanent) return Long.MAX_VALUE;

        long currentTime = level.getGameTime();
        long elapsed = currentTime - contractStartTime;
        return Math.max(0, totalContractDuration - elapsed);
    }

    public int getRemainingMinutes(Level level) {
        if (isPermanent) return Integer.MAX_VALUE;
        return (int) (getRemainingTime(level) / (60 * 20));
    }

    public int getRemainingSeconds(Level level) {
        if (isPermanent) return 0;
        return (int) ((getRemainingTime(level) % (60 * 20)) / 20);
    }

    public long getTotalContractDuration() {
        return totalContractDuration;
    }

    public long getElapsedTime(Level level) {
        if (contractorUUID == null) return 0;
        return level.getGameTime() - contractStartTime;
    }

    public int getAdditionalMinutesForStarLevel(int starLevel) {
        long additionalDuration = getBaseDurationForStarLevel(starLevel);
        return (int) (additionalDuration / (60 * 20));
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public String getTimeDescription(Level level) {
        if (isPermanent) {
            return "Permanent";
        } else {
            int minutes = getRemainingMinutes(level);
            int seconds = getRemainingSeconds(level);
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (contractorUUID != null) {
            tag.putString("contractor_uuid", contractorUUID.toString());
        }
        tag.putLong("contract_start_time", contractStartTime);
        tag.putLong("total_contract_duration", totalContractDuration);
        tag.putBoolean("is_permanent", isPermanent);
        tag.putLong("last_time_update", lastTimeUpdate);

        // Serialize total time by player
        CompoundTag playerTimesTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : totalContractTimeByPlayer.entrySet()) {
            playerTimesTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("total_contract_times", playerTimesTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("contractor_uuid")) {
            try {
                this.contractorUUID = UUID.fromString(tag.getString("contractor_uuid"));
            } catch (IllegalArgumentException e) {
                this.contractorUUID = null;
            }
        } else {
            this.contractorUUID = null;
        }
        this.contractStartTime = tag.getLong("contract_start_time");
        this.totalContractDuration = tag.getLong("total_contract_duration");
        this.isPermanent = tag.getBoolean("is_permanent");
        this.lastTimeUpdate = tag.getLong("last_time_update");

        if (this.totalContractDuration == 0 && this.contractorUUID != null && !this.isPermanent) {
            this.totalContractDuration = ONE_STAR_DURATION;
        }

        if (this.lastTimeUpdate == 0 && this.contractStartTime != 0) {
            this.lastTimeUpdate = this.contractStartTime;
        }

        // Deserialize total time by player
        this.totalContractTimeByPlayer = new HashMap<>();
        if (tag.contains("total_contract_times")) {
            CompoundTag playerTimesTag = tag.getCompound("total_contract_times");
            for (String uuidString : playerTimesTag.getAllKeys()) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    long time = playerTimesTag.getLong(uuidString);
                    totalContractTimeByPlayer.put(playerUUID, time);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid UUIDs
                }
            }
        }
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, ContractData> {

        @Override
        public ContractData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            ContractData data = new ContractData();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Override
        public @Nullable CompoundTag write(ContractData attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}