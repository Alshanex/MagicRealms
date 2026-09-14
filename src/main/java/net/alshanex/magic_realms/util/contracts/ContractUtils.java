package net.alshanex.magic_realms.util.contracts;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.IExclusiveMercenary;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.screens.ContractInventoryMenu;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.alshanex.magic_realms.util.humans.mercenaries.EntitySnapshot;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenarySpeechHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueManager;

import javax.annotation.Nullable;

public class ContractUtils {
    public static void handlePermanentContractCreation(Player player,
                                                       AbstractMercenaryEntity humanEntity,
                                                       ContractData contractData,
                                                       ItemStack heldItem) {

        Level level = humanEntity.level();  // Get the level from the entity

        if (contractData.hasActiveContract(level) && !contractData.isContractor(player.getUUID(), level)) {
            if (player instanceof ServerPlayer serverPlayer) {
                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_already_contract"), humanEntity);
                //MutableComponent message;
                //message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.already_have_contract");
                //serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        if (contractData.isPermanent() && contractData.isContractor(player.getUUID(), level)) {
            if (player instanceof ServerPlayer serverPlayer) {
                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_already_permanent"), humanEntity);
                //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.contract_already_permanent");
                //serverPlayer.sendSystemMessage(message);
            }
            return;
        }

        if(!player.getAbilities().instabuild){
            if (!contractData.hasMinimumContractTime(player.getUUID(), level)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    //int remainingMinutes = contractData.getRemainingMinutesForPermanent(player.getUUID(), level);

                    //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity,"ui.magic_realms.permanent_contract_insufficient_time", remainingMinutes);

                    //serverPlayer.sendSystemMessage(message);

                    DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "minutes_until_permanent"), humanEntity);
                }
                return;
            }
        }

        boolean isUpgrade = contractData.hasActiveContract(level) && contractData.isContractor(player.getUUID(), level);

        boolean success = contractData.trySetPermanentContract(player.getUUID(), level, player.getAbilities().instabuild);

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

        humanEntity.refreshDisplayName();

        if (player instanceof ServerPlayer serverPlayer) {
            //MutableComponent message;
            //message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.contract_established_permanent");
            //serverPlayer.sendSystemMessage(message);

            DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "contract_already_permanent"), humanEntity);
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
    }

    public static void handleTemporaryContractCreation(Player player,
                                                       AbstractMercenaryEntity humanEntity,
                                                       ContractData contractData,
                                                       ItemStack heldItem) {

        Level level = humanEntity.level();

        // Someone else already holds this mercenary's contract.
        if (contractData.hasActiveContract(level) && !contractData.isContractor(player.getUUID(), level)) {
            if (player instanceof ServerPlayer serverPlayer) {
                //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.already_have_contract");
                //serverPlayer.sendSystemMessage(message);
                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_already_contract"), humanEntity);
            }
            return;
        }

        // Already permanently bound to this player — a temporary contract would be a downgrade.
        if (contractData.isPermanent() && contractData.isContractor(player.getUUID(), level)) {
            if (player instanceof ServerPlayer serverPlayer) {
                //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.contract_already_permanent");
                //serverPlayer.sendSystemMessage(message);
                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "contract_already_permanent"), humanEntity);
            }
            return;
        }

        // No level gate any more: if the mercenary is free, anyone can hire them.
        int contractMinutes = contractData.getContractMinutes();
        boolean isRenewal = contractData.isContractor(player.getUUID(), level);

        boolean success;
        if (isRenewal) {
            success = contractData.renewContract(player.getUUID(), level);
        } else {
            success = contractData.trySetTemporaryContract(player.getUUID(), level);
            if (success) {
                humanEntity.setSummoner(player);
            }
        }

        if (!success) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_failed")
                        .withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            return;
        }

        humanEntity.refreshDisplayName();

        if (player instanceof ServerPlayer serverPlayer) {
            /*
            String key = isRenewal
                    ? "ui.magic_realms.contract_extended"
                    : "ui.magic_realms.contract_established";
            serverPlayer.sendSystemMessage(MercenaryMessageFormatter.buildFor(humanEntity, key, contractMinutes));
            */
            ResourceLocation dialogue = isRenewal
                    ? ResourceLocation.fromNamespaceAndPath("magic_realms", "contract_extended")
                    : ResourceLocation.fromNamespaceAndPath("magic_realms", "contract_established");

            DialogueManager.open(serverPlayer, dialogue, humanEntity);
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
    }

    /**
     * Handle a "bare hands" right-click (no contract item, no food, no hell pass) on a mercenary.
     *
     * <p>Behavior depends on whether the player is the active contractor:
     * <ul>
     *     <li>Not the contractor + no active contract: send introduction message.</li>
     *     <li>Not the contractor + someone else holds the contract: refuse politely.</li>
     *     <li>Is the contractor + plain right-click: pick a random speech line from the mercenary's pool.</li>
     *     <li>Is the contractor + shift held: open the contract menu.</li>
     * </ul>
     */
    public static void handleContractInteraction(Player player,
                                                 AbstractMercenaryEntity humanEntity,
                                                 ContractData contractData) {

        Level level = humanEntity.level();  // Get the level from the entity

        if (!contractData.isContractor(player.getUUID(), level)) {
            if (contractData.hasActiveContract(level)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.already_have_contract");
                    //serverPlayer.sendSystemMessage(message);

                    DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_already_contract"), humanEntity);
                }
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    sendIntroductionMessage(serverPlayer, humanEntity, humanEntity.getEntityClass());
                }
            }
            return;
        }

        // From here on the player IS the contractor.

        // Plain right-click: speech line.
        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                MercenarySpeechHelper.trySpeak(humanEntity, serverPlayer);
            }
            return;
        }
