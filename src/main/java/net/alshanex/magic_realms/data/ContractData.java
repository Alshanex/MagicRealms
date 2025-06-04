package net.alshanex.magic_realms.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContractData implements INBTSerializable<CompoundTag> {
    private UUID contractorUUID;
    private long contractStartTime;
    private long totalContractDuration; // Duración total acumulada del contrato
    private boolean isPermanent; // Nuevo campo para contratos permanentes

    // Nuevo campo para trackear tiempo total de contratación por jugador
    private Map<UUID, Long> totalContractTimeByPlayer = new HashMap<>();

    // Para evitar doble contabilización, trackear la última vez que se actualizó el tiempo
    private long lastTimeUpdate = 0;

    // Requisito mínimo de tiempo para contratos permanentes (200 minutos en milisegundos)
    private static final long PERMANENT_CONTRACT_REQUIREMENT = 200 * 60 * 1000L; // 200 minutos

    private static final long ONE_STAR_DURATION = 10 * 60 * 1000; // 10 minutos
    private static final long TWO_STAR_DURATION = 8 * 60 * 1000;  // 8 minutos
    private static final long THREE_STAR_DURATION = 5 * 60 * 1000; // 5 minutos

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

    // Constructor para contratos permanentes
    public ContractData(UUID contractorUUID, boolean isPermanent) {
        this.contractorUUID = contractorUUID;
        this.contractStartTime = System.currentTimeMillis();
        this.totalContractDuration = 0; // No importa para contratos permanentes
        this.isPermanent = isPermanent;
        this.totalContractTimeByPlayer = new HashMap<>();
        this.lastTimeUpdate = this.contractStartTime;
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

        // Si es un contrato permanente, siempre está activo
        if (isPermanent) return true;

        long currentTime = System.currentTimeMillis();
        return (currentTime - contractStartTime) < totalContractDuration;
    }

    public boolean isContractor(UUID playerUUID) {
        return hasActiveContract() && contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public void setContract(UUID playerUUID, int starLevel) {
        // Actualizar tiempo del contrato anterior si existe
        updateTotalContractTime();

        this.contractorUUID = playerUUID;
        this.contractStartTime = System.currentTimeMillis();
        this.totalContractDuration = getBaseDurationForStarLevel(starLevel);
        this.isPermanent = false;
        this.lastTimeUpdate = this.contractStartTime;
    }

    // Nuevo método para establecer contrato permanente
    public void setPermanentContract(UUID playerUUID) {
        // Antes de establecer el contrato permanente, actualizar el tiempo total
        updateTotalContractTime();

        this.contractorUUID = playerUUID;
        this.contractStartTime = System.currentTimeMillis();
        this.totalContractDuration = 0; // No necesario para permanentes
        this.isPermanent = true;
        this.lastTimeUpdate = this.contractStartTime;
    }

    public void extendContract(int starLevel) {
        if (!hasActiveContract()) {
            throw new IllegalStateException("Cannot extend a contract that is not active");
        }

        // No se puede extender un contrato permanente
        if (isPermanent) {
            throw new IllegalStateException("Cannot extend a permanent contract");
        }

        long additionalDuration = getBaseDurationForStarLevel(starLevel);
        this.totalContractDuration += additionalDuration;
        // No actualizar lastTimeUpdate aquí porque no estamos cambiando el contrato actual
    }

    public boolean canEstablishTemporaryContract(UUID playerUUID) {
        // No se puede establecer un contrato temporal si ya hay uno permanente
        if (isPermanent) {
            return false;
        }

        // Si no hay contrato activo, se puede establecer
        if (!hasActiveContract()) {
            return true;
        }

        // Si hay contrato activo, solo el mismo jugador puede renovar
        return contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public boolean canEstablishPermanentContract(UUID playerUUID) {
        // Si ya es permanente, solo permitir al mismo jugador (aunque no haga nada)
        if (isPermanent) {
            return contractorUUID != null && contractorUUID.equals(playerUUID);
        }

        // Verificar si el jugador cumple con el requisito de tiempo mínimo
        if (!hasMinimumContractTime(playerUUID)) {
            return false;
        }

        // Si no hay contrato activo, se puede establecer
        if (!hasActiveContract()) {
            return true;
        }

        // Si hay contrato temporal activo, solo el mismo jugador puede convertir a permanente
        return contractorUUID != null && contractorUUID.equals(playerUUID);
    }

    public boolean trySetTemporaryContract(UUID playerUUID, int starLevel) {
        if (!canEstablishTemporaryContract(playerUUID)) {
            return false;
        }

        setContract(playerUUID, starLevel);
        return true;
    }

    public boolean trySetPermanentContract(UUID playerUUID) {
        if (!canEstablishPermanentContract(playerUUID)) {
            return false;
        }

        // Si ya es permanente del mismo jugador, no hacer nada pero retornar true
        if (isPermanent && contractorUUID != null && contractorUUID.equals(playerUUID)) {
            return true;
        }

        setPermanentContract(playerUUID);
        return true;
    }

    public String getContractType() {
        if (!hasActiveContract()) {
            return "None";
        }
        return isPermanent ? "Permanent" : "Temporary";
    }

    public String getDetailedInfo() {
        if (!hasActiveContract()) {
            return "No active contract";
        }

        StringBuilder info = new StringBuilder();
        info.append("Type: ").append(getContractType());
        info.append(", Contractor: ").append(contractorUUID);

        if (!isPermanent) {
            info.append(", Remaining: ").append(getTimeDescription());
            info.append(", Duration: ").append(totalContractDuration / (60 * 1000)).append(" minutes");
        }

        return info.toString();
    }

    public boolean renewContract(UUID playerUUID, int starLevel) {
        if (contractorUUID == null || !contractorUUID.equals(playerUUID)) {
            return false; // No es el mismo contratista
        }

        // Si ya es permanente, no hacer nada
        if (isPermanent) {
            return true;
        }

        if (hasActiveContract()) {
            extendContract(starLevel);
        } else {
            setContract(playerUUID, starLevel);
        }
        return true;
    }

    public boolean renewPermanentContract(UUID playerUUID) {
        if (contractorUUID == null || !contractorUUID.equals(playerUUID)) {
            return false;
        }

        // Si ya es permanente, no hacer nada
        if (isPermanent) {
            return true;
        }

        // Verificar requisito de tiempo antes de convertir
        if (!hasMinimumContractTime(playerUUID)) {
            return false;
        }

        // Convertir a permanente
        setPermanentContract(playerUUID);
        return true;
    }

    public void clearContract() {
        // Antes de limpiar, actualizar el tiempo total acumulado
        updateTotalContractTime();

        this.contractorUUID = null;
        this.contractStartTime = 0;
        this.totalContractDuration = 0;
        this.isPermanent = false;
        this.lastTimeUpdate = 0;
    }

    // Nuevos métodos para manejar tiempo total de contratación

    /**
     * Actualiza el tiempo total de contratación para el contratista actual
     * Solo actualiza si hay tiempo nuevo que no ha sido contabilizado
     */
    private void updateTotalContractTime() {
        if (contractorUUID == null || isPermanent) {
            return; // No actualizar para contratos permanentes o sin contratista
        }

        long currentTime = System.currentTimeMillis();
        long contractEndTime = contractStartTime + totalContractDuration;
        long actualEndTime = Math.min(currentTime, contractEndTime);

        // Calcular solo el tiempo nuevo desde la última actualización
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

    public void periodicTimeUpdate() {
        if (contractorUUID != null && !isPermanent && hasActiveContract()) {
            updateTotalContractTime();
        }
    }

    public boolean hasMinimumContractTime(UUID playerUUID) {
        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Solo añadir tiempo del contrato actual si es el mismo jugador y no es permanente
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract()) {
            long currentTime = System.currentTimeMillis();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            // Calcular tiempo del contrato actual SIN actualizar lastTimeUpdate
            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        return totalTime >= PERMANENT_CONTRACT_REQUIREMENT;
    }

    public int getTotalContractTimeMinutes(UUID playerUUID) {
        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Solo añadir tiempo del contrato actual si es el mismo jugador y no es permanente
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract()) {
            long currentTime = System.currentTimeMillis();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            // Calcular tiempo del contrato actual SIN actualizar lastTimeUpdate
            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        return (int) (totalTime / (60 * 1000));
    }

    /**
     * Obtiene los minutos restantes que necesita un jugador para poder usar contratos permanentes
     */
    public int getRemainingMinutesForPermanent(UUID playerUUID) {
        if (hasMinimumContractTime(playerUUID)) {
            return 0;
        }

        long totalTime = totalContractTimeByPlayer.getOrDefault(playerUUID, 0L);

        // Añadir tiempo del contrato actual si aplica
        if (contractorUUID != null && contractorUUID.equals(playerUUID) && !isPermanent && hasActiveContract()) {
            long currentTime = System.currentTimeMillis();
            long contractEndTime = contractStartTime + totalContractDuration;
            long actualEndTime = Math.min(currentTime, contractEndTime);

            long currentContractTime = Math.max(0, actualEndTime - Math.max(lastTimeUpdate, contractStartTime));
            totalTime += currentContractTime;
        }

        long remainingTime = PERMANENT_CONTRACT_REQUIREMENT - totalTime;
        return (int) (remainingTime / (60 * 1000));
    }

    /**
     * Obtiene el requisito mínimo en minutos para contratos permanentes
     */
    public static int getMinimumRequiredMinutes() {
        return (int) (PERMANENT_CONTRACT_REQUIREMENT / (60 * 1000));
    }

    public UUID getContractorUUID() {
        return contractorUUID;
    }

    public long getRemainingTime() {
        if (!hasActiveContract()) return 0;

        if (isPermanent) return Long.MAX_VALUE;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - contractStartTime;
        return Math.max(0, totalContractDuration - elapsed);
    }

    public int getRemainingMinutes() {
        if (isPermanent) return Integer.MAX_VALUE;
        return (int) (getRemainingTime() / (60 * 1000));
    }

    public int getRemainingSeconds() {
        if (isPermanent) return 0;
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

    public boolean isPermanent() {
        return isPermanent;
    }

    public String getTimeDescription() {
        if (isPermanent) {
            return "Permanent";
        } else {
            int minutes = getRemainingMinutes();
            int seconds = getRemainingSeconds();
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        // Actualizar tiempo antes de guardar
        updateTotalContractTime();

        CompoundTag tag = new CompoundTag();
        if (contractorUUID != null) {
            tag.putString("contractor_uuid", contractorUUID.toString());
        }
        tag.putLong("contract_start_time", contractStartTime);
        tag.putLong("total_contract_duration", totalContractDuration);
        tag.putBoolean("is_permanent", isPermanent);
        tag.putLong("last_time_update", lastTimeUpdate);

        // Serializar tiempo total por jugador
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
            this.totalContractDuration = TWO_STAR_DURATION;
        }

        // Si lastTimeUpdate no estaba guardado (versiones anteriores), inicializar
        if (this.lastTimeUpdate == 0 && this.contractStartTime != 0) {
            this.lastTimeUpdate = this.contractStartTime;
        }

        // Deserializar tiempo total por jugador
        this.totalContractTimeByPlayer = new HashMap<>();
        if (tag.contains("total_contract_times")) {
            CompoundTag playerTimesTag = tag.getCompound("total_contract_times");
            for (String uuidString : playerTimesTag.getAllKeys()) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    long time = playerTimesTag.getLong(uuidString);
                    totalContractTimeByPlayer.put(playerUUID, time);
                } catch (IllegalArgumentException e) {
                    // Ignorar UUIDs inválidos
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