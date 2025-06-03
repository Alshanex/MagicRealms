package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.item.HumanInfoItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.util.EntitySnapshot;
import net.minecraft.ChatFormatting;
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

            // Si el jugador tiene un HumanInfoItem en la mano
            if (heldItem.getItem() instanceof HumanInfoItem) {
                handleContractCreation(event, player, humanEntity, contractData, heldItem);
                return;
            }

            // Si el jugador hace click derecho sin item (mano vacía) y tiene contrato activo
            if (heldItem.isEmpty()) {
                handleContractInteraction(event, player, humanEntity, contractData);
                return;
            }
        }
    }

    private static void handleContractCreation(PlayerInteractEvent.EntityInteract event,
                                               Player player,
                                               RandomHumanEntity humanEntity,
                                               ContractData contractData,
                                               ItemStack heldItem) {

        // Verificar si ya tiene un contrato activo con otro jugador
        if (contractData.hasActiveContract() && !contractData.isContractor(player.getUUID())) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.magic_realms.already_have_contract")
                                .withStyle(ChatFormatting.RED)
                ));
            }
            event.setCanceled(true);
            return;
        }

        // Crear o renovar el contrato
        contractData.setContract(player.getUUID());
        humanEntity.setSummoner(player);

        // Consumir el item
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        // Mensaje de confirmación
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.magic_realms.contract_established", humanEntity.getEntityName())
                            .withStyle(ChatFormatting.GREEN)
            ));
        }

        MagicRealms.LOGGER.info("Player {} established contract with entity {} ({})",
                player.getName().getString(),
                humanEntity.getEntityName(),
                humanEntity.getUUID());

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
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.translatable("ui.magic_realms.need_contract_item")
                                    .withStyle(ChatFormatting.YELLOW)
                    ));
                }
            }
            event.setCanceled(true);
            return;
        }

        // Mostrar tiempo restante
        int minutes = contractData.getRemainingMinutes();
        int seconds = contractData.getRemainingSeconds();

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.magic_realms.contract_time_remaining", minutes, seconds)
                            .withStyle(ChatFormatting.AQUA)
            ));
        }

        // Crear snapshot actualizado y abrir el menú
        EntitySnapshot snapshot = EntitySnapshot.fromEntity(humanEntity);
        player.openMenu(new ContractMenuProvider(snapshot, humanEntity), buf -> {
            buf.writeNbt(snapshot.serialize());
            buf.writeUUID(humanEntity.getUUID());
        });

        event.setCanceled(true);
    }

    private static class ContractMenuProvider implements MenuProvider {
        private final EntitySnapshot snapshot;
        private final RandomHumanEntity entity;

        public ContractMenuProvider(EntitySnapshot snapshot, RandomHumanEntity entity) {
            this.snapshot = snapshot;
            this.entity = entity;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.magic_realms.human_info.title")
                    .append(" - ")
                    .append(Component.literal(entity.getEntityName()).withStyle(ChatFormatting.AQUA));
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ContractHumanInfoMenu(containerId, playerInventory, snapshot, entity);
        }
    }
}
