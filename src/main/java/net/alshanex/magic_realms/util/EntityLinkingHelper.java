package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.item.HumanInfoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class EntityLinkingHelper {

    public static InteractionResult tryLinkEntityToItem(RandomHumanEntity entity, Player player, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getItemInHand(hand);

        // Verificar si el jugador tiene un HumanInfoItem en la mano
        if (heldItem.getItem() instanceof HumanInfoItem) {

            // Si el item ya está enlazado a otra entidad
            if (HumanInfoItem.isLinkedToEntity(heldItem)) {
                player.sendSystemMessage(Component.literal("This item is already linked to another entity.")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            // Crear el enlace
            ItemStack linkedItem = HumanInfoItem.createLinkedItem(entity);

            // Reemplazar el item en la mano del jugador
            player.setItemInHand(hand, linkedItem);

            player.sendSystemMessage(Component.literal("Successfully linked ")
                    .append(Component.literal(entity.getEntityName()).withStyle(ChatFormatting.YELLOW))
                    .append(" to the item!")
                    .withStyle(ChatFormatting.GREEN));

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static boolean hasLinkedItemInInventory(ServerPlayer player, UUID entityUUID) {
        try {
            // Revisar inventario principal + hotbar
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (isItemLinkedToEntity(stack, entityUUID)) {
                    return true;
                }
            }

            // Revisar enderchest
            for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
                ItemStack stack = player.getEnderChestInventory().getItem(i);
                if (isItemLinkedToEntity(stack, entityUUID)) {
                    return true;
                }
            }
        } catch (Exception e) {
            net.alshanex.magic_realms.MagicRealms.LOGGER.debug("Error checking player inventory: {}", e.getMessage());
        }

        return false;
    }

    private static boolean isItemLinkedToEntity(ItemStack stack, UUID entityUUID) {
        if (stack.getItem() instanceof HumanInfoItem) {
            UUID linkedUUID = HumanInfoItem.getLinkedEntityUUID(stack);
            return entityUUID.equals(linkedUUID);
        }
        return false;
    }

    public static void updateLinkedItems(RandomHumanEntity entity) {
        if (entity.level().isClientSide) return;

        try {
            for (ServerPlayer player : ((net.minecraft.server.level.ServerLevel) entity.level()).getPlayers(p -> true)) {
                updateLinkedItemsInPlayerInventory(player, entity);
            }
        } catch (Exception e) {
            net.alshanex.magic_realms.MagicRealms.LOGGER.debug("Error updating linked items: {}", e.getMessage());
        }
    }

    private static void updateLinkedItemsInPlayerInventory(ServerPlayer player, RandomHumanEntity entity) {
        try {
            UUID entityUUID = entity.getUUID();

            // Revisar inventario principal + hotbar
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (isItemLinkedToEntity(stack, entityUUID)) {
                    HumanInfoItem.updateBasicInfo(stack, entity);
                }
            }

            // Revisar enderchest
            for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
                ItemStack stack = player.getEnderChestInventory().getItem(i);
                if (isItemLinkedToEntity(stack, entityUUID)) {
                    HumanInfoItem.updateBasicInfo(stack, entity);
                }
            }
        } catch (Exception e) {
            net.alshanex.magic_realms.MagicRealms.LOGGER.debug("Error updating player inventory items: {}", e.getMessage());
        }
    }
}
