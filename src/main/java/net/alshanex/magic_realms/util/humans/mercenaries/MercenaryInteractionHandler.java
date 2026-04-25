package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
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
 *
 * <p><b>Contractor interaction model</b>:
 * <ul>
 *     <li>Right-click (no item, no shift): mercenary speaks a random line.</li>
 *     <li>Shift + right-click (no item): opens the contract menu.</li>
 *     <li>Holding a contract item: contract creation / extension / upgrade flow regardless of shift.</li>
 *     <li>Holding food the mercenary considers a gift: gift accepted.</li>
 *     <li>Holding the Hell Pass: immortality grant.</li>
 * </ul>
 *
 * <p>Patrol/follow toggling has been moved out of shift+right-click and into a button inside the contract screen.
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

        boolean isContractor = contractData != null
                && contractData.getContractorUUID() != null
                && contractData.getContractorUUID().equals(player.getUUID());
        boolean isContractItem = heldItem.getItem() instanceof PermanentContractItem
                || heldItem.getItem() instanceof TieredContractItem;

        // Contractor interacting with a non-contract item mid-combat gets a flavor refusal line rather than opening the menu.
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

        // Contract-related interactions (introduction for non-contractor, contract creation for held contract items, menu/speech for current contractor).
        handleContractInteraction(entity, player, contractData, heldItem);

        // Let the parent class run its own logic (e.g., opening the inventory menu).
        return InteractionResult.PASS;
    }

    private static InteractionResult handleHellPass(
            AbstractMercenaryEntity entity, Player player, ItemStack heldItem) {

        if (entity.isImmortal()) {
            player.sendSystemMessage(MercenaryMessageFormatter.buildFor(entity, "message.magic_realms.already_immortal"));
            return InteractionResult.FAIL;
        }

        entity.setImmortal(true);
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(MercenaryMessageFormatter.buildFor(entity, "message.magic_realms.granted_immortality"));
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
                    MutableComponent message = MercenaryMessageFormatter.buildFor(entity,
                            "ui.magic_realms.contract_reject_permanent");
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
        return MercenaryMessageFormatter.buildFor(entity, "message.magic_realms.mercenary.busy_fighting." + variant);
    }
}