package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TemporaryContractItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueManager;
import net.pixeldreamstudios.rpgdialogue.murmur.MurmurManager;

import java.util.List;
import java.util.Random;

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
 * <p>There are exactly two contract items: {@link TemporaryContractItem}, which starts or extends a fixed-length
 * contract, and {@link PermanentContractItem}, which upgrades an existing relationship to permanent once enough
 * cumulative service time has been accrued.
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

        if(entity.isInCombat()){
            List<String> keys = List.of(
                    "message.magic_realms.mercenary.busy_fighting.0",
                    "message.magic_realms.mercenary.busy_fighting.1",
                    "message.magic_realms.mercenary.busy_fighting.2",
                    "message.magic_realms.mercenary.busy_fighting.3"
            );

            Random random = new Random();
            String chosenKey = keys.get(random.nextInt(keys.size()));

            Component line = Component.translatable(chosenKey);
            MurmurManager.speak(entity, line, MurmurManager.EARSHOT);

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

        // Contract-related interactions (introduction for non-contractor, contract creation for held contract items, menu/speech for current contractor).
        handleContractInteraction(entity, player, contractData, heldItem);

        // Let the parent class run its own logic (e.g., opening the inventory menu).
        return InteractionResult.PASS;
    }

    private static InteractionResult handleHellPass(
            AbstractMercenaryEntity entity, Player player, ItemStack heldItem) {

        if (entity.isImmortal()) {
            if(player instanceof ServerPlayer serverPlayer){
                DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_already_immortal"), entity);
            }
            //player.sendSystemMessage(MercenaryMessageFormatter.buildFor(entity, "message.magic_realms.already_immortal"));
            return InteractionResult.FAIL;
        }

        entity.setImmortal(true);
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        if (player instanceof ServerPlayer serverPlayer) {
            DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_granted_immortal"), entity);
            //serverPlayer.sendSystemMessage(MercenaryMessageFormatter.buildFor(entity, "message.magic_realms.granted_immortality"));
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
                    DialogueManager.open(serverPlayer, ResourceLocation.fromNamespaceAndPath("magic_realms", "mercenary_reject_permanent"), entity);
                    //MutableComponent message = MercenaryMessageFormatter.buildFor(entity, "ui.magic_realms.contract_reject_permanent");
                    //serverPlayer.sendSystemMessage(message);
                }
            } else {
                ContractUtils.handlePermanentContractCreation(player, entity, contractData, heldItem);
            }
        } else if (heldItem.getItem() instanceof TemporaryContractItem) {
            ContractUtils.handleTemporaryContractCreation(player, entity, contractData, heldItem);
        } else {
            ContractUtils.handleContractInteraction(player, entity, contractData);
        }
    }
}