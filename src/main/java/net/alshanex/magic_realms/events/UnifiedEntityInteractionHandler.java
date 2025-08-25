package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.util.ContractTier;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.alshanex.magic_realms.util.chat_system.EntityAIBrain;
import net.alshanex.magic_realms.util.chat_system.EntityAIManager;
import net.alshanex.magic_realms.util.chat_system.EntityChatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import javax.annotation.Nullable;

/**
 * Unified handler for all entity interactions and AI system management
 */
@EventBusSubscriber(modid = MagicRealms.MODID)
public class UnifiedEntityInteractionHandler {

    // ========== Server Events for AI System ==========

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Initialize the AI manager when server starts
        EntityAIManager.initialize(event.getServer());
        MagicRealms.LOGGER.info("Initialized Entity AI Manager");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Save all AI data when server stops
        EntityAIManager.saveAll();
        MagicRealms.LOGGER.info("Saved all Entity AI data");
    }

    // ========== Entity Interaction Handler ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        // Only process on server
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();

        // Additional server-side check
        if (player.level().isClientSide()) return;

        // Only process RandomHumanEntity interactions
        if (!(event.getTarget() instanceof RandomHumanEntity humanEntity)) return;

        // Only process main hand to avoid duplications
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack heldItem = player.getItemInHand(event.getHand());
        ContractData contractData = humanEntity.getData(MRDataAttachments.CONTRACT_DATA);

        // Priority 1: Contract items (Permanent Contract)
        if (heldItem.getItem() instanceof PermanentContractItem) {
            handlePermanentContractCreation(event, player, humanEntity, contractData, heldItem);
            return;
        }

        // Priority 2: Contract items (Tiered Contract)
        if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
            handleTieredContractCreation(event, player, humanEntity, contractData, heldItem, tieredContract);
            return;
        }

        // Priority 3: Stick for standby toggle (for contracted players)
        if (heldItem.getItem() == Items.STICK) {
            handleStandbyToggle(event, player, humanEntity, contractData);
            return;
        }

        // Priority 4: Shift + Right-click for menu (for contracted players)
        if (player.isShiftKeyDown()) {
            if (contractData.isContractor(player.getUUID())) {
                handleContractMenuOpen(player, humanEntity);
                event.setCanceled(true);
            } else {
                sendMessage(player, Component.literal("You need a contract to access this entity's information.")
                        .withStyle(ChatFormatting.RED));
                event.setCanceled(true);
            }
            return;
        }

        // Priority 5: Empty hand right-click for chat (no shift)
        if (heldItem.isEmpty() && !player.isShiftKeyDown()) {
            handleChatInteraction(event, player, humanEntity);
            return;
        }
    }

    // ========== Chat Interaction ==========

    private static void handleChatInteraction(PlayerInteractEvent.EntityInteract event, Player player, RandomHumanEntity humanEntity) {
        // Get or create AI brain for this entity
        EntityAIBrain brain = EntityAIManager.getBrain(humanEntity);

        // Open chat interface
        if (player instanceof ServerPlayer serverPlayer) {
            // Send initial greeting
            String greeting = brain.generateResponse(player, "", humanEntity);

            // Create chat component with entity name
            MutableComponent entityName = Component.literal(humanEntity.getEntityName())
                    .withStyle(ChatFormatting.AQUA);

            MutableComponent chatMessage = Component.literal("")
                    .append(entityName)
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(greeting).withStyle(ChatFormatting.WHITE));

            serverPlayer.sendSystemMessage(chatMessage);

            // Start chat session
            EntityChatManager.startChat(serverPlayer, humanEntity);
        }

        event.setCanceled(true);
    }

    // ========== Standby Toggle ==========

    private static void handleStandbyToggle(PlayerInteractEvent.EntityInteract event, Player player,
                                            RandomHumanEntity humanEntity, ContractData contractData) {
        // Check if player has contract
        if (!contractData.isContractor(player.getUUID())) {
            sendMessage(player, Component.literal("You need a contract to control this entity.")
                    .withStyle(ChatFormatting.RED));
            event.setCanceled(true);
            return;
        }

        boolean currentStandbyState = humanEntity.isStandby();

        if (currentStandbyState) {
            humanEntity.setStandby(false);
            humanEntity.restoreMovementGoals();
            sendMessage(player, Component.literal("Entity is now following.")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            humanEntity.setStandby(true);
            humanEntity.clearMovementGoals();
            sendMessage(player, Component.literal("Entity is now on standby.")
                    .withStyle(ChatFormatting.YELLOW));
        }

        MagicRealms.LOGGER.info("Player {} toggled standby for entity {} to {}",
                player.getName().getString(), humanEntity.getEntityName(), !currentStandbyState);

        event.setCanceled(true);
    }

    // ========== Contract Menu ==========

    public static void handleContractMenuOpen(Player player, RandomHumanEntity humanEntity) {
        ContractData contractData = humanEntity.getData(MRDataAttachments.CONTRACT_DATA);

        // Verify that the entity is in a valid state before creating the snapshot
        if (humanEntity.isRemoved() || !humanEntity.isAlive()) {
            MagicRealms.LOGGER.warn("Attempted to open menu for removed/dead entity: {}", humanEntity.getEntityName());
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.literal("Entity is no longer available");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            return;
        }

        // Create snapshot and open the menu
        EntitySnapshot snapshot = EntitySnapshot.fromEntity(humanEntity);

        MagicRealms.LOGGER.info("Opening contract menu for entity: {} (UUID: {}, Star Level: {}, Contract Type: {})",
                humanEntity.getEntityName(), humanEntity.getUUID(), humanEntity.getStarLevel(),
                contractData.isPermanent() ? "Permanent" : "Temporary");

        player.openMenu(new ContractMenuProvider(snapshot, humanEntity), buf -> {
            CompoundTag snapshotNbt = snapshot.serialize();
            MagicRealms.LOGGER.debug("Sending snapshot NBT size: {} bytes", snapshotNbt.toString().length());
            buf.writeNbt(snapshotNbt);
            buf.writeUUID(humanEntity.getUUID());
            MagicRealms.LOGGER.debug("Sent entity UUID: {}", humanEntity.getUUID());
        });
    }

    private static class ContractMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final RandomHumanEntity entity;

        public ContractMenuProvider(EntitySnapshot snapshot, RandomHumanEntity entity) {
            this.snapshot = snapshot;
            this.entity = entity;

            MagicRealms.LOGGER.info("ContractMenuProvider created:");
            MagicRealms.LOGGER.info("  - Snapshot: {}", snapshot != null ? "present" : "null");
            MagicRealms.LOGGER.info("  - Entity: {}", entity != null ? entity.getEntityName() : "null");
            if (entity != null) {
                MagicRealms.LOGGER.info("  - Entity UUID: {}", entity.getUUID());
                MagicRealms.LOGGER.info("  - Entity alive: {}", entity.isAlive());
                MagicRealms.LOGGER.info("  - Entity removed: {}", entity.isRemoved());
                MagicRealms.LOGGER.info("  - Star Level: {}", entity.getStarLevel());
            }
        }

        @Override
        public Component getDisplayName() {
            String starDisplay = "★".repeat(entity.getStarLevel());
            MutableComponent title = Component.translatable("gui.magic_realms.human_info.title");
            MutableComponent entityInfo = Component.literal(starDisplay + " " + (entity != null ? entity.getEntityName() : "Unknown"));
            entityInfo = entityInfo.withStyle(ChatFormatting.AQUA);

            return Component.literal(title.getString() + " - " + entityInfo.getString());
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            MagicRealms.LOGGER.info("Creating menu for player: {} (client side: {})",
                    player.getName().getString(), player.level().isClientSide);

            return new ContractHumanInfoMenu(containerId, playerInventory, snapshot, entity);
        }
    }

    // ========== Permanent Contract Creation ==========

    private static void handlePermanentContractCreation(PlayerInteractEvent.EntityInteract event,
                                                        Player player,
                                                        RandomHumanEntity humanEntity,
                                                        ContractData contractData,
                                                        ItemStack heldItem) {
        // Check if player meets time requirement
        if (!contractData.hasMinimumContractTime(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                int remainingMinutes = contractData.getRemainingMinutesForPermanent(player.getUUID());

                MutableComponent message = Component.translatable("ui.magic_realms.permanent_contract_insufficient_time",
                        remainingMinutes);
                message = message.withStyle(ChatFormatting.RED);

                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        if (!contractData.canEstablishPermanentContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                if (contractData.isPermanent()) {
                    message = Component.translatable("ui.magic_realms.contract_permanent_other_player",
                            humanEntity.getEntityName());
                    message = message.withStyle(ChatFormatting.RED);
                } else {
                    message = Component.translatable("ui.magic_realms.already_have_contract");
                    message = message.withStyle(ChatFormatting.RED);
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        // If already permanent for same player, show message and don't consume item
        if (contractData.isPermanent() && contractData.isContractor(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_already_permanent",
                        humanEntity.getEntityName());
                message = message.withStyle(ChatFormatting.YELLOW);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        boolean isUpgrade = contractData.hasActiveContract() && contractData.isContractor(player.getUUID());

        // Try to establish permanent contract
        boolean success = contractData.trySetPermanentContract(player.getUUID());

        if (!success) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_failed");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        if (!isUpgrade) {
            humanEntity.setSummoner(player);
        }

        humanEntity.updateCustomNameWithStars();

        // Send success message
        if (player instanceof ServerPlayer serverPlayer) {
            MutableComponent message;
            if (isUpgrade) {
                message = Component.translatable("ui.magic_realms.contract_upgraded_permanent", humanEntity.getEntityName());
                message = message.withStyle(ChatFormatting.GOLD);
            } else {
                message = Component.translatable("ui.magic_realms.contract_established_permanent", humanEntity.getEntityName());
                message = message.withStyle(ChatFormatting.GOLD);
            }

            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }

        // Log event
        String actionType = isUpgrade ? "upgraded to permanent" : "established permanent";
        int totalMinutes = contractData.getTotalContractTimeMinutes(player.getUUID());
        MagicRealms.LOGGER.info("Player {} {} contract with entity {} ({}). Total contract time: {} minutes",
                player.getName().getString(),
                actionType,
                humanEntity.getEntityName(),
                humanEntity.getUUID(),
                totalMinutes);

        // Consume item
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        event.setCanceled(true);
    }

    // ========== Tiered Contract Creation ==========

    private static void handleTieredContractCreation(PlayerInteractEvent.EntityInteract event,
                                                     Player player,
                                                     RandomHumanEntity humanEntity,
                                                     ContractData contractData,
                                                     ItemStack heldItem,
                                                     TieredContractItem contractItem) {
        // Check if temporary contract can be established
        if (!contractData.canEstablishTemporaryContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                if (contractData.isPermanent()) {
                    if (contractData.isContractor(player.getUUID())) {
                        message = Component.translatable("ui.magic_realms.contract_permanent_no_upgrade",
                                humanEntity.getEntityName());
                        message = message.withStyle(ChatFormatting.GOLD);
                    } else {
                        message = Component.translatable("ui.magic_realms.contract_permanent_other_player",
                                humanEntity.getEntityName());
                        message = message.withStyle(ChatFormatting.RED);
                    }
                } else {
                    message = Component.translatable("ui.magic_realms.already_have_contract");
                    message = message.withStyle(ChatFormatting.RED);
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        // Get entity level
        KillTrackerData killTracker = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
        int entityLevel = killTracker.getCurrentLevel();
        ContractTier contractTier = contractItem.getTier();
        ContractTier requiredTier = ContractTier.getRequiredTierForLevel(entityLevel);

        // Verify contract is appropriate for entity level
        if (!contractTier.canContractLevel(entityLevel)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message;
                if (contractTier.getMinLevel() > entityLevel) {
                    message = Component.translatable("ui.magic_realms.entity_level_too_low", entityLevel);
                    message = message.withStyle(ChatFormatting.RED);
                } else {
                    message = Component.translatable("ui.magic_realms.contract_tier_mismatch",
                            entityLevel,
                            requiredTier.getDisplayName().getString(),
                            requiredTier.getMinLevel(),
                            requiredTier.getMaxLevel());
                    message = message.withStyle(ChatFormatting.RED);
                }

                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        int starLevel = humanEntity.getStarLevel();
        int additionalMinutes = contractData.getAdditionalMinutesForStarLevel(starLevel);
        boolean isRenewal = contractData.isContractor(player.getUUID());

        boolean success;
        if (isRenewal) {
            success = contractData.renewContract(player.getUUID(), starLevel);
        } else {
            success = contractData.trySetTemporaryContract(player.getUUID(), starLevel);
            if (success) {
                humanEntity.setSummoner(player);
            }
        }

        if (!success) {
            if (player instanceof ServerPlayer serverPlayer) {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_failed");
                message = message.withStyle(ChatFormatting.RED);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        // Send success message
        if (player instanceof ServerPlayer serverPlayer) {
            if (isRenewal) {
                int remainingMinutes = contractData.getRemainingMinutes();
                int remainingSeconds = contractData.getRemainingSeconds();

                MutableComponent message = Component.translatable("ui.magic_realms.contract_extended",
                        humanEntity.getEntityName(),
                        additionalMinutes,
                        remainingMinutes,
                        remainingSeconds);
                message = message.withStyle(ChatFormatting.GREEN);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            } else {
                MutableComponent message = Component.translatable("ui.magic_realms.contract_established",
                        humanEntity.getEntityName(),
                        additionalMinutes);
                message = message.withStyle(ChatFormatting.GREEN);
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
        }

        // Log event
        String actionType = isRenewal ? "extended" : "established";
        MagicRealms.LOGGER.info("Player {} {} contract with Level {} entity {} ({}) using {} contract",
                player.getName().getString(),
                actionType,
                entityLevel,
                humanEntity.getEntityName(),
                humanEntity.getUUID(),
                contractTier.getName());

        // Consume item
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        event.setCanceled(true);
    }

    // ========== Helper Methods ==========

    private static void sendMessage(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message);
        }
    }
}
