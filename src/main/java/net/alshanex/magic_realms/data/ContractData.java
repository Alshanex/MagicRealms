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
    private static final long CONTRACT_DURATION = 5 * 60 * 1000; // 5 minutos en millisegundos

    public ContractData() {
        this.contractorUUID = null;
        this.contractStartTime = 0;
    }

    public ContractData(UUID contractorUUID, long contractStartTime) {
        this.contractorUUID = contractorUUID;
        this.contractStartTime = contractStartTime;
    }

    public boolean hasActiveContract() {
        if (contractorUUID == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - contractStartTime) < CONTRACT_DURATION;
    }

    public boolean isContractor(UUID playerUUID) {
        return hasActiveContract() && contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public void setContract(UUID playerUUID) {
        this.contractorUUID = playerUUID;
        this.contractStartTime = System.currentTimeMillis();
    }

    public void clearContract() {
        this.contractorUUID = null;
        this.contractStartTime = 0;
    }

    public UUID getContractorUUID() {
        return contractorUUID;
    }

    public long getRemainingTime() {
        if (!hasActiveContract()) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - contractStartTime;
        return Math.max(0, CONTRACT_DURATION - elapsed);
    }

    public int getRemainingMinutes() {
        return (int) (getRemainingTime() / (60 * 1000));
    }

    public int getRemainingSeconds() {
        return (int) ((getRemainingTime() % (60 * 1000)) / 1000);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (contractorUUID != null) {
            tag.putString("contractor_uuid", contractorUUID.toString());
        }
        tag.putLong("contract_start_time", contractStartTime);
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
