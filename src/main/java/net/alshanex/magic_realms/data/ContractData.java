package net.alshanex.magic_realms.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.UUID;

public class ContractData implements INBTSerializable<CompoundTag> {
    private UUID contractorUUID;
    private long contractStartTime;
    private long totalContractDuration; // Duración total acumulada del contrato

    private static final long ONE_STAR_DURATION = 10 * 60 * 1000; // 10 minutos
    private static final long TWO_STAR_DURATION = 8 * 60 * 1000;  // 8 minutos
    private static final long THREE_STAR_DURATION = 5 * 60 * 1000; // 5 minutos

    public ContractData() {
        this.contractorUUID = null;
        this.contractStartTime = 0;
        this.totalContractDuration = 0;
    }

    public ContractData(UUID contractorUUID, long contractStartTime, long duration) {
        this.contractorUUID = contractorUUID;
        this.contractStartTime = contractStartTime;
        this.totalContractDuration = duration;
    }

    public static long getBaseDurationForStarLevel(int starLevel) {
        return switch (starLevel) {
            case 1 -> ONE_STAR_DURATION;
            case 2 -> TWO_STAR_DURATION;
            case 3 -> THREE_STAR_DURATION;
            default -> TWO_STAR_DURATION; // Valor por defecto
        };
    }

    public boolean hasActiveContract() {
        if (contractorUUID == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - contractStartTime) < totalContractDuration;
    }

    public boolean isContractor(UUID playerUUID) {
        return hasActiveContract() && contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public void setContract(UUID playerUUID, int starLevel) {
        this.contractorUUID = playerUUID;
        this.contractStartTime = System.currentTimeMillis();
        this.totalContractDuration = getBaseDurationForStarLevel(starLevel);
    }

    public void extendContract(int starLevel) {
        if (!hasActiveContract()) {
            throw new IllegalStateException("Cannot extend a contract that is not active");
        }

        long additionalDuration = getBaseDurationForStarLevel(starLevel);
        this.totalContractDuration += additionalDuration;
    }

    public boolean renewContract(UUID playerUUID, int starLevel) {
        if (contractorUUID == null || !contractorUUID.equals(playerUUID)) {
            return false; // No es el mismo contratista
        }

        if (hasActiveContract()) {
            extendContract(starLevel);
        } else {
            setContract(playerUUID, starLevel);
        }
        return true;
    }

    public void clearContract() {
        this.contractorUUID = null;
        this.contractStartTime = 0;
        this.totalContractDuration = 0;
    }

    public UUID getContractorUUID() {
        return contractorUUID;
    }

    public long getRemainingTime() {
        if (!hasActiveContract()) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - contractStartTime;
        return Math.max(0, totalContractDuration - elapsed);
    }

    public int getRemainingMinutes() {
        return (int) (getRemainingTime() / (60 * 1000));
    }

    public int getRemainingSeconds() {
        return (int) ((getRemainingTime() % (60 * 1000)) / 1000);
    }

    public long getTotalContractDuration() {
        return totalContractDuration;
    }

    public long getElapsedTime() {
        if (contractorUUID == null) return 0;
        return System.currentTimeMillis() - contractStartTime;
    }

    public int getAdditionalMinutesForStarLevel(int starLevel) {
        long additionalDuration = getBaseDurationForStarLevel(starLevel);
        return (int) (additionalDuration / (60 * 1000));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (contractorUUID != null) {
            tag.putString("contractor_uuid", contractorUUID.toString());
        }
        tag.putLong("contract_start_time", contractStartTime);
        tag.putLong("total_contract_duration", totalContractDuration);
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

        if (this.totalContractDuration == 0 && this.contractorUUID != null) {
            this.totalContractDuration = TWO_STAR_DURATION;
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
