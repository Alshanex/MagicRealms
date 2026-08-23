package net.alshanex.magic_realms.item.lore;

import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRDataComponentRegistry;
import net.alshanex.magic_realms.story.LoreProgression;
import net.alshanex.magic_realms.util.MRClientUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class LoreArtifactItem extends Item {

    public enum ArtifactType {
        WORLD_TABLET,
        ORDER_CRYSTAL
    }

    private final ArtifactType type;

    public LoreArtifactItem(Properties properties, ArtifactType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {


        ItemStack stack = player.getItemInHand(hand);
        PlayerLoreProgress progress = player.getData(MRDataAttachments.PLAYER_LORE);

        // Check if this specific item has ALREADY been read and assigned a Lore ID
        ResourceLocation storedLoreId = stack.get(MRDataComponentRegistry.LORE_NODE_ID.get());

        if(this.type == ArtifactType.ORDER_CRYSTAL){
            player.getCooldowns().addCooldown(this, 1200);
        }

        if (storedLoreId != null) {
            LoreProgression.LoreNode node = LoreProgression.getNodeById(storedLoreId);
            if (node != null) {
                // SAFETY CHECK: Does the player already know this lore?
                if (progress.hasLoreNode(storedLoreId)) {
                    if (level.isClientSide) {
                        if(this.type == ArtifactType.WORLD_TABLET){
                            MRClientUtils.openLoreArtifactScreen(node.message(), node.title());
                        } else if (this.type == ArtifactType.ORDER_CRYSTAL) {
                            MRClientUtils.playLoreSound(node.sound());
                        }
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }

                // SAFETY CHECK: If they don't know it, is this EXACTLY their next allowed lore step?
                Optional<LoreProgression.LoreNode> nextAllowed = (this.type == ArtifactType.WORLD_TABLET)
                        ? LoreProgression.getNextWorldLore(progress)
                        : LoreProgression.getNextOrderLore(progress);

                if (nextAllowed.isPresent() && nextAllowed.get().id().equals(storedLoreId)) {
                    if (level.isClientSide) {
                        if(this.type == ArtifactType.WORLD_TABLET){
                            MRClientUtils.openLoreArtifactScreen(node.message(), node.title());
                        } else if (this.type == ArtifactType.ORDER_CRYSTAL) {
                            MRClientUtils.playLoreSound(node.sound());
                        }
                    }
                    if (!level.isClientSide) {
                        PlayerLoreProgress newProgress = progress.withLoreNode(storedLoreId);
                        player.setData(MRDataAttachments.PLAYER_LORE, newProgress);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                } else {
                    // The lore is too far ahead of their current progression. They can't comprehend it.
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.translatable("item.magic_realms.lore_too_advanced").withStyle(ChatFormatting.RED), true);
                    }
                    return InteractionResultHolder.fail(stack);
                }
            }
        }

        // The item is UNREAD (Brand new item). Try to decipher it.
        Optional<LoreProgression.LoreNode> nextLore = (this.type == ArtifactType.WORLD_TABLET)
                ? LoreProgression.getNextWorldLore(progress)
                : LoreProgression.getNextOrderLore(progress);

        if (nextLore.isPresent()) {
            LoreProgression.LoreNode node = nextLore.get();

            if (level.isClientSide) {
                if(this.type == ArtifactType.WORLD_TABLET){
                    MRClientUtils.openLoreArtifactScreen(node.message(), node.title());
                } else if (this.type == ArtifactType.ORDER_CRYSTAL) {
                    MRClientUtils.playLoreSound(node.sound());
                }
            }

            if (!level.isClientSide) {
                // Assign the Lore ID to the item permanently
                stack.set(MRDataComponentRegistry.LORE_NODE_ID.get(), node.id());

                // Unlock the lore in the player's progression
                PlayerLoreProgress newProgress = progress.withLoreNode(node.id());
                player.setData(MRDataAttachments.PLAYER_LORE, newProgress);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            // The player already knows all lore for this type, or is missing prerequisites
            if (!level.isClientSide) {
                if (this.type == ArtifactType.ORDER_CRYSTAL && !progress.hasLoreNode(LoreProgression.WORLD_4.id())) {
                    player.displayClientMessage(Component.translatable("item.magic_realms.crystal_locked").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.RED), true);
                } else {
                    player.displayClientMessage(Component.translatable("item.magic_realms.all_lore_found").withStyle(ChatFormatting.YELLOW), true);
                }
            }
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if(stack.has(MRDataComponentRegistry.LORE_NODE_ID.get())){
            ResourceLocation storedLoreId = stack.get(MRDataComponentRegistry.LORE_NODE_ID.get());
            if(storedLoreId != null && LoreProgression.getNodeById(storedLoreId).title() != null){
                tooltipComponents.add(LoreProgression.getNodeById(storedLoreId).title().copy().withStyle(ChatFormatting.GOLD));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(MRDataComponentRegistry.LORE_NODE_ID.get());
    }
}
