package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ContractUtils {
    public static void handlePermanentContractCreation(Player player,
                                                       AbstractMercenaryEntity humanEntity,
                                                       ContractData contractData,
                                                       ItemStack heldItem) {

        if (!contractData.hasMinimumContractTime(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                int remainingMinutes = contractData.getRemainingMinutesForPermanent(player.getUUID());

                MutableComponent message = Component.translatable("ui.magic_realms.permanent_contract_insufficient_time",
                        humanEntity.getEntityName(), remainingMinutes);

                serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        if (!contractData.canEstablishPermanentContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                message = Component.translatable("ui.magic_realms.already_have_contract");
                serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        if (contractData.isPermanent() && contractData.isContractor(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_already_permanent",
                        humanEntity.getEntityName());
                serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        boolean isUpgrade = contractData.hasActiveContract() && contractData.isContractor(player.getUUID());

        boolean success = contractData.trySetPermanentContract(player.getUUID());

        if (!success) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_failed");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            return;
        }

        if (!isUpgrade) {
            humanEntity.setSummoner(player);
        }

        humanEntity.updateCustomNameWithStars();

        if (player instanceof ServerPlayer serverPlayer) {
            MutableComponent message;
            message = Component.translatable("ui.magic_realms.contract_established_permanent", humanEntity.getEntityName());

            serverPlayer.sendSystemMessage(message);
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
    }

    public static void handleTieredContractCreation(Player player,
                                                    AbstractMercenaryEntity humanEntity,
                                                    ContractData contractData,
                                                    ItemStack heldItem,
                                                    TieredContractItem contractItem) {

        if (!contractData.canEstablishTemporaryContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                if (contractData.isPermanent()) {
                    if (contractData.isContractor(player.getUUID())) {
                        message = Component.translatable("ui.magic_realms.contract_already_permanent",
                                humanEntity.getEntityName());
                    } else {
                        message = Component.translatable("ui.magic_realms.already_have_contract",
                                humanEntity.getEntityName());
                    }
                } else {
                    message = Component.translatable("ui.magic_realms.already_have_contract",
                            humanEntity.getEntityName());
                }
                serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        // Obtener el nivel de la entidad
        KillTrackerData killTracker = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
        int entityLevel = killTracker.getCurrentLevel();
        ContractTier contractTier = contractItem.getTier();

        // Verificar si el contrato es apropiado para el nivel de la entidad
        if (!contractTier.canContractLevel(entityLevel)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                message = Component.translatable("ui.magic_realms.entity_level_too_low",
                        humanEntity.getEntityName());

                serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        int starLevel = humanEntity.getStarLevel();
        int additionalMinutes = contractData.getAdditionalMinutesForStarLevel(starLevel);
        boolean isRenewal = contractData.isContractor(player.getUUID());

        boolean success;
        if (isRenewal) {
            success = contractData.renewContract(player.getUUID(), starLevel);
            if(success){
                humanEntity.addEmeralds(additionalMinutes);
            }
        } else {
            success = contractData.trySetTemporaryContract(player.getUUID(), starLevel);
            if (success) {
                humanEntity.addEmeralds(additionalMinutes);
                humanEntity.setSummoner(player);
            }
        }

        if (!success) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_failed");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MutableComponent message;
            if (isRenewal) {
                message = Component.translatable("ui.magic_realms.contract_extended",
                        humanEntity.getEntityName(),
                        additionalMinutes);
            } else {
                message = Component.translatable("ui.magic_realms.contract_established",
                        humanEntity.getEntityName(),
                        additionalMinutes);
            }
            serverPlayer.sendSystemMessage(message);
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
    }

    public static void handleContractInteraction(Player player,
                                                 AbstractMercenaryEntity humanEntity,
                                                 ContractData contractData) {

        if (!contractData.isContractor(player.getUUID())) {
            if (contractData.hasActiveContract()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    MutableComponent message = Component.translatable("ui.magic_realms.already_have_contract",
                            humanEntity.getEntityName());
                    serverPlayer.sendSystemMessage(message);
                }
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    sendIntroductionMessage(serverPlayer, humanEntity, contractData);
                }
            }
            return;
        }

        if (player.isShiftKeyDown()) {
            boolean currentPatrolState = humanEntity.isPatrolMode();

            if (currentPatrolState) {
                humanEntity.setPatrolMode(false);

                if (player instanceof ServerPlayer serverPlayer) {
                    MutableComponent message = Component.translatable("ui.magic_realms.patrol_following");
                    message = message.withStyle(ChatFormatting.YELLOW);
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
                }
            } else {
                humanEntity.setPatrolMode(true);

                if (player instanceof ServerPlayer serverPlayer) {
                    MutableComponent message = Component.translatable("ui.magic_realms.patrol_active");
                    message = message.withStyle(ChatFormatting.YELLOW);
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
                }
            }
            return;
        }

        if (contractData.isPermanent()) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_time_permanent",
                        humanEntity.getEntityName());
                serverPlayer.sendSystemMessage(message);
            }
        } else {
            int minutes = contractData.getRemainingMinutes();
            int seconds = contractData.getRemainingSeconds();

            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_time_remaining_with_extension",
                        humanEntity.getEntityName(), minutes, seconds);
                serverPlayer.sendSystemMessage(message);
            }
        }

        if (humanEntity.isRemoved() || !humanEntity.isAlive()) {
            MagicRealms.LOGGER.warn("Attempted to open menu for removed/dead entity: {}", humanEntity.getEntityName());
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.literal("Entity is no longer available");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            return;
        }

        EntitySnapshot snapshot = EntitySnapshot.fromEntity(humanEntity);

        humanEntity.setMenuState(true);

        player.openMenu(new ContractMenuProvider(snapshot, humanEntity), buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(humanEntity.getUUID());
        });
    }

    public static void sendIntroductionMessage(ServerPlayer serverPlayer, AbstractMercenaryEntity humanEntity, ContractData contractData) {
        String entityName = humanEntity.getEntityName();
        EntityClass entityClass = humanEntity.getEntityClass();

        KillTrackerData killTracker = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
        int entityLevel = killTracker.getCurrentLevel();
        ContractTier requiredTier = ContractTier.getRequiredTierForLevel(entityLevel);
        int contractMinutes = contractData.getAdditionalMinutesForStarLevel(humanEntity.getStarLevel());

        String messageKey;
        switch (entityClass) {
            case WARRIOR -> messageKey = "ui.magic_realms.introduction.warrior";
            case ROGUE -> {
                if (humanEntity.isArcher()) {
                    messageKey = "ui.magic_realms.introduction.archer";
                } else {
                    messageKey = "ui.magic_realms.introduction.assassin";
                }
            }
            case MAGE -> messageKey = "ui.magic_realms.introduction.mage";
            default -> messageKey = "ui.magic_realms.introduction.default";
        }

        MutableComponent message = Component.translatable(messageKey,
                entityName,
                requiredTier.getDisplayName().getString(),
                contractMinutes);

        serverPlayer.sendSystemMessage(message);
    }

    private static class ContractMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final AbstractMercenaryEntity entity;
        private final EntityType<? extends AbstractMercenaryEntity> entityType; // NEW

        public ContractMenuProvider(EntitySnapshot snapshot, AbstractMercenaryEntity entity) {
            this.snapshot = snapshot;
            this.entity = entity;
            this.entityType = entity != null ?
                    (EntityType<? extends AbstractMercenaryEntity>) entity.getType() :
                    (snapshot != null ? snapshot.entityType : null);
        }

        @Override
        public Component getDisplayName() {
            String starDisplay = "★".repeat(entity != null ? entity.getStarLevel() :
                    (snapshot != null ? snapshot.starLevel : 1));
            MutableComponent title = Component.translatable("gui.magic_realms.human_info.title");
            String entityName = entity != null ? entity.getEntityName() :
                    (snapshot != null ? snapshot.entityName : "Unknown");
            MutableComponent entityInfo = Component.literal(starDisplay + " " + entityName);
            entityInfo = entityInfo.withStyle(ChatFormatting.AQUA);

            return Component.literal(title.getString() + " - " + entityInfo.getString());
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            // UPDATED: Use new constructor with entity type support
            return new ContractHumanInfoMenu(containerId, playerInventory, snapshot, entity, entityType);
        }
    }

    // Convenience method for opening contract screen with any entity type
    public static void openContractScreen(Player player, AbstractMercenaryEntity entity) {
        if (player.level().isClientSide()) return;

        EntitySnapshot snapshot = EntitySnapshot.fromEntity(entity);
        entity.setMenuState(true);

        player.openMenu(new ContractMenuProvider(snapshot, entity), buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(entity.getUUID());
        });
    }

    // NEW: Method for opening screen from snapshot only (useful for client-side or when entity unavailable)
    public static void openContractScreenFromSnapshot(Player player, EntitySnapshot snapshot) {
        if (player.level().isClientSide()) return;

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                String starDisplay = "★".repeat(snapshot.starLevel);
                MutableComponent title = Component.translatable("gui.magic_realms.human_info.title");
                MutableComponent entityInfo = Component.literal(starDisplay + " " + snapshot.entityName);
                entityInfo = entityInfo.withStyle(ChatFormatting.AQUA);
                return Component.literal(title.getString() + " - " + entityInfo.getString());
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ContractHumanInfoMenu(containerId, playerInventory, snapshot);
            }
        }, buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(snapshot.entityUUID);
        });
    }
}