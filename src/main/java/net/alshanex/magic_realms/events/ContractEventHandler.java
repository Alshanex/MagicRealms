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
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class ContractEventHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        // Solo procesar en el servidor para evitar mensajes duplicados
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();

        // Verificación adicional para asegurar que estamos en el servidor
        if (player.level().isClientSide()) return;

        // Solo procesar si es un servidor real (no integrado en single player)
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel)) return;

        if (event.getTarget() instanceof RandomHumanEntity humanEntity) {
            ItemStack heldItem = player.getItemInHand(event.getHand());
            ContractData contractData = humanEntity.getData(MRDataAttachments.CONTRACT_DATA);

            // Solo procesar con la mano principal para evitar duplicaciones
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }

            // Si el jugador tiene un contrato permanente en la mano
            if (heldItem.getItem() instanceof PermanentContractItem) {
                handlePermanentContractCreation(event, player, humanEntity, contractData, heldItem);
                return;
            }

            // Si el jugador tiene un contrato tiered (nuevo o viejo) en la mano
            if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
                handleTieredContractCreation(event, player, humanEntity, contractData, heldItem, tieredContract);
                return;
            }

            // Si el jugador hace click derecho sin item (mano vacía) y tiene contrato activo
            if (heldItem.isEmpty()) {
                handleContractInteraction(event, player, humanEntity, contractData);
                return;
            }
        }
    }

    private static void handlePermanentContractCreation(PlayerInteractEvent.EntityInteract event,
                                                        Player player,
                                                        RandomHumanEntity humanEntity,
                                                        ContractData contractData,
                                                        ItemStack heldItem) {

        // Verificación usando el método seguro
        if (!contractData.canEstablishPermanentContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component message;
                if (contractData.isPermanent()) {
                    // Ya es permanente de otro jugador
                    message = Component.translatable("ui.magic_realms.contract_permanent_other_player",
                                    humanEntity.getEntityName())
                            .withStyle(ChatFormatting.RED);
                } else {
                    // Contrato temporal con otro jugador
                    message = Component.translatable("ui.magic_realms.already_have_contract")
                            .withStyle(ChatFormatting.RED);
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        // Si ya es permanente del mismo jugador, mostrar mensaje y no consumir item
        if (contractData.isPermanent() && contractData.isContractor(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_already_permanent",
                                        humanEntity.getEntityName())
                                .withStyle(ChatFormatting.YELLOW)
                ));
            }
            event.setCanceled(true);
            return;
        }

        boolean isUpgrade = contractData.hasActiveContract() && contractData.isContractor(player.getUUID());

        // Intentar establecer contrato permanente
        boolean success = contractData.trySetPermanentContract(player.getUUID());

        if (!success) {
            // Esto no debería pasar, pero por seguridad
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_failed")
                                .withStyle(ChatFormatting.RED)
                ));
            }
            event.setCanceled(true);
            return;
        }

        if (!isUpgrade) {
            humanEntity.setSummoner(player);
        }

        // Actualizar el nombre para mostrar el símbolo de infinito
        humanEntity.updateCustomNameWithStars();

        // Enviar mensaje de éxito
        if (player instanceof ServerPlayer serverPlayer) {
            Component message;
            if (isUpgrade) {
                message = Component.translatable("ui.magic_realms.contract_upgraded_permanent", humanEntity.getEntityName())
                        .withStyle(ChatFormatting.GOLD);
            } else {
                message = Component.translatable("ui.magic_realms.contract_established_permanent", humanEntity.getEntityName())
                        .withStyle(ChatFormatting.GOLD);
            }

            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }

        // Log del evento
        String actionType = isUpgrade ? "upgraded to permanent" : "established permanent";
        MagicRealms.LOGGER.info("Player {} {} contract with entity {} ({}). Contract info: {}",
                player.getName().getString(),
                actionType,
                humanEntity.getEntityName(),
                humanEntity.getUUID(),
                contractData.getDetailedInfo());

        // Consumir el item
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        event.setCanceled(true);
    }

    private static void handleTieredContractCreation(PlayerInteractEvent.EntityInteract event,
                                                     Player player,
                                                     RandomHumanEntity humanEntity,
                                                     ContractData contractData,
                                                     ItemStack heldItem,
                                                     TieredContractItem contractItem) {

        // Verificación temprana: no se puede usar contrato temporal en entidad con contrato permanente
        if (!contractData.canEstablishTemporaryContract(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component message;
                if (contractData.isPermanent()) {
                    if (contractData.isContractor(player.getUUID())) {
                        // Es el mismo jugador con contrato permanente
                        message = Component.translatable("ui.magic_realms.contract_permanent_no_upgrade",
                                        humanEntity.getEntityName())
                                .withStyle(ChatFormatting.GOLD);
                    } else {
                        // Es otro jugador
                        message = Component.translatable("ui.magic_realms.contract_permanent_other_player",
                                        humanEntity.getEntityName())
                                .withStyle(ChatFormatting.RED);
                    }
                } else {
                    // Contrato temporal con otro jugador
                    message = Component.translatable("ui.magic_realms.already_have_contract")
                            .withStyle(ChatFormatting.RED);
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
            }
            event.setCanceled(true);
            return;
        }

        // Obtener el nivel de la entidad
        KillTrackerData killTracker = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
        int entityLevel = killTracker.getCurrentLevel();
        ContractTier contractTier = contractItem.getTier();
        ContractTier requiredTier = ContractTier.getRequiredTierForLevel(entityLevel);

        // Verificar si el contrato es apropiado para el nivel de la entidad
        if (!contractTier.canContractLevel(entityLevel)) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component message;
                if (contractTier.getMinLevel() > entityLevel) {
                    // El contrato es demasiado poderoso
                    message = Component.translatable("ui.magic_realms.entity_level_too_low", entityLevel)
                            .withStyle(ChatFormatting.RED);
                } else {
                    // El contrato no es lo suficientemente poderoso
                    message = Component.translatable("ui.magic_realms.contract_tier_mismatch",
                                    entityLevel,
                                    requiredTier.getDisplayName().getString(),
                                    requiredTier.getMinLevel(),
                                    requiredTier.getMaxLevel())
                            .withStyle(ChatFormatting.RED);
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
            // Esto no debería pasar ya que verificamos antes, pero por seguridad
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_failed")
                                .withStyle(ChatFormatting.RED)
                ));
            }
            event.setCanceled(true);
            return;
        }

        // Enviar mensaje de éxito
        if (player instanceof ServerPlayer serverPlayer) {
            if (isRenewal) {
                int remainingMinutes = contractData.getRemainingMinutes();
                int remainingSeconds = contractData.getRemainingSeconds();

                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_extended",
                                        humanEntity.getEntityName(),
                                        additionalMinutes,
                                        remainingMinutes,
                                        remainingSeconds)
                                .withStyle(ChatFormatting.GREEN)
                ));
            } else {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_established",
                                        humanEntity.getEntityName(),
                                        additionalMinutes)
                                .withStyle(ChatFormatting.GREEN)
                ));
            }
        }

        // Log del evento
        String actionType = isRenewal ? "extended" : "established";
        MagicRealms.LOGGER.info("Player {} {} contract with Level {} entity {} ({}) using {} contract. Contract info: {}",
                player.getName().getString(),
                actionType,
                entityLevel,
                humanEntity.getEntityName(),
                humanEntity.getUUID(),
                contractTier.getName(),
                contractData.getDetailedInfo());

        // Consumir el item
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        event.setCanceled(true);
    }

    private static void handleContractInteraction(PlayerInteractEvent.EntityInteract event,
                                                  Player player,
                                                  RandomHumanEntity humanEntity,
                                                  ContractData contractData) {

        // Verificar si el jugador tiene un contrato activo
        if (!contractData.isContractor(player.getUUID())) {
            if (contractData.hasActiveContract()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.translatable("ui.magic_realms.contract_other_player")
                                    .withStyle(ChatFormatting.RED)
                    ));
                }
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    // Obtener información del nivel y mostrar qué contrato necesita
                    KillTrackerData killTracker = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
                    int entityLevel = killTracker.getCurrentLevel();
                    ContractTier requiredTier = ContractTier.getRequiredTierForLevel(entityLevel);
                    int contractMinutes = contractData.getAdditionalMinutesForStarLevel(humanEntity.getStarLevel());

                    Component message = Component.translatable("ui.magic_realms.need_contract_item", contractMinutes)
                            .append(" ")
                            .append(Component.translatable("ui.magic_realms.wrong_contract_tier",
                                    entityLevel,
                                    requiredTier.getDisplayName().getString()))
                            .withStyle(ChatFormatting.YELLOW);

                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
                }
            }
            event.setCanceled(true);
            return;
        }

        // Si el contrato es permanente, mostrar mensaje especial
        if (contractData.isPermanent()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_time_permanent")
                                .withStyle(ChatFormatting.GOLD)
                ));
            }
        } else {
            // Mostrar tiempo restante para contratos temporales
            int minutes = contractData.getRemainingMinutes();
            int seconds = contractData.getRemainingSeconds();
            int starLevel = humanEntity.getStarLevel();
            int extensionMinutes = contractData.getAdditionalMinutesForStarLevel(starLevel);

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.contract_time_remaining_with_extension",
                                        minutes, seconds, extensionMinutes)
                                .withStyle(ChatFormatting.AQUA)
                ));
            }
        }

        // Verificar que la entidad esté en un estado válido antes de crear el snapshot
        if (humanEntity.isRemoved() || !humanEntity.isAlive()) {
            MagicRealms.LOGGER.warn("Attempted to open menu for removed/dead entity: {}", humanEntity.getEntityName());
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal("Entity is no longer available").withStyle(ChatFormatting.RED)
                ));
            }
            event.setCanceled(true);
            return;
        }

        // Crear snapshot actualizado y abrir el menú
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

        event.setCanceled(true);
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
            return Component.translatable("gui.magic_realms.human_info.title")
                    .append(" - ")
                    .append(Component.literal(starDisplay + " " + (entity != null ? entity.getEntityName() : "Unknown")).withStyle(ChatFormatting.AQUA));
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            MagicRealms.LOGGER.info("Creating menu for player: {} (client side: {})",
                    player.getName().getString(), player.level().isClientSide);

            return new ContractHumanInfoMenu(containerId, playerInventory, snapshot, entity);
        }
    }
}
