package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the cascading {@code mobInteract} logic for mercenaries: sitting / stunned shortcuts, hell pass immortality grants, contract creation, and
 * combat-busy refusal messages.
 */
public final class MercenaryInteractionHandler {

    private MercenaryInteractionHandler() {}

    public static InteractionResult handleInteraction(
            AbstractMercenaryEntity entity, Player player, InteractionHand hand) {

        // Click-to-unsit takes priority over everything else.
        if (entity.isSittingInChair()) {
            entity.unsitFromChair();
            return InteractionResult.SUCCESS;
        }

        // Stunned mercs ignore interactions entirely.
        if (entity.isStunned()) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        ContractData contractData = entity.getData(MRDataAttachments.CONTRACT_DATA);

        // Offhand interactions are never meaningful here.
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }

        // Hell Pass grants immortality.
        if (heldItem.is(MRItems.HELL_PASS.get())) {
            return handleHellPass(entity, player, heldItem);
        }

        // Contractor interacting with a non-contract item mid-combat gets a flavor refusal line rather than opening the menu.
        boolean isContractor = contractData != null
                && contractData.getContractorUUID() != null
                && contractData.getContractorUUID().equals(player.getUUID());
        boolean isContractItem = heldItem.getItem() instanceof PermanentContractItem
                || heldItem.getItem() instanceof TieredContractItem;

        if (isContractor && !isContractItem && entity.isInCombat()) {
            if (player instanceof ServerPlayer serverPlayer) {
                Component message = Component.translatable(
                        "message.magic_realms.mercenary.speech",
                        entity.getEntityName(),
                        pickCombatRefusalLine(entity)
                ).withStyle(ChatFormatting.RED);
                serverPlayer.sendSystemMessage(message);
            }
            return InteractionResult.SUCCESS;
        }

        // Contract-related interactions.
        handleContractInteraction(entity, player, contractData, heldItem);

        // Let the parent class run its own logic (e.g., opening the inventory menu).
        return InteractionResult.PASS;
    }

    private static InteractionResult handleHellPass(
            AbstractMercenaryEntity entity, Player player, ItemStack heldItem) {

        if (entity.isImmortal()) {
            player.sendSystemMessage(Component.translatable("message.magic_realms.already_immortal",
                    entity.getEntityName()).withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }

        entity.setImmortal(true);
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("message.magic_realms.granted_immortality",
                    entity.getEntityName()).withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Dispatches to the correct contract-creation flow based on the held item.
     * Exclusive mercenaries reject permanent contracts.
     */
    public static void handleContractInteraction(
            AbstractMercenaryEntity entity, Player player,
            ContractData contractData, ItemStack heldItem) {

        if (heldItem.getItem() instanceof PermanentContractItem) {
            if (entity.isExclusiveMercenary()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    MutableComponent message = Component.translatable(
                            "ui.magic_realms.contract_reject_permanent",
                            entity.getEntityName()).withStyle(ChatFormatting.GOLD);
                    serverPlayer.sendSystemMessage(message);
                }
            } else {
                ContractUtils.handlePermanentContractCreation(player, entity, contractData, heldItem);
            }
        } else if (heldItem.getItem() instanceof TieredContractItem tieredContract) {
            ContractUtils.handleTieredContractCreation(player, entity, contractData, heldItem, tieredContract);
        } else {
            ContractUtils.handleContractInteraction(player, entity, contractData);
        }
    }

    /** Picks one of four flavor-text refusal lines for a contractor's mid-combat interaction. */
    private static Component pickCombatRefusalLine(AbstractMercenaryEntity entity) {
        int variant = entity.getRandom().nextInt(4);
        String key = "message.magic_realms.mercenary.busy_fighting." + variant;
        return Component.translatable(key);
    }
}