/*
        // Shift + right-click: open the contract menu (and tell the player how much time is left).
        if (contractData.isPermanent()) {
            if (player instanceof ServerPlayer serverPlayer) {
                //MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity, "ui.magic_realms.contract_time_permanent");
                //serverPlayer.sendSystemMessage(message);

                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "contract_permanent"), humanEntity);
            }
        } else {
            int minutes = contractData.getRemainingMinutes(level);
            String seconds = contractData.getRemainingSecondsFormatted(level);

            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = MercenaryMessageFormatter.buildFor(humanEntity,
                        "ui.magic_realms.contract_time_remaining_with_extension", minutes, seconds);
                serverPlayer.sendSystemMessage(message);
            }
        }
*/
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
            buf.writeBoolean(humanEntity.isPatrolMode());
        });
    }

    public static void openAttributesScreen(Player player, AbstractMercenaryEntity entity) {
        if (player.level().isClientSide()) return;

        EntitySnapshot snapshot = EntitySnapshot.fromEntity(entity);
        entity.setMenuState(true);

        player.openMenu(new ContractMenuProvider(snapshot, entity), buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(entity.getUUID());
            buf.writeBoolean(entity.isPatrolMode());
        });
    }

    public static void openInventoryScreen(Player player, AbstractMercenaryEntity entity) {
        if (player.level().isClientSide()) return;

        EntitySnapshot snapshot = EntitySnapshot.fromEntity(entity);
        entity.setMenuState(true);

        player.openMenu(new ContractInventoryMenuProvider(snapshot, entity), buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(entity.getUUID());
            buf.writeBoolean(entity.isPatrolMode());
        });
    }

    private static void sendIntroductionMessage(ServerPlayer serverPlayer,
                                                AbstractMercenaryEntity humanEntity,
                                                EntityClass entityClass) {
/*
        int contractMinutes = humanEntity.getData(MRDataAttachments.CONTRACT_DATA).getContractMinutes();

        String messageKey;
        switch (entityClass) {
            case WARRIOR -> messageKey = humanEntity.hasShield()
                    ? "ui.magic_realms.introduction.warrior"
                    : "ui.magic_realms.introduction.warrior_no_shield";
            case ROGUE -> messageKey = humanEntity.isArcher()
                    ? "ui.magic_realms.introduction.archer"
                    : "ui.magic_realms.introduction.assassin";
            case MAGE -> messageKey = "ui.magic_realms.introduction.mage";
            default -> messageKey = "ui.magic_realms.introduction.default";
        }

        if (humanEntity instanceof IExclusiveMercenary exclusiveMercenary) {
            messageKey = exclusiveMercenary.getExclusiveMercenaryPresentationMessage();
        }

        serverPlayer.sendSystemMessage(
                MercenaryMessageFormatter.buildFor(humanEntity, messageKey, contractMinutes));
*/
        boolean isExclusive = humanEntity instanceof IExclusiveMercenary;

        ResourceLocation dialogue = isExclusive
                ? ResourceLocation.fromNamespaceAndPath("magic_realms", "exclusive_introduction_dialogues")
                : ResourceLocation.fromNamespaceAndPath("magic_realms", "introduction_dialogues");

        DialogueManager.open(serverPlayer, dialogue, humanEntity);
    }

    private static class ContractMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final AbstractMercenaryEntity entity;
        private final EntityType<? extends AbstractMercenaryEntity> entityType;

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
            return new ContractHumanInfoMenu(containerId, playerInventory, snapshot, entity, entityType);
        }
    }

    private static class ContractInventoryMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final AbstractMercenaryEntity entity;

        public ContractInventoryMenuProvider(EntitySnapshot snapshot, AbstractMercenaryEntity entity) {
            this.snapshot = snapshot;
            this.entity = entity;
        }

        @Override
        public Component getDisplayName() {
            String starDisplay = "★".repeat(entity != null ? entity.getStarLevel() : 1);
            MutableComponent title = Component.translatable("gui.magic_realms.human_info.title");
            String entityName = entity != null ?
                    entity.getEntityName() :
                    (snapshot != null ? snapshot.entityName : "Unknown");
            MutableComponent entityInfo = Component.literal(starDisplay + " " + entityName);
            entityInfo = entityInfo.withStyle(ChatFormatting.AQUA);
            return Component.literal(title.getString() + " - " + entityInfo.getString());
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ContractInventoryMenu(containerId, playerInventory, snapshot, entity);
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
            buf.writeBoolean(entity.isPatrolMode());
        });
    }

    // Method for opening screen from snapshot only (useful for client-side or when entity unavailable)
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
            // No live entity available here, so we default to follow-mode. This entry point isn't reached by the normal interaction flow.
            buf.writeBoolean(false);
        });
    }
}